package org.insightedge.spark.jobs;

import com.gigaspaces.annotation.pojo.SpaceId;
import java.io.Serializable;

public class Person implements Serializable {
    private String id;
    private String name;
    private Country country;

    public Person() {}

    public Person(String id, String name, Country country) {
        this.id = id;
        this.name = name;
        this.country = country;
    }

    @SpaceId
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

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }
}