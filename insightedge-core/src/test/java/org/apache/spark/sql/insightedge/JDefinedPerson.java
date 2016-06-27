package org.apache.spark.sql.insightedge;

import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.spark.japi.JGridModel;
import org.apache.spark.sql.types.SQLUserDefinedType;

import java.io.Serializable;

public class JDefinedPerson extends JGridModel {

    private String id;
    private String name;
    private Integer age;
    private JDefinedAddress address;

    public JDefinedPerson() {
    }

    public JDefinedPerson(String id, String name, Integer age, JDefinedAddress address) {
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

    public JDefinedAddress getAddress() {
        return address;
    }

    public void setAddress(JDefinedAddress address) {
        this.address = address;
    }
}

@SQLUserDefinedType(udt = JDefinedAddressUDT.class)
class JDefinedAddress implements Serializable {
    private String city;
    private String state;

    public JDefinedAddress() {
    }

    public JDefinedAddress(String city, String state) {
        this.city = city;
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}

