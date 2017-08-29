#!/bin/bash

DIRNAME=$(dirname ${BASH_SOURCE[0]})
source ${DIRNAME}/../conf/insightedge-env.sh

EMPTY="[]"
THIS_SCRIPT_NAME=`basename "$0"`
IE_VERSION=`grep -w "Version" ${XAP_HOME}/insightedge/VERSION | awk -F  ":" '{print $2}' | sed 's/ //'`
EDITION=`grep -w "Edition" ${XAP_HOME}/insightedge/VERSION | awk -F  ":" '{print $2}' | sed 's/ //'`

main() {
    display_logo
    local option=$1
    shift
    case "$option" in
      "")
        display_usage
        ;;
      "-h")
        display_usage
        ;;
      "demo")
        main_demo $@
        ;;
      "run")
        main_run $@
        ;;
      "deploy-space")
        main_deploy_space $@
        ;;
      "undeploy")
        main_undeploy $@
        ;;
      "shutdown")
        main_shutdown $@
        ;;
      *)
        echo "Unknown option [$option]"
        ;;
    esac
}

display_usage() {
    sleep 1
    echo ""
    display_logo
    echo ""
    echo "Usage: * - required"
    echo "     --mode      |  * insightedge mode:"
    echo "                 |       master:        locally restarts spark master and grid manager"
    echo "                 |       slave:         locally restarts spark slave and grid containers"
    echo "                 |       deploy:        deploys empty space to grid"
    echo "                 |       undeploy:      undeploys space from grid"
    echo "                 |       zeppelin:      locally starts zeppelin"
    echo "                 |       demo:          locally starts datagrid master, datagrid slave and zeppelin, deploys empty space"
    echo "                 |       shutdown:      stops 'master', 'slave' and 'zeppelin'"
    echo " -c, --container |    (slave modes) number of grid containers to start           | default 2"
    echo " -n, --name      |    (deploy/undeploy modes) name of the deployed space         | default insightedge-space"
    echo " -t, --topology  |    (deploy mode) number of space primary and backup instances | default 2,0"
    echo "                 |             format:  <num-of-primaries>,<num-of-backups-per-primary>"
    echo "                 |             example: '4,1' will deploy 8 instances - 4 primary and 4 backups"
    echo ""
    local script="./sbin/$THIS_SCRIPT_NAME"
    echo "Examples:"
    echo "  Restart master |  restarts spark master at spark://127.0.0.1:7077"
    echo "        on local |  restarts spark master web UI at http://127.0.0.1:8080"
    echo "     environment |  restarts grid manager"
    echo "                 |  restarts grid lookup service"
    echo ""
    echo " $script --mode master --master 127.0.0.1"
    echo ""
    echo "   Restart slave |  restarts spark slave that points to master at spark://127.0.0.1:7077"
    echo "        on local |  restarts 2 grid containers"
    echo "     environment |"
    echo ""
    echo " $script --mode slave --master 127.0.0.1"
    echo ""
    echo "    Deploy empty |  deploys insightedge-space with 2 primary instances"
    echo "           space |  deploys insightedge-space with 2 primary instances"
    echo ""
    echo " $script --mode deploy"
    echo ""
    echo "  Undeploy space |  undeploys insightedge-space"
    echo ""
    echo " $script --mode undeploy"
    echo ""
    exit 1
}


local_zeppelin() { ###
    echo ""
    step_title "--- Restarting Zeppelin server"
    helper_stop_zeppelin
    helper_start_zeppelin
    step_title "--- Zeppelin server can be accessed at http://${XAP_NIC_ADDRESS}:9090"
}

helper_stop_zeppelin() {
    step_title "--- Stopping Zeppelin"
    "${XAP_HOME}/insightedge/zeppelin/bin/zeppelin-daemon.sh" stop
}

helper_start_zeppelin() {
    step_title "--- Starting Zeppelin"
    "${XAP_HOME}/insightedge/zeppelin/bin/zeppelin-daemon.sh" start
}


display_demo_help() {
    printf '\e[0;34m\n'
    echo "Demo steps:"
    echo "1. make sure steps above were successfully executed"
    echo "2. Open Web Notebook at http://${XAP_NIC_ADDRESS}:9090 and run any of the available examples"
    printf "\e[0m\n"
}

step_title() {
    printf "\e[32m$1\e[0m\n"
}

error_line() {
    printf "\e[31mError: $1\e[0m\n"
}

function handle_error {
    error_line "$@"
    exit 1
}

display_logo() {
    echo "   _____           _       _     _   ______    _            "
    echo "  |_   _|         (_)     | |   | | |  ____|  | |           "
    echo "    | |  _ __  ___ _  __ _| |__ | |_| |__   __| | __ _  ___ "
    echo "    | | | '_ \\/ __| |/ _\` | '_ \\| __|  __| / _\` |/ _\` |/ _ \\"
    echo "   _| |_| | | \\__ \\ | (_| | | | | |_| |___| (_| | (_| |  __/"
    echo "  |_____|_| |_|___/_|\\__, |_| |_|\\__|______\\__,_|\\__, |\\___|"
    echo "                      __/ |                       __/ |     "
    echo "                     |___/                       |___/   version: $IE_VERSION"
    echo "                                                         edition: $EDITION"
}


# argument must be in format key=value, the function returns the value
get_option_value() {
    local arr=(${1//=/ })
    echo ${arr[1]}
}


main_demo() {
    if [ $# -ne 0 ]; then
        handle_error "demo does not accept parameters"
    fi

    main_shutdown

    echo ""
    step_title "--- Starting Gigaspaces datagrid local node"

    mkdir -p "$INSIGHTEDGE_LOG_DIR"
    local log="$INSIGHTEDGE_LOG_DIR/insightedge-datagrid-local.out"
    echo "Starting ie local"

    XAP_GSA_OPTIONS="$XAP_GSA_OPTIONS -Dinsightedge.marker=master" nohup ${XAP_HOME}/bin/gs-agent.sh --manager-local --spark_master --spark_worker > $log 2>&1 &
    echo "Datagrid master started (log: $log)"

    step_title "--- Gigaspaces datagrid management node started"

    main_deploy_space --topology=1,0 "insightedge-space"

    helper_start_zeppelin

    display_demo_help
}



main_deploy_space() {
    echo ""
    step_title "--- Deploying space"

    parse_deploy_options() {
        while [ "$1" != "" ]; do
          local option="$1"
          case ${option} in
            --topology=*)
              SPACE_TOPOLOGY=$(get_option_value ${option})
              if [ -z "${SPACE_TOPOLOGY}" ]; then handle_error "topology can't be empty"; fi
              ;;
            *)
              echo "Unknown option: ${option}"
              display_usage
              exit
              ;;
          esac
          shift
        done
    }

    await_master_start() {
        TIMEOUT=60
        echo "  awaiting datagrid master ..."
        while [ -z "$(${XAP_HOME}/bin/gs.sh list 2>/dev/null | grep GSM)" ] ; do
            if [ $TIMEOUT -le 0 ]; then
              echo "Datagrid master is not available within timeout"
              return
              #exit 1
            fi
            TIMEOUT=$((TIMEOUT - 10))
#            echo "  .. ($TIMEOUT sec)"
        done
    }

    display_usage() {
        sleep 3
        local script="./sbin/$THIS_SCRIPT_NAME"
        echo ""
        echo "Usage: ${script} deploy-space [--topology=p,b] name"
        echo ""
        echo "Examples:"
        echo "    Deploy space |  deploys 8 primary and 8 backup partitions of 'my-space' on cluster"
        echo ""
        echo " $script deploy-space --topology=8,1 my-space"
        echo ""
#        exit 1
        return
    }

    if [ $# -eq 0 ]; then
        handle_error "space name is missing"
    fi

    local args=( "$@" )

    #last argument is spaceName
    local SPACE_NAME="${args[${#args[@]}-1]}"
    local SPACE_TOPOLOGY="1,0"

#    echo "LAST: ${args[${#args[@]}-1]}"
    unset "args[${#args[@]}-1]"
#    echo "New without last: ${args[@]}"

    parse_deploy_options ${args[@]}

    echo "Deploying space: $SPACE_NAME [$SPACE_TOPOLOGY]"
    await_master_start #TODO: revisit in IE-87
    ${XAP_HOME}/bin/gs.sh deploy-space -cluster schema=partitioned-sync2backup total_members=${SPACE_TOPOLOGY} ${SPACE_NAME}

    step_title "--- Done deploying space"
}

main_undeploy() {
    echo ""
    step_title "--- Undeploying space"

    display_usage() {
        local script="./sbin/$THIS_SCRIPT_NAME"
        sleep 3
        echo ""
        echo "Usage: $script undeploy [name]"
        echo ""
        echo "Examples:"
        echo "  Undeploy space |  undeploys 'my-space' from cluster"
        echo ""
        echo " $script undeploy my-space"
        echo ""
#        exit 1
        return
    }

    local spaceName="$1"

    if [ "$spaceName" == "" ]; then
        #TODO better message
        error_line "space name is missing"
        display_usage
        exit
    elif [ $# -ne 1 ]; then
        #TODO better message
        error_line "too many arguments"
        display_usage
        exit
    fi

    echo "Undeploying space: ${spaceName}"
    ${XAP_HOME}/bin/gs.sh undeploy ${spaceName}

    step_title "--- Done undeploying space"
}

main_shutdown() {
    if [ $# -ne 0 ]; then
        #TODO better error message, maybe add/display usage for shutdown?
        error_line "shutdown does not accept parameters"
        return
    fi

    helper_stop_zeppelin
    helper_stop_ie_master
    helper_stop_ie_worker
}

helper_start_master() {
    display_usage_start_master() {
        sleep 2
        echo ""
        echo "Usage: "
        echo ""
        local script="./sbin/$THIS_SCRIPT_NAME"
        echo " $script run --master"
        echo ""
        return
    }

    check_already_started() {
        pid=`ps aux | grep -v grep | grep insightedge.marker=master | awk '{print $2}'`
        if [ ! -z "$pid" ]; then
            echo "Datagrid master is already running. pid: $pid"
            exit 1
        fi
    }

    if [ $# -ne 0 ]; then
        error_line "run master does not accept parameters"
        display_usage_start_master
        exit 1
    fi

    if [ -z "${XAP_MANAGER_SERVERS}" ]; then
        handle_error "XAP_MANAGER_SERVERS is not set, please refer to the documentation"
    fi

    echo ""
    step_title "--- Starting Gigaspaces datagrid management node"

    check_already_started

    echo "Starting datagrid master"
    XAP_GSA_OPTIONS="$XAP_GSA_OPTIONS -Dinsightedge.marker=master" ${XAP_HOME}/bin/gs-agent.sh --manager --spark_master
}

helper_stop_ie_master() {
    echo ""
    step_title "--- Stopping datagrid master"

    do_stop_ie_master() {
        pid=`ps aux | grep -v grep | grep insightedge.marker=master | awk '{print $2}'`
        if [ -z "$pid" ]; then
            echo "Datagrid master is not running"
            return
#            exit
        fi
        echo "Stopping datagrid master (pid: $pid)..."

        kill -SIGTERM $pid

        TIMEOUT=60
        while ps -p $pid > /dev/null; do
            if [ $TIMEOUT -le 0 ]; then
                echo "Timed out"
                return
            fi
#            echo "  waiting termination ($TIMEOUT sec)"
            ((TIMEOUT--))
            sleep 1
        done
        echo "Datagrid master stopped"
    }

    do_stop_ie_master
    step_title "--- Datagrid master stopped"
}

start_ie_worker() { ####
    echo ""
    step_title "--- Starting Gigaspaces datagrid node"

    display_usage() {
        sleep 3
        echo ""
        echo "Usage: "
        echo " -c, --container |    (slave modes) number of grid containers to start           | default 2"
        echo ""
        local script="./sbin/$THIS_SCRIPT_NAME"
        echo "Examples:"
        echo "  Start datagrid |  starts 8 containers"
        echo ""
        echo " $script -c 8"
        echo ""
#        exit 1
        return
    }

    define_defaults() {
        GSC_COUNT="2"
    }

    parse_options() {
        while [ "$1" != "" ]; do
          case $1 in
            "-c" | "--container")
              shift
              GSC_COUNT=$1
              ;;
            "--mode")
              shift
              ;;
            *)
#              echo "Unknown option: $1"
#              display_usage
              ;;
          esac
          shift
        done
    }


    check_already_started() {
        pid=`ps aux | grep -v grep | grep insightedge.marker=slave | awk '{print $2}'`
        if [ ! -z "$pid" ]; then
            echo "Datagrid slave is already running. pid: $pid"
            return
#            exit
        fi
    }

    define_defaults
    parse_options $@
    check_already_started

    mkdir -p "$INSIGHTEDGE_LOG_DIR"
    local log="$INSIGHTEDGE_LOG_DIR/insightedge-datagrid-slave.out"
    echo "Starting datagrid slave (containers: $GSC_COUNT)"
    XAP_GSA_OPTIONS="$XAP_GSA_OPTIONS -Dinsightedge.marker=slave" nohup ${XAP_HOME}/bin/gs-agent.sh --gsc=$GSC_COUNT --spark_worker  > $log 2>&1 &
    echo "Datagrid slave started (log: $log)"

    step_title "--- Gigaspaces datagrid node started"
}

helper_stop_ie_worker() {

    echo ""
    step_title "--- Stopping datagrid slave instances"

    do_stop_ie_worker() {
        pid=`ps aux | grep -v grep | grep insightedge.marker=slave | awk '{print $2}'`
        if [ -z "$pid" ]; then
            echo "Datagrid slave is not running"
#            exit
            return
        fi
        echo "Stopping datagrid slave (pid: $pid)..."

        kill -SIGTERM $pid

        TIMEOUT=60
        while ps -p $pid > /dev/null; do
            if [ $TIMEOUT -le 0 ]; then
                echo "Timed out"
                break
            fi
#            echo "  waiting termination ($TIMEOUT sec)"
            ((TIMEOUT--))
            sleep 1
        done
        echo "Datagrid slave stopped"
    }

    do_stop_ie_worker
    step_title "--- Datagrid slave instances stopped"
}

main_run() {
    local option=$1
    shift
    case "$option" in
    "")
        display_run_usage
        ;;
    "--master")
        helper_start_master $@
        ;;
    "--worker")
        helper_start_worker
        ;;
    *)
        echo "Unknown command $1"
        exit 1
        ;;
    esac
}

main "$@"