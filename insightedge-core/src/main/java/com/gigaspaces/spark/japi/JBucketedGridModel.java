package com.gigaspaces.spark.japi;

import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.metadata.index.SpaceIndexType;
import com.gigaspaces.spark.model.BucketedGridModel;

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
