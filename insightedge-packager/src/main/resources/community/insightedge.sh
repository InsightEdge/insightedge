#!/bin/bash

if [ -z "${INSIGHTEDGE_HOME}" ]; then
  export INSIGHTEDGE_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi

source $INSIGHTEDGE_HOME/sbin/common-insightedge.sh

EMPTY="[]"
THIS_SCRIPT_NAME=`basename "$0"`
VERSION=`grep "Version" $INSIGHTEDGE_HOME/VERSION | awk -F  ":" '{print $2}' | sed 's/ //'`
EDITION=`grep "Edition" $INSIGHTEDGE_HOME/VERSION | awk -F  ":" '{print $2}' | sed 's/ //'`
ARTIFACT="gigaspaces-insightedge-$VERSION-$EDITION"
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
        local_slave $IE_PATH $IE_INSTALL $ARTIFACT "$ARTIFACT_DOWNLOAD_COMMAND" $CLUSTER_MASTER $GRID_LOCATOR $GRID_GROUP $SPACE_NAME $SPACE_TOPOLOGY $GSC_SIZE
        ;;
      "zeppelin")
        local_zeppelin $IE_PATH $CLUSTER_MASTER
        ;;
      "demo")
        local_master $IE_PATH $IE_INSTALL $ARTIFACT "$ARTIFACT_DOWNLOAD_COMMAND" $CLUSTER_MASTER $GRID_LOCATOR $GRID_GROUP $GSC_SIZE
        local_slave $IE_PATH $IE_INSTALL $ARTIFACT "$ARTIFACT_DOWNLOAD_COMMAND" $CLUSTER_MASTER $GRID_LOCATOR $GRID_GROUP $SPACE_NAME $SPACE_TOPOLOGY $GSC_SIZE
        local_zeppelin $IE_PATH $CLUSTER_MASTER
        display_demo_help $CLUSTER_MASTER
        ;;
      "remote-master")
        remote_master $REMOTE_HOSTS $REMOTE_USER $REMOTE_KEY
        ;;
      "remote-slave")
        remote_slave $REMOTE_HOSTS $REMOTE_USER $REMOTE_KEY
        ;;
      "shutdown")
        shutdown_all $IE_PATH
        ;;
       "describe")
        describe_topology_allocation $SPACE_TOPOLOGY $HOSTS_COUNT
        ;;
    esac
}

display_usage() {
    sleep 3
    echo ""
    display_logo
    echo ""
    echo "Usage: * - required, ** - required in some modes"
    echo "     --mode      |  * insightedge mode:"
    echo "                 |       master:         locally restarts spark master and Lookup Service"
    echo "                 |       slave:          locally restarts spark slave and space partitions"
    echo "                 |       zeppelin:       locally starts zeppelin"
    echo "                 |       demo:           locally starts Lookup Service, zeppelin, space partitions"
    echo "                 |       remote-master:  executes 'master' mode on remote system (use for automation)"
    echo "                 |       remote-slave:   executes 'slave' mode on remote system (use for automation)"
    echo "                 |       shutdown:       stops 'master', 'slave' and 'zeppelin'"
    echo "                 |       describe:       prints grid allocation for given topology and hosts"
    echo " -m, --master    |  * cluster master IP or hostname"
    echo " -l, --locator   |    lookup locators for the grid components                    | default master:4174"
    echo " -g, --group     |    lookup groups for the grid components                      | default insightedge"
    echo "                 |               usage: if you have several clusters in one LAN,"
    echo "                 |                      it's recommended to have unique group per cluster"
    echo " -s, --size      |    grid container/manager heap size                           | default 1G"
    echo "                 |             format:  java-style heap size string"
    echo "                 |             example: '1G', '4096M'"
    echo " -n, --name      |    name of the deployed space                                 | default insightedge-space"
    echo " -t, --topology  |    number of space primary and backup instances               | default 2,0"
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
    echo "     environment |  restarts Lookup Service at 127.0.0.1:4174 with group 'insightedge'"
    echo ""
    echo " $script --mode master --path \$INSIGHTEDGE_HOME --master 127.0.0.1"
    echo ""
    echo "   Restart slave |  restarts spark slave that points to master at spark://127.0.0.1:7077"
    echo "        on local |  restarts 2 'insightedge-space' space partitions with 1G heap size each"
    echo "     environment |"
    echo ""
    echo " $script --mode slave --path \$INSIGHTEDGE_HOME --master 127.0.0.1"
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
    echo " $script --mode remote-master --hosts \$MASTER \\"
    echo "   --user insightedge --key ~/.shh/dev-env.pem \\"
    echo "   --path \$IEPATH --master \$MASTER --group dev-env --size 2G"
    echo " $script --mode remote-slave --hosts \$SLAVES \\"
    echo "   --user insightedge --key ~/.shh/dev-env.pem \\"
    echo "   --path \$IEPATH --master \$MASTER --group dev-env --size 2G --container 4"
    echo ""
    echo "  Show grid topology allocation for specified topology"
    echo ""
    echo " $script --mode describe --topology 3,2 --count 3"
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
    SPACE_NAME="insightedge-space"
    SPACE_TOPOLOGY="2,0"
    HOSTS_COUNT=2
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
        "-c" | "--count")
          shift
          HOSTS_COUNT=$1
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
          error_line "unknown option: $1"
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
       [ $MODE != "remote-master" ] && \
       [ $MODE != "remote-slave" ] && \
       [ $MODE != "shutdown" ] && \
       [ $MODE != "describe" ]; then
         error_line "unknown mode selected with --mode: $MODE"
         display_usage
    fi

    if [ "$CLUSTER_MASTER" == "$EMPTY" ] && [ $MODE != "demo" ] && [ $MODE != "shutdown" ] && [ $MODE != "describe" ]; then
      error_line "--master is required"
      display_usage
    fi

    if [[ $MODE == remote-* ]]; then
        if [ "$REMOTE_USER" == "$EMPTY" ]; then
            error_line "--user is required in remote modes"
            display_usage
        fi
        if [ "$REMOTE_HOSTS" == "$EMPTY" ]; then
            error_line "--hosts is required in remote modes"
            display_usage
        fi
        if [ "$IE_PATH" == "$EMPTY" ]; then
          error_line "--path is required"
          display_usage
        fi
    fi
}

redefine_defaults() {
    if [ $CLUSTER_MASTER = "$EMPTY" ]; then
        CLUSTER_MASTER="127.0.0.1"
    fi
    if [ $GRID_LOCATOR = "$EMPTY" ]; then
        GRID_LOCATOR="$CLUSTER_MASTER:4174"
    fi
    if [ $IE_PATH = "$EMPTY" ]; then
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
    local space_name=$8
    local topology=$9
    local size=${10}
    local instances=${11}

    if [[ -z $instances ]]; then
        instances=`java -cp "$home/lib/*" com.gigaspaces.spark.utils.GridTopologyAllocator "$topology" ""`
        if [[ $instances == ERROR* ]]; then
            error_line "$instances"
            exit 1
        fi
    fi

    install_insightedge $install $artifact "$download" $home
    stop_grid_slave $home
    stop_spark_slave $home
    start_grid_slave $home $master $locator $group $space_name $topology $size $instances
    start_spark_slave $home $master
}

remote_master() {
    local hosts=${1//,/ }
    local user=$2
    local key=$3

    local args="$IE_PATH $IE_INSTALL $ARTIFACT \"$ARTIFACT_DOWNLOAD_COMMAND\" $CLUSTER_MASTER $GRID_LOCATOR $GRID_GROUP $GSC_SIZE"
    for host in $hosts; do
        echo ""
        step_title "---- Connecting to master at $host"
        if [ "$key" == "$EMPTY" ]; then
          ssh -oStrictHostKeyChecking=no $user@$host "$(typeset -f); local_master $args"
        else
          ssh -i $key -oStrictHostKeyChecking=no $user@$host "$(typeset -f); local_master $args"
        fi
        echo ""
        step_title "---- Disconnected from $host"
    done
}

remote_slave() {
    local hosts=$1
    local user=$2
    local key=$3

    hosts_to_instances=`java -cp "$INSIGHTEDGE_HOME/lib/*" com.gigaspaces.spark.utils.GridTopologyAllocator "$SPACE_TOPOLOGY" "$hosts"`
    if [[ $hosts_to_instances == ERROR* ]]; then
        error_line "$hosts_to_instances"
        exit 1
    else
        echo "Grid topology: $hosts_to_instances"
    fi

    local args="$IE_PATH $IE_INSTALL $ARTIFACT \"$ARTIFACT_DOWNLOAD_COMMAND\" $CLUSTER_MASTER $GRID_LOCATOR $GRID_GROUP $SPACE_NAME $SPACE_TOPOLOGY $GSC_SIZE"
    for host_to_instance_raw in $hosts_to_instances; do
        IFS=':' read -r -a host_to_instance <<< "$host_to_instance_raw"
        host=${host_to_instance[0]}
        instances=${host_to_instance[1]}
        echo ""
        step_title "---- Connecting to slave at $host"
        if [ "$key" == "$EMPTY" ]; then
          ssh -oStrictHostKeyChecking=no $user@$host "$(typeset -f); local_slave $args \"$instances\""
        else
          ssh -i $key -oStrictHostKeyChecking=no $user@$host "$(typeset -f); local_slave $args \"$instances\""
        fi
        echo ""
        step_title "---- Disconnected from $host"
    done
}


shutdown_all() {
    local home=$1

    stop_zeppelin $home
    stop_grid_master $home
    stop_grid_slave $home
    stop_spark_master $home
    stop_spark_slave $home
}

start_grid_master() {
    local home=$1
    local locator=$2
    local group=$3
    local size=$4

    step_title "--- Starting Gigaspaces datagrid management node (locator: $locator, group: $group, heap: $size)"
    $home/sbin/start-datagrid-master.sh --master $master --locator $locator --group $group --size $size
    step_title "--- Gigaspaces datagrid management node started"
}

stop_grid_master() {
    local home=$1

    echo ""
    step_title "--- Stopping datagrid master"
    $home/sbin/stop-datagrid-master.sh
    step_title "--- Datagrid master stopped"
}

start_grid_slave() {
    local home=$1
    local master=$2
    local locator=$3
    local group=$4
    local space_name=$5
    local topology=$6
    local size=$7
    local instances=$8

    echo ""
    step_title "--- Starting Gigaspaces datagrid instances (locator: $locator, group: $group, heap: $size, instances: $instances)"
    $home/sbin/start-datagrid-slave.sh --master $master --locator $locator --group $group --name $space_name --topology $topology --size $size --instances $instances
    step_title "--- Gigaspaces datagrid instances started"
}

stop_grid_slave() {
    local home=$1

    echo ""
    step_title "--- Stopping datagrid slave instances"
    $home/sbin/stop-datagrid-slave.sh
    step_title "--- Datagrid slave instances stopped"
}

describe_topology_allocation() {
    local topology=$1
    local hosts_count=$2

    local hosts="host_1"
    for i in `seq 2 $hosts_count`; do
        hosts="$hosts,host_$i"
    done
    echo "Allocated topology for $hosts_count hosts, topology $topology"
    IE_PATH="/code/insightedge/insightedge-packager/target/contents-community"
    instances=`java -cp "$IE_PATH/lib/*" com.gigaspaces.spark.utils.GridTopologyAllocator "$topology" "$hosts"`
    for ins in $instances; do
        echo $ins
    done
}

main "$@"