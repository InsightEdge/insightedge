package org.insightedge.spark.utils;

import com.gigaspaces.async.AsyncResult;

import org.jini.rio.boot.BootUtil;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.TaskGigaSpace;

import java.util.ArrayList;
import java.util.List;

/**
 * Returns a list of [hostName, containerName, partitionId]
 *
 * @author Oleksiy_Dyagilev
 */
public class LookupPartitionTask implements DistributedTask<ArrayList<String>, List<List<String>>> {

    @TaskGigaSpace
    private transient GigaSpace gridProxy;

    public ArrayList<String> execute() throws Exception {
        String hostName = BootUtil.getHostAddress();
        String containerName = gridProxy.getSpace().getContainerName();

        ArrayList<String> res = new ArrayList<String>();
        res.add(hostName);
        res.add(containerName);
        return res;
    }


    public List<List<String>> reduce(List<AsyncResult<ArrayList<String>>> mapAsyncResults) throws Exception {
        List<List<String>> reduceResult = new ArrayList<List<String>>();

        int id = 0;
        for (AsyncResult<ArrayList<String>> mapAsyncResult : mapAsyncResults) {
            if (mapAsyncResult.getException() != null) {
                throw mapAsyncResult.getException();
            }

            ArrayList<String> mapResult = mapAsyncResult.getResult();
            mapResult.add(String.valueOf(id));
            id++;
            reduceResult.add(mapResult);
        }

        return reduceResult;
    }
}
