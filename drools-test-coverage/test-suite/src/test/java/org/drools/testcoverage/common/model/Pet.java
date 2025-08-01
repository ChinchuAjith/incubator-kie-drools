/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.testcoverage.common.model;

import java.io.Serializable;

public class Pet implements Serializable {

    private static final long serialVersionUID = 366857408049359963L;

    public enum PetType {
        DOG, CAT, PARROT
    }

    private String name;
    private PetType type;
    private int age;
    private Person owner;

    public Pet(final String name) {
        this.name = name;
    }

    public Pet(final String name, final int age) {
        this.name = name;
        this.age = age;
    }

    public Pet(final PetType type) {
        this.type = type;
        age = 0;
    }

    public Pet(final PetType type, final int age) {
        super();
        this.type = type;
        this.age = age;
    }

    public PetType getType() {
        return type;
    }

    public void setType(final PetType type) {
        this.type = type;
    }

    public int getAge() {
        return age;
    }

    public void setAge(final int age) {
        this.age = age;
    }

    public Person getOwner() {
        return owner;
    }

    public void setOwner(final Person owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
