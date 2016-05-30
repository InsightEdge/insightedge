#!/usr/bin/env bash


main() {

     echo "Do nothing for open source version"
#    $IE_PATH/datagrid/bin/puInstance.sh -cluster schema=partitioned-sync2backup total_members=2,1 id=1 $IE_PATH/datagrid/deploy/templates/insightedge-datagrid &
#    $IE_PATH/datagrid/bin/puInstance.sh -cluster schema=partitioned-sync2backup total_members=2,1 id=2 backup_id=1 $IE_PATH/datagrid/deploy/templates/insightedge-datagrid &
#    $IE_PATH/datagrid/bin/puInstance.sh -cluster schema=partitioned-sync2backup total_members=2,1 id=2 $IE_PATH/datagrid/deploy/templates/insightedge-datagrid &
#    $IE_PATH/datagrid/bin/puInstance.sh -cluster schema=partitioned-sync2backup total_members=2,1 id=1 backup_id=1 $IE_PATH/datagrid/deploy/templates/insightedge-datagrid &
}

main "$@"