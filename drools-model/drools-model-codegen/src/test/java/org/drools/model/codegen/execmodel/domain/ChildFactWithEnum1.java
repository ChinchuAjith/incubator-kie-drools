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
package org.drools.model.codegen.execmodel.domain;

public class ChildFactWithEnum1 {

    private final int id;
    private final int parentId;
    private final EnumFact1 enumValue;

    public ChildFactWithEnum1(final int id, final int parentId, final EnumFact1 enumValue) {
        this.id = id;
        this.parentId = parentId;
        this.enumValue = enumValue;
    }

    public int getId() {
        return id;
    }

    public int getParentId() {
        return parentId;
    }

    public EnumFact1 getEnumValue() {
        return enumValue;
    }

    public String getEnumValueFromInterface() {
        switch (enumValue) {
            case FIRST: return InterfaceAsEnum.FIRST;
            case SECOND: return InterfaceAsEnum.SECOND;
            case THIRD: return InterfaceAsEnum.THIRD;
            case FOURTH: return InterfaceAsEnum.FOURTH;
        }
        throw new RuntimeException( "UNKNOWN" );
    }
}
