package org.apache.spark.storage;

import java.io.Serializable;

/**
 * @author Oleksiy_Dyagilev
 */
public class GridBlockId implements Serializable {

    private String name;
    private String appExecutorId;

    public GridBlockId() {
    }

    public GridBlockId(String name, String appExecutorId) {
        this.name = name;
        this.appExecutorId = appExecutorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAppExecutorId() {
        return appExecutorId;
    }

    public void setAppExecutorId(String appExecutorId) {
        this.appExecutorId = appExecutorId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GridBlockId that = (GridBlockId) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return appExecutorId != null ? appExecutorId.equals(that.appExecutorId) : that.appExecutorId == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (appExecutorId != null ? appExecutorId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GridBlockId{" +
                "name='" + name + '\'' +
                ", appExecutorId='" + appExecutorId + '\'' +
                '}';
    }
}
