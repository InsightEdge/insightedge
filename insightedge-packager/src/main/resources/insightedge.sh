#!/bin/bash

DISTRO_CE="CE"
DISTRO_PREMIUM="PREMIUM"

DISTRO="PREMIUM" #TODO how to set param DISTO?

if [ -z "${INSIGHTEDGE_HOME}" ]; then
  export INSIGHTEDGE_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi

EMPTY="[]"
THIS_SCRIPT_NAME=`basename "$0"`
VERSION="0.4.0-SNAPSHOT"
ARTIFACT="gigaspaces-insightedge-$VERSION"
ARTIFACT_EC2="https://s3.amazonaws.com/insightedge/builds/gigaspaces-insightedge-0.4.0-SNAPSHOT.zip"

# override this variable with custom command if you want your distribution to be downloaded from custom location
# for customization, call before insightedge.sh:
# export ARTIFACT_DOWNLOAD_COMMAND="curl -L -O http://.../gigaspaces-insightedge.zip"
if [ -z "${ARTIFACT_DOWNLOAD_COMMAND}" ]; then
  export ARTIFACT_DOWNLOAD_COMMAND="curl -L -O $ARTIFACT_EC2"
fi

main() {
    define_defaults
    parse_options $@
    check_options
    redefine_defaults
    display_logo
    case "$MODE" in
      "master")
        local_master $IE_PATH $IE_INSTALL $ARTIFACT "$ARTIFACT_DOWNLOAD_COMMAND" $CLUSTER_MASTER $GRID_LOCATOR $GRID_GROUP $GSC_SIZE
        ;;
      "slave")
        if [[ "$DISTRO" == "$DISTRO_CE" ]]; then
            local_slave_ce $IE_PATH $IE_INSTALL $ARTIFACT "$ARTIFACT_DOWNLOAD_COMMAND" $CLUSTER_MASTER $GRID_LOCATOR $GRID_GROUP $SPACE_NAME $SPACE_TOPOLOGY $GSC_SIZE
        elif [[ "$DISTRO" == "$DISTRO_PREMIUM" ]]; then
            local_slave $IE_PATH $IE_INSTALL $ARTIFACT "$ARTIFACT_DOWNLOAD_COMMAND" $CLUSTER_MASTER $GRID_LOCATOR $GRID_GROUP $GSC_COUNT $GSC_SIZE
        fi
        ;;
      "zeppelin")
        local_zeppelin $IE_PATH $CLUSTER_MASTER
        ;;
      "demo")
        local_master $IE_PATH $IE_INSTALL $ARTIFACT "$ARTIFACT_DOWNLOAD_COMMAND" $CLUSTER_MASTER $GRID_LOCATOR $GRID_GROUP $GSC_SIZE
        if [[ "$DISTRO" == "$DISTRO_CE" ]]; then
            local_slave_ce $IE_PATH $IE_INSTALL $ARTIFACT "$ARTIFACT_DOWNLOAD_COMMAND" $CLUSTER_MASTER $GRID_LOCATOR $GRID_GROUP $SPACE_NAME $SPACE_TOPOLOGY $GSC_SIZE
        elif [[ "$DISTRO" == "$DISTRO_PREMIUM" ]]; then
            local_slave $IE_PATH $IE_INSTALL $ARTIFACT "$ARTIFACT_DOWNLOAD_COMMAND" $CLUSTER_MASTER $GRID_LOCATOR $GRID_GROUP $GSC_COUNT $GSC_SIZE
        fi
        deploy_space $IE_PATH $GRID_LOCATOR $GRID_GROUP $SPACE_NAME $SPACE_TOPOLOGY
        local_zeppelin $IE_PATH $CLUSTER_MASTER
        display_demo_help $CLUSTER_MASTER
        ;;
      "remote-master")
        remote_master $REMOTE_HOSTS $REMOTE_USER $REMOTE_KEY
        ;;
      "remote-slave")
        remote_slave $REMOTE_HOSTS $REMOTE_USER $REMOTE_KEY
        ;;
      "deploy")
        if [[ "$DISTRO" == "$DISTRO_CE" ]]; then
            deploy_space_ce $IE_PATH $GRID_LOCATOR $GRID_GROUP $SPACE_NAME $SPACE_TOPOLOGY $CLUSTER_MASTER $GSC_SIZE
        elif [[ "$DISTRO" == "$DISTRO_PREMIUM" ]]; then
            deploy_space $IE_PATH $GRID_LOCATOR $GRID_GROUP $SPACE_NAME $SPACE_TOPOLOGY
        fi
        ;;
      "undeploy")
        undeploy_space $IE_PATH $GRID_LOCATOR $GRID_GROUP $SPACE_NAME
        ;;
      "shutdown")
        shutdown_all $IE_PATH
        ;;
    esac
}

display_usage() {
    sleep 3
    echo ""
    display_logo
    echo ""
    echo "Usage: * - required, ** - required in some modes"
    echo "     --mode      |  * insightedge mode: master, slave, deploy, remote-master, remote-slave"
    echo "                 |       master:        locally restarts spark master and grid manager"
    echo "                 |       slave:         locally restarts spark slave and grid containers"
    echo "                 |       deploy:        deploys empty space to grid"
    echo "                 |       undeploy:      undeploys space from grid"
    echo "                 |       zeppelin:      locally starts zeppelin"
    echo "                 |       demo:          locally starts datagrid master, datagrid slave and zeppelin, deploys empty space"
    echo "                 |       remote-master: executes 'master' mode on remote system (use for automation)"
    echo "                 |       remote-slave:  executes 'slave' mode on remote system (use for automation)"
    echo "                 |       shutdown:      stops 'master', 'slave' and 'zeppelin'"
    echo " -m, --master    |  * cluster master IP or hostname"
    echo " -l, --locator   |    lookup locators for the grid components                    | default master:4174"
    echo " -g, --group     |    lookup groups for the grid components                      | default insightedge"
    echo "                 |               usage: if you have several clusters in one LAN,"
    echo "                 |                      it's recommended to have unique group per cluster"
    echo " -s, --size      |    grid container/manager heap size                           | default 1G"
    echo "                 |             format:  java-style heap size string"
    echo "                 |             example: '1G', '4096M'"
    echo " -c, --container |    (slave modes) number of grid containers to start           | default 2"
    echo " -n, --name      |    (deploy/undeploy modes) name of the deployed space         | default insightedge-space"
    echo " -t, --topology  |    (deploy mode) number of space primary and backup instances | default 2,0"
    echo "                 |             format:  <num-of-primaries>,<num-of-backups-per-primary>"
    echo "                 |             example: '4,1' will deploy 8 instances - 4 primary and 4 backups"
    echo " -p, --path      | ** (remote modes) path to insightedge installation"
    echo " -i, --install   |    if specified, a fresh insightedge distribution will be installed to specified path"
    echo "                 |             warning: folder specified in path will be fully removed and recreated"
    echo " -h, --hosts     | ** (remote modes) comma separated list of remote nodes: IPs or hostnames"
    echo " -u, --user      | ** (remote modes) username"
    echo " -k, --key       |    (remote modes) identity file"
    echo ""
    local script="./sbin/$THIS_SCRIPT_NAME"
    echo "Examples:"
    echo "  Restart master |  restarts spark master at spark://127.0.0.1:7077"
    echo "        on local |  restarts spark master web UI at http://127.0.0.1:8080"
    echo "     environment |  restarts grid manager with 1G heap size"
    echo "                 |  restarts grid lookup service at 127.0.0.1:4174 with group 'insightedge'"
    echo ""
    echo " $script --mode master --path \$INSIGHTEDGE_HOME --master 127.0.0.1"
    echo ""
    echo "   Restart slave |  restarts spark slave that points to master at spark://127.0.0.1:7077"
    echo "        on local |  restarts 2 grid containers with 1G heap size"
    echo "     environment |"
    echo ""
    echo " $script --mode slave --path \$INSIGHTEDGE_HOME --master 127.0.0.1"
    echo ""
    echo "    Deploy empty |  deploys insightedge-space with 2 primary instances"
    echo "           space |  deploys insightedge-space with 2 primary instances"
    echo "                 |  cluster is searched with 127.0.0.1:4174 locator and 'insightedge' group"
    echo ""
    echo " $script --mode deploy --path \$INSIGHTEDGE_HOME --master 127.0.0.1"
    echo ""
    echo "  Undeploy space |  undeploys insightedge-space"
    echo "                 |  cluster is searched with 127.0.0.1:4174 locator and 'insightedge' group"
    echo ""
    echo " $script --mode undeploy --path \$INSIGHTEDGE_HOME --master 127.0.0.1"
    echo ""
    echo "  Remote install |  connects to remote via ssh"
    echo "        on slave |  installs insightedge to home folder"
    echo ""
    echo " $script --mode remote-slave --hosts 10.0.0.2 \\"
    echo "   --user insightedge --key ~/.shh/dev-env.pem \\"
    echo "   --install --path ~/$ARTIFACT --master 10.0.0.1"
    echo ""
    echo " Cluster restart |  connects to cluster members via ssh"
    echo "      automation |  restarts components"
    echo ""
    echo " MASTER=10.0.0.1"
    echo " SLAVES=10.0.0.2,10.0.0.3,10.0.0.4"
    echo " IEPATH=~/$ARTIFACT"
    echo " $script --mode undeploy \\"
    echo "   --master \$MASTER --group dev-env --name insightedge-dev-space"
    echo " $script --mode remote-master --hosts \$MASTER \\"
    echo "   --user insightedge --key ~/.shh/dev-env.pem \\"
    echo "   --path \$IEPATH --master \$MASTER --group dev-env --size 2G"
    echo " $script --mode remote-slave --hosts \$SLAVES \\"
    echo "   --user insightedge --key ~/.shh/dev-env.pem \\"
    echo "   --path \$IEPATH --master \$MASTER --group dev-env --size 2G --container 4"
    echo " $script --mode deploy \\"
    echo "   --master \$MASTER --group dev-env --name insightedge-dev-space --topology 12,0"
    echo ""
    exit 1
}

define_defaults() {
    MODE=$EMPTY
    IE_PATH=$EMPTY
    IE_INSTALL="false"
    CLUSTER_MASTER=$EMPTY
    GRID_LOCATOR=$EMPTY
    GRID_GROUP="insightedge"
    GSC_SIZE="1G"
    GSC_COUNT="2"
    SPACE_NAME="insightedge-space"
    SPACE_TOPOLOGY="2,0"
    REMOTE_HOSTS=$EMPTY
    REMOTE_USER=$EMPTY
    REMOTE_KEY=$EMPTY
}

parse_options() {
    while [ "$1" != "" ]; do
      case $1 in
        "--mode")
          shift
          MODE=$1
          ;;
        "-p" | "--path")
          shift
          IE_PATH=$1
          ;;
        "-i" | "--install")
          IE_INSTALL="true"
          ;;
        "-m" | "--master")
          shift
          CLUSTER_MASTER=$1
          ;;
        "-l" | "--locator")
          shift
          GRID_LOCATOR=$1
          ;;
        "-g" | "--group")
          shift
          GRID_GROUP=$1
          ;;
        "-s" | "--size")
          shift
          GSC_SIZE=$1
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
        "-h" | "--hosts")
          shift
          REMOTE_HOSTS=$1
          ;;
        "-u" | "--user")
          shift
          REMOTE_USER=$1
          ;;
        "-k" | "--key")
          shift
          REMOTE_KEY=$1
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

    if [[ ("$DISTRO" != "$DISTRO_CE") && ("$DISTRO" != "$DISTRO_PREMIUM") ]]; then
        echo "Wrong distribution parameter: $DISTRO"
        exit 1
    fi

    if [ $MODE == $EMPTY ]; then
      error_line "--mode is required"
      display_usage
    fi

    if [ $MODE != "master" ] && \
       [ $MODE != "slave" ] && \
       [ $MODE != "zeppelin" ] && \
       [ $MODE != "demo" ] && \
       [ $MODE != "remote-master" ] && \
       [ $MODE != "remote-slave" ] && \
       [ $MODE != "remote-slave" ] && \
       [ $MODE != "shutdown" ] && \
       [ $MODE != "deploy" ] && \
       [ $MODE != "undeploy" ]; then
         error_line "unknown mode selected with --mode: $MODE"
         display_usage
    fi

    if [ $CLUSTER_MASTER == $EMPTY ] && [ $MODE != "demo" ] && [ $MODE != "shutdown" ]; then
      error_line "--master is required"
      display_usage
    fi

    if [[ $MODE == remote-* ]]; then
        if [ $REMOTE_USER == $EMPTY ]; then
            error_line "--user is required in remote modes"
            display_usage
        fi
        if [ $REMOTE_HOSTS == $EMPTY ]; then
            error_line "--hosts is required in remote modes"
            display_usage
        fi
        if [ $IE_PATH == $EMPTY ]; then
          error_line "--path is required"
          display_usage
        fi
    fi
}

redefine_defaults() {
    if [ $CLUSTER_MASTER = $EMPTY ]; then
        CLUSTER_MASTER="127.0.0.1"
    fi
    if [ $GRID_LOCATOR = $EMPTY ]; then
        GRID_LOCATOR="$CLUSTER_MASTER:4174"
    fi
    if [ $IE_PATH = $EMPTY ]; then
        IE_PATH="$INSIGHTEDGE_HOME"
    fi
}

local_master() {
    local home=$1
    local install=$2
    local artifact=$3
    local download=$4
    local master=$5
    local locator=$6
    local group=$7
    local size=$8

    install_insightedge $install $artifact "$download" $home
    stop_grid_master $home
    stop_spark_master $home
    start_grid_master $home $locator $group $size
    start_spark_master $home $master
}

local_slave() {
    local home=$1
    local install=$2
    local artifact=$3
    local download=$4
    local master=$5
    local locator=$6
    local group=$7
    local containers=$8
    local size=$9

    install_insightedge $install $artifact "$download" $home
    stop_grid_slave $home
    stop_spark_slave $home
    start_grid_slave $home $master $locator $group $containers $size
    start_spark_slave $home $master
}

local_slave_ce() {
    local home=$1
    local install=$2
    local artifact=$3
    local download=$4
    local master=$5
    local locator=$6
    local group=$7
    local space_name=$8
    local topology=$9
    local size=${10}
    local instances=${11}

    if [[ -z $instances ]]; then
        instances=`java -cp "$home/lib/*" com.gigaspaces.spark.packager.ResolverRunner "" "$topology" "single"`
        if [[ $instances == ERROR* ]]; then
            echo "$instances"
            exit 1
        fi
    fi

    install_insightedge $install $artifact "$download" $home
    stop_grid_slave $home
    stop_spark_slave $home
    start_grid_slave_ce $home $master $locator $group $space_name $topology $size $instances
    start_spark_slave $home $master
}

local_zeppelin() {
    local home=$1
    local master=$2
    echo ""
    step_title "--- Restarting Zeppelin server"
    stop_zeppelin $1
    start_zeppelin $1
    step_title "--- Zeppelin server can be accessed at http://$master:8090"
}

remote_master() {
    local hosts=${1//,/ }
    local user=$2
    local key=$3

    local args="$IE_PATH $IE_INSTALL $ARTIFACT \"$ARTIFACT_DOWNLOAD_COMMAND\" $CLUSTER_MASTER $GRID_LOCATOR $GRID_GROUP $GSC_SIZE"
    for host in $hosts; do
        echo ""
        step_title "---- Connecting to master at $host"
        if [ $key == $EMPTY ]; then
          ssh $user@$host "$(typeset -f); local_master $args"
        else
          ssh -i $key $user@$host "$(typeset -f); local_master $args"
        fi
        echo ""
        step_title "---- Disconnected from $host"
    done
}

remote_slave() {
    local hosts=$1
    local user=$2
    local key=$3

    if [[ "$DISTRO" == "$DISTRO_CE" ]]; then
        remote_slave_ce $1 $2 $3
    elif [[ "$DISTRO" == "$DISTRO_PREMIUM" ]]; then
        remote_slave_premium $1 $2 $3
    fi
}

remote_slave_ce() {
    local hosts=${1//,/ }
    local user=$2
    local key=$3

    hosts_to_instances=`java -cp "/code/insightedge/insightedge-packager/target/gigaspaces-insightedge-0.4.0-SNAPSHOT/lib/*" com.gigaspaces.spark.packager.ResolverRunner "$1" "$SPACE_TOPOLOGY" "multiple"`
    if [[ $hosts_to_instances == ERROR* ]]; then
        echo "$hosts_to_instances"
        exit 1
    else
        echo "Hosts to instances: $hosts_to_instances"
    fi

    local args="$IE_PATH $IE_INSTALL $ARTIFACT \"$ARTIFACT_DOWNLOAD_COMMAND\" $CLUSTER_MASTER $GRID_LOCATOR $GRID_GROUP $SPACE_NAME $SPACE_TOPOLOGY $GSC_SIZE"
    for host_to_instance_raw in $hosts_to_instances; do
        IFS=':' read -r -a host_to_instance <<< "$host_to_instance_raw"
        host=${host_to_instance[0]}
        instances=${host_to_instance[1]}
        echo ""
        step_title "---- Connecting to slave at $host"
        if [ $key == $EMPTY ]; then
          ssh $user@$host "$(typeset -f); local_slave_ce $args \"$instances\""
        else
          ssh -i $key $user@$host "$(typeset -f); local_slave_ce $args \"$instances\""
        fi
        echo ""
        step_title "---- Disconnected from $host"
    done
}

remote_slave_premium() {
    local hosts=${1//,/ }
    local user=$2
    local key=$3

    local args="$IE_PATH $IE_INSTALL $ARTIFACT \"$ARTIFACT_DOWNLOAD_COMMAND\" $CLUSTER_MASTER $GRID_LOCATOR $GRID_GROUP $GSC_COUNT $GSC_SIZE"
    for host in $hosts; do
        echo ""
        step_title "---- Connecting to slave at $host"
        if [ $key == $EMPTY ]; then
          ssh $user@$host "$(typeset -f); local_slave $args"
        else
          ssh -i $key $user@$host "$(typeset -f); local_slave $args"
        fi
        echo ""
        step_title "---- Disconnected from $host"
    done
}

deploy_space() {
    local home=$1
    local locator=$2
    local group=$3
    local space=$4
    local topology=$5

    if [[ "$DISTRO" == "$DISTRO_PREMIUM" ]]; then
        echo ""
        step_title "--- Deploying space: $space [$topology] (locator: $locator, group: $group)"
        $home/sbin/deploy-datagrid.sh --locator $locator --group $group --name $space --topology $topology
        step_title "--- Done deploying space: $space"
    fi
}

deploy_space_ce() {
    local home=$1
    local master=$2
    local locator=$3
    local group=$4
    local space_name=$5
    local topology=$6
    local size=$7

    instances=`java -cp "$home/lib/*" com.gigaspaces.spark.packager.ResolverRunner "" "$topology" "single"`
    if [[ $instances == ERROR* ]]; then
        echo "$instances"
        exit 1
    fi
    start_grid_slave_ce $home $master $locator $group $space_name $topology $size $instances
}

undeploy_space() {
    local home=$1
    local locator=$2
    local group=$3
    local space=$4

    echo ""
    if [[ "$DISTRO" == "$DISTRO_CE" ]]; then
        stop_grid_slave $home
    elif [[ "$DISTRO" == "$DISTRO_PREMIUM" ]]; then
        step_title "--- Undeploying space: $space (locator: $locator, group: $group)"
        $home/sbin/undeploy-datagrid.sh --locator $locator --group $group --name $space
        step_title "--- Done undeploying space: $space"
    fi
}

shutdown_all() {
    local home=$1

    echo ""
    stop_zeppelin $home
    echo ""
    stop_grid_master $home
    echo ""
    stop_grid_slave $home
    echo ""
    stop_spark_master $home
    echo ""
    stop_spark_slave $home
}

stop_zeppelin() {
    local home=$1
    step_title "--- Stopping Zeppelin"
    $home/sbin/stop-zeppelin.sh
}

start_zeppelin() {
    local home=$1
    step_title "--- Starting Zeppelin"
    $home/sbin/start-zeppelin.sh
}

install_insightedge() {
    local install=$1
    local artifact=$2
    local command=$3
    local home=$4

    if [ $install == "false" ]; then
        return
    fi

    echo ""
    step_title "--- Installing InsightEdge ($2) at $home"
    echo "- Cleaning up $home"
    rm -rf $home
    mkdir $home
    cd $home
    echo "- Downloading $artifact"
    eval $command
    echo "- Unpacking $artifact"
    unzip ${artifact}.zip > insightedge-unzip.log
    rm ${artifact}.zip
    echo "- Extracting files from subfolder"
    mv ${artifact}/** .
    echo "- Removing $artifact bundle"
    rm -rf ${artifact}
    step_title "--- Installation complete"
}

start_grid_master() {
    local home=$1
    local locator=$2
    local group=$3
    local size=$4

    step_title "--- Starting Gigaspaces datagrid management node (locator: $locator, group: $group, heap: $size)"
    if [[ "$DISTRO" == "$DISTRO_CE" ]]; then
        $home/sbin/start-datagrid-master-ce.sh --master $master --locator $locator --group $group --size $size
    elif [[ "$DISTRO" == "$DISTRO_PREMIUM" ]]; then
        $home/sbin/start-datagrid-master.sh --master $master --locator $locator --group $group --size $size
    fi
    step_title "--- Gigaspaces datagrid management node started"
}

stop_grid_master() {
    local home=$1

    echo ""
    step_title "--- Stopping datagrid master"
    $home/sbin/stop-datagrid-master.sh
    step_title "--- Datagrid master stopped"
}

start_grid_slave_ce() {
    local home=$1
    local master=$2
    local locator=$3
    local group=$4
    local space_name=$5
    local topology=$6
    local size=$7
    local instances=$8

    step_title "--- Starting Gigaspaces datagrid instances (locator: $locator, group: $group, heap: $size, instances: $instances)"
    $home/sbin/start-datagrid-slave-ce.sh --master $master --locator $locator --group $group --name $space_name --topology $topology --size $size --instances $instances
    step_title "--- Gigaspaces datagrid instances started"
}

start_grid_slave() {
    local home=$1
    local master=$2
    local locator=$3
    local group=$4
    local containers=$5
    local size=$6

    step_title "--- Starting Gigaspaces datagrid node (locator: $locator, group: $group, heap: $size, containers: $containers)"
    $home/sbin/start-datagrid-slave.sh --master $master --locator $locator --group $group --container $containers --size $size
    step_title "--- Gigaspaces datagrid node started"
}

stop_grid_slave() {
    local home=$1

    echo ""
    if [[ "$DISTRO" == "$DISTRO_CE" ]]; then
        step_title "--- Stopping datagrid slave instances"
        $home/sbin/stop-datagrid-slave-ce.sh
        step_title "--- Datagrid slave instances stopped"
    elif [[ "$DISTRO" == "$DISTRO_PREMIUM" ]]; then
        step_title "--- Stopping datagrid slave"
        $home/sbin/stop-datagrid-slave.sh
        step_title "--- Datagrid slave stopped"
    fi

}

start_spark_master() {
    local home=$1
    local master=$2

    echo ""
    step_title "--- Starting Spark master at $master"
    $home/sbin/start-master.sh -h $master
    step_title "--- Spark master started"
}

stop_spark_master() {
    local home=$1

    echo ""
    step_title "--- Stopping Spark master"
    $home/sbin/stop-master.sh
    step_title "--- Spark master stopped"
}

start_spark_slave() {
    local home=$1
    local master=$2

    echo ""
    step_title "--- Starting Spark slave"
    $home/sbin/start-slave.sh spark://$master:7077
    step_title "--- Spark slave started"
}

stop_spark_slave() {
    local home=$1

    echo ""
    step_title "--- Stopping Spark slave"
    $home/sbin/stop-slave.sh
    step_title "--- Spark slave stopped"
}

display_demo_help() {
    local master=$1

    printf '\e[0;34m\n'
    echo "Demo steps:"
    echo "1. make sure steps above were successfully executed"
    echo "2. Open Web Notebook at http://$master:8090 and run any of the available examples"
    printf "\e[0m\n"
}

step_title() {
    printf "\e[32m$1\e[0m\n"
}

error_line() {
    printf "\e[31mError: $1\e[0m\n"
}

display_logo() {
    echo "   _____           _       _     _   ______    _            "
    echo "  |_   _|         (_)     | |   | | |  ____|  | |           "
    echo "    | |  _ __  ___ _  __ _| |__ | |_| |__   __| | __ _  ___ "
    echo "    | | | '_ \\/ __| |/ _\` | '_ \\| __|  __| / _\` |/ _\` |/ _ \\"
    echo "   _| |_| | | \\__ \\ | (_| | | | | |_| |___| (_| | (_| |  __/"
    echo "  |_____|_| |_|___/_|\\__, |_| |_|\\__|______\\__,_|\\__, |\\___|"
    echo "                      __/ |                       __/ |     "
    echo "                     |___/                       |___/   version $VERSION"
}

main "$@"