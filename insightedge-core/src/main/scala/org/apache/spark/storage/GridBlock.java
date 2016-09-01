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

package org.apache.spark.storage;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceStorageType;
import com.gigaspaces.metadata.StorageType;

/**
 * @author Oleksiy_Dyagilev
 */
@SpaceClass
public class GridBlock {

    private GridBlockId id;
    private byte[] bytes;
    private Integer size;

    public GridBlock() {
    }

    public GridBlock(GridBlockId id, byte[] bytes, Integer size) {
        this.id = id;
        this.bytes = bytes;
        this.size = size;
    }

    @SpaceId
    public GridBlockId getId() {
        return id;
    }

    public void setId(GridBlockId id) {
        this.id = id;
    }

    @SpaceStorageType(storageType = StorageType.BINARY)
    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
