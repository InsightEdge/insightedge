#!/usr/bin/env bash

STANDALONE_CONTAINER_CLASS="org.openspaces.pu.container.standalone.StandaloneProcessingUnitContainer"

# Stops the Gigaspaces Datagrid slave on the machine this script is executed on.

main() {
    # TODO need space name?
    # SPACE_NAME=$1
    echo "Trying to stop space instances"
#    pids=`ps aux | grep -v grep | grep $STANDALONE_CONTAINER_CLASS | awk '{print $2}'`
    pids=`ps aux | grep -v grep | grep $insightedge.marker=slave | awk '{print $2}'`
    if [ ${#pids[@]} -eq 0 ]; then #TODO does not work if there are not processes
        echo "Datagrid instances are not running"
        exit
    fi
    echo "Stopping datagrid instances (pids: $pids)..."

    kill -SIGTERM $pids

    TIMEOUT=60
    while ps -p $pids > /dev/null; do
        if [ $TIMEOUT -le 0 ]; then
            echo "Some instances are still running"
            break
        fi
        echo "  waiting termination ($TIMEOUT sec)"
        ((TIMEOUT--))
        sleep 1
    done
    echo "Datagrid instances stopped"
}

main "$@"