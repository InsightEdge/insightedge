package com.gigaspaces.spark.rdd;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.gigaspaces.spark.japi.JGridModel;
import com.gigaspaces.spark.model.GridModel;

import java.io.Serializable;

/**
 * Space class for the tests that work with Java POJOs. Mirror of Data.scala
 *
 * @author Oleksiy_Dyagilev
 */
@SpaceClass
public class JData extends JGridModel {

    private String id;
    private Long routing;
    private String data;
    private Boolean flag;

    public JData() {
    }

    public JData(Long routing, String data) {
        this.routing = routing;
        this.data = data;
    }

    @SpaceId(autoGenerate = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @SpaceRouting
    public Long getRouting() {
        return routing;
    }

    public void setRouting(Long routing) {
        this.routing = routing;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Boolean getFlag() {
        return flag;
    }

    public void setFlag(Boolean flag) {
        this.flag = flag;
    }
}
