package com.gigaspaces.spark.java.model;

import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.spark.context.GigaSpacesSparkContext;
import com.gigaspaces.spark.model.GridModel;

import java.io.Serializable;

/**
 * A port of {@link GridModel} for Java API.
 *
 * @author Oleksiy_Dyagilev
 */
public class JGridModel implements GridModel, Serializable {

    private Integer metaBucketId;

    public Integer metaBucketId() {
        GigaSpacesSparkContext sc = new GigaSpacesSparkContext(null);

        return metaBucketId;
    }

    @SpaceIndex
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
