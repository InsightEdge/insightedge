package org.apache.spark.sql.insightedge;

import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.spark.japi.JGridModel;

import java.io.Serializable;

/**
 * @author Oleksiy_Dyagilev
 */
public class JPerson extends JGridModel {

    private String id;
    private String name;
    private Integer age;
    private JAddress address;

    public JPerson() {
    }

    public JPerson(String id, String name, Integer age, JAddress address) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.address = address;
    }

    @SpaceId(autoGenerate = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public JAddress getAddress() {
        return address;
    }

    public void setAddress(JAddress address) {
        this.address = address;
    }
}

