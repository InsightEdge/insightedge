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

package org.insightedge.spark.japi;

import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.metadata.index.SpaceIndexType;
import org.insightedge.spark.model.BucketedGridModel;

import java.io.Serializable;

/**
 * A port of {@link BucketedGridModel} for Java API.
 *
 * @author Oleksiy_Dyagilev
 */
public class JBucketedGridModel implements BucketedGridModel, Serializable {

    private Integer metaBucketId;

    public Integer metaBucketId() {
        return metaBucketId;
    }

    @SpaceIndex(type = SpaceIndexType.EXTENDED)
    public Integer getMetaBucketId() {
        return metaBucketId;
    }

    public void setMetaBucketId(Integer metaBucketId) {
        this.metaBucketId = metaBucketId;
    }

    public void metaBucketId_$eq(Integer metaBucketId) {
        this.metaBucketId = metaBucketId;
    }

}
