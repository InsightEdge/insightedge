#!/bin/bash
#
# Wrapper around gsInstance.sh script to make space deployment easier.
#
# Logs are stored in XAP_DIR/logs folder.
#
# If something does not work, blame Danylo Hurin

if [ -z "${INSIGHTEDGE_HOME}" ]; then
  export INSIGHTEDGE_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi

INSIGHTEDGE_HOME="/code/insightedge/insightedge-packager/target/gigaspaces-insightedge-0.4.0-SNAPSHOT"

THIS_SCRIPT_NAME=`basename "$0"`

SPACE_NAME="insightedge-space"
CLUSTER_SCHEMA="partitioned-sync2backup"
INSTANCES="2"
BACKUPS="1"
GRID_GROUP="insightedge"
GRID_LOCATOR=""
INSTANCE_HEAP="512M"
ADDITIONAL_CLASSPATH=""
ADDITIONAL_ARGUMENTS=""

main() {
    parse_arguments "$@"
    verify_arguments
    print_arguments
    start_instances
}

parse_arguments() {
    while [ "$1" != "" ]; do
      case $1 in
        "-n" | "--name")
          shift
          SPACE_NAME=$1
          ;;
        "-t" | "--topology")
          shift
          CLUSTER_SCHEMA=$1
          ;;
        "-i" | "--instances")
          shift
          INSTANCES=$1
          ;;
        "-b" | "--backups")
          shift
          BACKUPS=$1
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
          INSTANCE_HEAP=$1
          ;;
        "-c" | "--classpath")
          shift
          ADDITIONAL_CLASSPATH=$1
          ;;
        "-a" | "--arguments")
          shift
          ADDITIONAL_ARGUMENTS=$1
          ;;
        *)
          echo "Unknown option: $1"
          display_usage
          ;;
      esac
      shift
    done
}

verify_arguments() {
    is_number $INSTANCES "Instances count should be a number"
    is_ge $INSTANCES 1 "Instances count should be greater than 0"

    if [[ ! -z "${BACKUPS// }" ]]; then
        is_number $BACKUPS "Backups count should be a number"
        is_ge $BACKUPS 0 "Backups count should be greater or equals to 0"
    fi

    if [[ -z "${GRID_GROUP// }" ]]; then
        err "ERROR: Please specify grid group"
        exit 1
    fi

    if [[ -z "${INSTANCE_HEAP// }" ]]; then
        err "ERROR: Please specify instance heap size"
        exit 1
    fi
}

is_number() {
    local value=$1
    local error_message=$2
    local re='^[0-9]+$'
    if ! [[ $value =~ $re ]] ; then
        err $error_message
        exit 1
    fi
}

is_ge() {
    local value1=$1
    local value2=$2
    local error_message=$3
    if ! [[ $value1 -ge $value2 ]] ; then
        err $error_message
        exit 1
    fi
}

err() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')]: $@" >&2
}

print_arguments() {
    echo "Arguments:"
    echo "- Space name: $SPACE_NAME"
    echo "- Cluster schema: $CLUSTER_SCHEMA"
    echo "- Instances count: $INSTANCES"
    echo "- Backups count: $BACKUPS"
    echo "- Group: $GRID_GROUP"
    echo "- Locator: $GRID_LOCATOR"
    echo "- Instance heap size: $INSTANCE_HEAP"
    echo "- Add. classpath: $ADDITIONAL_CLASSPATH"
    echo "- Add. args: $ADDITIONAL_ARGUMENTS"
    echo ""
}

start_instances() {
    declare -A url_args_map

    local readonly time=$(date +"%Y-%m-%d~%H.%M")
    local readonly log_file_head="$time-insightedge_${SPACE_NAME}"

    url_args_map["schema"]="default"
    url_args_map["groups"]=$GRID_GROUP
    url_args_map["locators"]=$GRID_LOCATOR
    url_args_map["cluster_schema"]=$CLUSTER_SCHEMA
    export GSINSTANCE_JAVA_OPTIONS="$GSINSTANCE_JAVA_OPTIONS -Xmx${INSTANCE_HEAP}"

    case $CLUSTER_SCHEMA in
        partitioned-sync2backup)

            url_args_map["total_members"]="$INSTANCES,$BACKUPS"
            local instance_id=1
            while [ $instance_id -le $INSTANCES ]; do
                url_args_map["id"]=$instance_id
                log_file_name="${log_file_head}_id=${instance_id}.log"
                instance_url=$(buildUrl "$(declare -p url_args_map)" $SPACE_NAME)
                start_instance "$instance_url" "$ADDITIONAL_CLASSPATH" "$ADDITIONAL_ARGUMENTS" "$log_file_name"

                local backup_id=1
                while [ $backup_id -le $BACKUPS ]; do
                    url_args_map["backup_id"]=$backup_id
                    log_file_name="${log_file_head}_id=${instance_id}_backup_id=${backup_id}.log"
                    backup_url=$(buildUrl "$(declare -p url_args_map)" $SPACE_NAME)
                    start_instance "$backup_url" "$ADDITIONAL_CLASSPATH" "$ADDITIONAL_ARGUMENTS" "$log_file_name"
                    backup_id=$[$backup_id+1]
                done
                unset url_args_map["backup_id"]

                instance_id=$[$instance_id+1]
            done
            ;;
        async_replicated | sync_replicated)

            url_args_map["total_members"]="$INSTANCES"
            local instance_id=1
            while [ $instance_id -le $INSTANCES ]; do
                url_args_map["id"]=$instance_id
                log_file_name="${log_file_head}_id=${instance_id}.log"
                instance_url=$(buildUrl "$(declare -p url_args_map)" $SPACE_NAME)
                start_instance "$instance_url" "$ADDITIONAL_CLASSPATH" "$ADDITIONAL_ARGUMENTS" "$log_file_name"
                instance_id=$[$instance_id+1]
            done
            ;;
        *)
            err "Wrong cluster schema $CLUSTER_SCHEMA. Available values: partitioned-sync2backup, async_replicated, sync_replicated"
            exit 1
    esac

}

buildUrl() {
    eval "declare -A url_map="${1#*=}
    local space_name=$2
    local url_head="/./$space_name?"
    local url="$url_head"
    for key in "${!url_map[@]}"
    do
        if [[ ! -z "${url_map[$key]}" ]]; then
            url+="$key=${url_map[$key]}&"
        fi
    done
    #echo "Building url: Done -> $url"
    echo "$url"
}

start_instance() {
    local url=$1
    local classpath=$2
    local arguments=$3
    local log_file=$4
    echo "$log_file"

    local logs="${INSIGHTEDGE_HOME}/datagrid/logs/${log_file}"
    echo "Deploing space partition. Command:"
    echo "${INSIGHTEDGE_HOME}/datagrid/bin/gsInstance.sh \"$url\" \"$classpath\" \"$arguments\" 2>&1 > ${logs} &"
    ${INSIGHTEDGE_HOME}/datagrid/bin/gsInstance.sh "$url" "$classpath" "$arguments" 2>&1 > ${logs} &
}


display_usage() {
    sleep 3
    echo ""
    echo "Usage:"
    echo " -n, --name      |    name of the space                                          | default insightedge-space"
    echo " -t, --topology  |    Data grid topology. Available options:                     | default partitioned-sync2backup"
    echo "                               - partitioned-sync2backup"
    echo "                               - async_replicated"
    echo "                               - sync_replicated"
    echo " -i, --instances |    Number of space instances                                  | default 2"
    echo " -b, --backups   |    Number of space backups                                    | default 1"
    echo " -l, --locator   |    lookup locators for the grid components"
    echo " -g, --group     |    lookup groups for the grid components                      | default insightedge"
    echo " -s, --size      |    instance heap size                                         | default 512M"
    echo "                 |             format:  java-style heap size string"
    echo "                 |             example: '1G', '4096M'"
    echo " -c, --classpath |    a user defined path which will be appended to the beginning of the used classpath"
    echo " -a, --arguments |    Any additional command line arguments such as system properties"
    echo ""
    local script="./$THIS_SCRIPT_NAME"
    echo "Examples:"
    echo "  Deploy partitioned space with two partitions and one backup per partition, 512Mb per partition/instance, space name 'insightedge-space' "
    echo ""
    echo " $script"
    echo " or"
    echo " $script -n insightedge-space -t partitioned-sync2backup -i 2 -b 1 -s 512M"
    echo ""
    echo "  Deploy synchronously replicated space with two partitions/instances, 1G per partition/instance, custom locator, additional classpath and command line arguments"
    echo " $script -n sync_space -t sync_replicated -i 2 -l 127.0.0.1:4174 -s 1G -c \"path/to/libs\" -a \"-DmyOwnSysProp=value -DmyOwnSysProp2=value\""
    echo ""
    echo ""
    exit 1
}

main "$@"