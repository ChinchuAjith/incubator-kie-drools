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
package org.kie.dmn.core.impl;

import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.event.BeforeEvaluateAllEvent;

public class BeforeEvaluateAllEventImpl implements BeforeEvaluateAllEvent {

    private String modelNamespace;
    private String modelName;
    private DMNResult result;

    public BeforeEvaluateAllEventImpl(String modelNamespace, String modelName, DMNResult result) {
        this.modelNamespace = modelNamespace;
        this.modelName = modelName;
        this.result = result;
    }

    @Override
    public String getModelNamespace() {
        return modelNamespace;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public DMNResult getResult() {
        return result;
    }
    
    @Override
    public String toString() {
        return "BeforeEvaluateAllEventImpl{ modelNamespace=" + modelNamespace + ", modelName=" + modelName + "}";
    }

}
