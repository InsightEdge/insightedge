#!/bin/bash
main() {
    echo "Lock file:           $1"
    echo "Locker name:         $2"
    echo ""
    LOCK=$1
    NAME=$2

    if [ -f $LOCK ]; then
        COUNT="$(grep -o "$2" $1 | wc -l)"
        if [ "$COUNT" == 0 ]; then
            echo "Lock file belongs to somebody else, ignoring"
            echo "Content: $(cat $LOCK)"
        else
            echo "Lock file belongs to this process, deleting"
            rm $LOCK
        fi
    else
        echo "Lock file is missing"
    fi
}

main "$@"