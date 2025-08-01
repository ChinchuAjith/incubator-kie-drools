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
package org.drools.core.marshalling;

import java.util.HashMap;
import java.util.Map;

import org.drools.util.ClassUtils;
import org.kie.api.marshalling.ObjectMarshallingStrategyAcceptor;

public class ClassObjectMarshallingStrategyAcceptor implements ObjectMarshallingStrategyAcceptor {
    public static final ClassObjectMarshallingStrategyAcceptor DEFAULT = new ClassObjectMarshallingStrategyAcceptor(new String[] { "*.*" } );
    
    private final Map<String, Object> patterns;
    
    public ClassObjectMarshallingStrategyAcceptor(String[] patterns) {
        this.patterns = new HashMap<>();
        for (String pattern : patterns ) {
            addPattern( pattern );
        }
    }
    
    public ClassObjectMarshallingStrategyAcceptor() {
        this.patterns = new HashMap<>();
    }
    
    private void addPattern(String pattern) {
        
        ClassUtils.addImportStylePatterns( this.patterns, pattern );
    }

    public boolean accept(Object object) {
        return ClassUtils.isMatched( this.patterns, object.getClass().getName() );
    }

    @Override
    public String toString() {
        return "ClassObjectMarshallingStrategyAcceptor for " + patterns.keySet();
    }
}
