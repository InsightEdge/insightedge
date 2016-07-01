package org.apache.spark.sql.insightedge;

import java.io.Serializable;

public class JAddress implements Serializable {
    private String city;
    private String state;

    public JAddress() {
    }

    public JAddress(String city, String state) {
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
