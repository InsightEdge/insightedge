#!/bin/bash
main() {
    echo "Lock file:           $1"
    echo "Lock timeout:        $2 seconds"
    echo "Lock check interval: $3 seconds"
    echo "Locker name:         $4"
    echo ""
    LOCK=$1
    TIMEOUT=$2
    RETRY_SLEEP=$3
    NAME=$4

    LOCKED="false"
    while [ "$LOCKED" == "false" ]; do
        if [ -f $LOCK ]; then
            echo "Lock file exists: $LOCK"
            echo "Content: $(cat $LOCK)"

            wait_lock
        else
            echo "Lock file does not exist"
        fi

        echo ""
        echo "Creating lock..."
        EXPECTED_MESSAGE="$(date +%s):$NAME"
        echo $EXPECTED_MESSAGE > $LOCK
        sleep 1

        ACTUAL_MESSAGE="$(cat $LOCK)"
        if [ "$EXPECTED_MESSAGE" == "$ACTUAL_MESSAGE" ]; then
            echo "Successfully locked the file!"
            LOCKED="true"
        else
            echo "Someone locked the file, retrying..."
        fi
    done
}

wait_lock() {
    WAITING="true"
    while [ "$WAITING" == true ]; do
        if [ -f $LOCK ]; then
            LOCK_TIMESTAMP="$(awk -F':' '{ print $1 }' $LOCK)"
            CURRENT_TIME="$(date +%s)"
            if ((LOCK_TIMESTAMP + TIMEOUT > CURRENT_TIME)); then
                EXPECTED_TIME_TO_WAIT=$((TIMEOUT + LOCK_TIMESTAMP - CURRENT_TIME))
                echo "Waiting lock, expected time: $EXPECTED_TIME_TO_WAIT seconds"
                sleep $RETRY_SLEEP
            else
                echo "Finished waiting for lock: lock timeout reached"
                WAITING="false"
            fi
        else
            echo "Finished waiting for lock: lock file is missing"
            WAITING="false"
        fi
    done
}

main "$@"