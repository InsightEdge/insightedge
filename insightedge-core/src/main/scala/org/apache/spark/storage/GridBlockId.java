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
