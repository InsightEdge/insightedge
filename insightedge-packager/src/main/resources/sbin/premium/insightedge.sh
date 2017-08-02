#!/bin/bash

DIRNAME=$(dirname ${BASH_SOURCE[0]})
source ${DIRNAME}/common-insightedge.sh

EMPTY="[]"
THIS_SCRIPT_NAME=`basename "$0"`
IE_VERSION=`grep -w "Version" ${XAP_HOME}/insightedge/VERSION | awk -F  ":" '{print $2}' | sed 's/ //'`
EDITION=`grep -w "Edition" ${XAP_HOME}/insightedge/VERSION | awk -F  ":" '{print $2}' | sed 's/ //'`

main() {
    define_defaults
    parse_options $@
    check_options
    redefine_defaults
    display_logo
    case "$MODE" in
      "master")
        local_master $@
        ;;
      "slave")
        local_slave $@
        ;;
      "zeppelin")
        local_zeppelin 127.0.0.1 #TODO
        ;;
      "demo")
        local_master $@
        local_slave $@
        deploy_space $@
        local_zeppelin 127.0.0.1 #TODO
        display_demo_help 127.0.0.1 #TODO
        ;;
      "deploy")
        deploy_space $@
        ;;
      "undeploy")
        undeploy_space $@
        ;;
      "shutdown")
        shutdown_all
        ;;
    esac
}

display_usage() {
    sleep 3
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
    echo " -m, --master    |  * cluster master IP or hostname"
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
    echo " $script --mode deploy --master 127.0.0.1"
    echo ""
    echo "  Undeploy space |  undeploys insightedge-space"
    echo ""
    echo " $script --mode undeploy"
    echo ""
    exit 1
}

define_defaults() {
    # '[]' means 'empty'
    MODE=$EMPTY
    CLUSTER_MASTER=$EMPTY
    GSC_COUNT="2"
    SPACE_NAME="insightedge-space"
    SPACE_TOPOLOGY="2,0"
}

parse_options() {
    while [ "$1" != "" ]; do
      case $1 in
        "--mode")
          shift
          MODE=$1
          ;;
        "-m" | "--master")
          shift
          CLUSTER_MASTER=$1
          ;;
        "-c" | "--container")
          shift
          GSC_COUNT=$1
          ;;
        "-n" | "--name")
          shift
          SPACE_NAME=$1
          ;;
        "-t" | "--topology")
          shift
          SPACE_TOPOLOGY=$1
          ;;
        *)
          echo "Unknown option: $1"
          display_usage
          ;;
      esac
      shift
    done
}

check_options() {
    # check required options
    if [ "$MODE" == "$EMPTY" ]; then
      error_line "--mode is required"
      display_usage
    fi

    if [ $MODE != "master" ] && \
       [ $MODE != "slave" ] && \
       [ $MODE != "zeppelin" ] && \
       [ $MODE != "demo" ] && \
       [ $MODE != "shutdown" ] && \
       [ $MODE != "deploy" ] && \
       [ $MODE != "undeploy" ]; then
         error_line "unknown mode selected with --mode: $MODE"
         display_usage
    fi


    if [ "$CLUSTER_MASTER" == "$EMPTY" ] && [ $MODE != "demo" ] && [ $MODE != "shutdown" ]; then
      error_line "--master is required"
      display_usage
    fi
}

redefine_defaults() {
    if [ "$CLUSTER_MASTER" = "$EMPTY" ]; then
        CLUSTER_MASTER="127.0.0.1"
    fi
}

local_master() {
    stop_grid_master
    stop_spark_master
    start_grid_master $@
    start_spark_master ${CLUSTER_MASTER}
}

local_slave() {

    stop_grid_slave
    stop_spark_slave
    start_grid_slave $@
    start_spark_slave ${CLUSTER_MASTER}
}

deploy_space() {
    echo ""
    step_title "--- Deploying space"

    define_defaults() {
        SPACE_NAME="insightedge-space"
        SPACE_TOPOLOGY="2,0"
    }

    parse_options() {
        while [ "$1" != "" ]; do
          case $1 in
            "-n" | "--name")
              shift
              SPACE_NAME=$1
              ;;
            "-t" | "--topology")
              shift
              SPACE_TOPOLOGY=$1
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
            echo "  .. ($TIMEOUT sec)"
        done
    }

    display_usage() {
        sleep 3
        echo ""
        echo "Usage:"
        echo " -n, --name      |   name of the deployed space                                 | default insightedge-space"
        echo " -t, --topology  |   number of space primary and backup instances               | default 2,0"
        echo "                 |            format:  <num-of-primaries>,<num-of-backups-per-primary>"
        echo "                 |            example: '4,1' will deploy 8 instances - 4 primary and 4 backups"
        echo ""
        local script="./sbin/$THIS_SCRIPT_NAME"
        echo "Examples:"
        echo "    Deploy space |  deploys 8 primary and 8 backup partitions of 'my-space' on cluster"
        echo ""
        echo " $script -n my-space -t 8,1"
        echo ""
#        exit 1
        return
    }

    define_defaults
    parse_options $@

    echo "Deploying space: $SPACE_NAME [$SPACE_TOPOLOGY]"
    await_master_start #TODO: revisit in IE-87
    ${XAP_HOME}/bin/gs.sh deploy-space -cluster schema=partitioned-sync2backup total_members=$SPACE_TOPOLOGY $SPACE_NAME

    step_title "--- Done deploying space"
}

undeploy_space() {
    echo ""
    step_title "--- Undeploying space"

    display_usage() {
        sleep 3
        echo ""
        echo "Usage: * - required"
        echo " -n, --name      |   name of the deployed space                                 | default insightedge-space"
        echo ""
        local script="./sbin/$THIS_SCRIPT_NAME"
        echo "Examples:"
        echo "  Undeploy space |  undeploys 'my-space' from cluster"
        echo ""
        echo " $script -m 10.0.0.1 -n my-space"
        echo ""
#        exit 1
        return
    }

    define_defaults() {
        SPACE_NAME="insightedge-space"
    }

    parse_options() {
        while [ "$1" != "" ]; do
          case $1 in
            "-n" | "--name")
              shift
              SPACE_NAME=$1
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


    define_defaults
    parse_options $@

    echo "Undeploying space: $SPACE_NAME"
    ${XAP_HOME}/bin/gs.sh undeploy $SPACE_NAME

    step_title "--- Done undeploying space"
}

shutdown_all() {
    stop_zeppelin
    stop_grid_master
    stop_grid_slave
    stop_spark_master
    stop_spark_slave
}

start_grid_master() {
    echo ""
    step_title "--- Starting Gigaspaces datagrid management node"

    display_usage() {
        sleep 3
        echo ""
        echo "Usage: "
        echo ""
        local script="./sbin/$THIS_SCRIPT_NAME"
        echo " $script"
        echo ""
        return
#        exit 1
    }

    parse_options() {
        while [ "$1" != "" ]; do
          case $1 in
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
        pid=`ps aux | grep -v grep | grep insightedge.marker=master | awk '{print $2}'`
        if [ ! -z "$pid" ]; then
            echo "Datagrid master is already running. pid: $pid"
#            exit
            return
        fi
    }

    parse_options $@
    check_already_started

    mkdir -p "$INSIGHTEDGE_LOG_DIR"
    local log="$INSIGHTEDGE_LOG_DIR/insightedge-datagrid-master.out"
    echo "Starting datagrid master"
    XAP_GSA_OPTIONS="$XAP_GSA_OPTIONS -Dinsightedge.marker=master" nohup ${XAP_HOME}/bin/gs-agent.sh gsa.gsc 0 gsa.global.gsm 0 gsa.gsm 1 gsa.global.lus 0 gsa.lus 1 > $log 2>&1 &
    echo "Datagrid master started (log: $log)"

    step_title "--- Gigaspaces datagrid management node started"
}

stop_grid_master() {
    echo ""
    step_title "--- Stopping datagrid master"

    do_stop_grid_master() {
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
            break
        fi
            echo "  waiting termination ($TIMEOUT sec)"
            ((TIMEOUT--))
            sleep 1
        done
        echo "Datagrid master stopped"
    }

    do_stop_grid_master
    step_title "--- Datagrid master stopped"
}

start_grid_slave() {
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
    XAP_GSA_OPTIONS="$XAP_GSA_OPTIONS -Dinsightedge.marker=slave" nohup ${XAP_HOME}/bin/gs-agent.sh gsa.gsc $GSC_COUNT gsa.global.gsm 0 gsa.global.lus 0  > $log 2>&1 &
    echo "Datagrid slave started (log: $log)"

    step_title "--- Gigaspaces datagrid node started"
}

stop_grid_slave() {

    echo ""
    step_title "--- Stopping datagrid slave instances"

    do_stop_grid_slave() {
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
            break
        fi
            echo "  waiting termination ($TIMEOUT sec)"
            ((TIMEOUT--))
            sleep 1
        done
        echo "Datagrid slave stopped"
    }

    do_stop_grid_slave
    step_title "--- Datagrid slave instances stopped"
}

main "$@"