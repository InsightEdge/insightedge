#!/usr/bin/env bash

# Stops the Datagrid master on the machine this script is executed on.

main() {
    pid=`ps aux | grep -v grep | grep insightedge.marker=master | awk '{print $2}'`
    if [ -z $pid ]; then
        echo "Datagrid master is not running"
        exit
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

main "$@"