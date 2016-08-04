#!/usr/bin/env bash

# Stops the space instances on the machine this script is executed on.

main() {
    echo "Trying to stop space instances"
    pids=`ps aux | grep -v grep | grep insightedge.marker=slave | awk '{print $2}'`
    if [[ -z $pids ]]; then
        echo "Datagrid instances are not running"
        exit
    fi
    if [[ ${#pids[@]} -eq 0 ]]; then
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