/*
 * Copyright (c) 2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.insightedge.spark.utils;

import com.gigaspaces.async.AsyncResult;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.TaskGigaSpace;

import java.net.InetAddress;
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
        String hostName = InetAddress.getLocalHost().getHostAddress();
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
