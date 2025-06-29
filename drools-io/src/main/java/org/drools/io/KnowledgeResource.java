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
package org.drools.io;

import org.kie.api.io.ResourceConfiguration;
import org.kie.api.io.ResourceType;

public class KnowledgeResource {
    private String                source;
    private ResourceType         type;
    private ResourceConfiguration configuration;

    public KnowledgeResource(String src,
                             ResourceType type) {
        this.source = src;
        this.type = type;
    }

    public KnowledgeResource(String src,
                             ResourceType type,
                             ResourceConfiguration configuration) {
        this.source = src;
        this.type = type;
        this.configuration = configuration;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String src) {
        this.source = src;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public ResourceConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ResourceConfiguration configuration) {
        this.configuration = configuration;
    }

}
