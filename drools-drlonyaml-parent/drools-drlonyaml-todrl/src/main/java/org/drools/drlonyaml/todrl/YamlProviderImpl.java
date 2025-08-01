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
package org.drools.drlonyaml.todrl;

import org.drools.drl.extensions.YamlProvider;
import org.drools.util.IoUtils;
import org.kie.api.io.Resource;

import static org.drools.drlonyaml.todrl.YAMLtoDrlDumper.yaml2drl;

public class YamlProviderImpl implements YamlProvider {

    @Override
    public String loadFromResource(Resource resource) {
        try {
            String content = new String(IoUtils.readBytesFromInputStream(resource.getInputStream()));
            return yaml2drl(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
