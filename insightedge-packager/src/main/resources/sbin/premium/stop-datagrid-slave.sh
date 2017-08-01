#!/usr/bin/env bash

# Stops the Gigaspaces Datagrid slave on the machine this script is executed on.

stop_datagrid_slave() {
    pid=`ps aux | grep -v grep | grep insightedge.marker=slave | awk '{print $2}'`
    if [ -z $pid ]; then
        echo "Datagrid slave is not running"
        exit
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

stop_datagrid_slave "$@"