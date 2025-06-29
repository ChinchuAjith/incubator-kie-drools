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
package org.drools.drl.ast.descr;

import java.util.ArrayList;
import java.util.List;

public class FactTemplateDescr extends BaseDescr {
    private static final long serialVersionUID = 510l;

    String                    name;
    List<FieldTemplateDescr>  fields           = new ArrayList<>( 1 );

    public FactTemplateDescr(final String name) {
        this.name = name;
    }

    public FactTemplateDescr() {
    }

    public void addFieldTemplate(final FieldTemplateDescr fieldTemplate) {
        this.fields.add( fieldTemplate );
    }

    public List<FieldTemplateDescr> getFields() {
        return this.fields;
    }

    public String getName() {
        return this.name;
    }

}
