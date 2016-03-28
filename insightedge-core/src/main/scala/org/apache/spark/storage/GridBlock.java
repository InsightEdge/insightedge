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
