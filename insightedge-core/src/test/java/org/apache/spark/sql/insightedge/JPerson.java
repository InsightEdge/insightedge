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

package org.apache.spark.sql.insightedge;

import com.gigaspaces.annotation.pojo.SpaceId;

/**
 * @author Oleksiy_Dyagilev
 */
public class JPerson {

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

