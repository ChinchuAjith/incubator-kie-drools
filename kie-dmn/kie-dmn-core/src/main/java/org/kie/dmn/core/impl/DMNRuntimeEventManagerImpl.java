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

import java.util.HashSet;
import java.util.Set;

import org.kie.dmn.api.core.DMNRuntime;
import org.kie.dmn.api.core.event.DMNRuntimeEventListener;
import org.kie.dmn.api.core.event.DMNRuntimeEventManager;

public class DMNRuntimeEventManagerImpl implements DMNRuntimeEventManager {

    private Set<DMNRuntimeEventListener> listeners = new HashSet<>();

    private DMNRuntime dmnRuntime;

    public DMNRuntimeEventManagerImpl() {

    }

    public DMNRuntimeEventManagerImpl(DMNRuntime dmnRuntime) {
        this.dmnRuntime = dmnRuntime;
    }

    @Override
    public void addListener(DMNRuntimeEventListener listener) {
        if( listener != null ) {
            this.listeners.add( listener );
        }
    }

    @Override
    public void removeListener(DMNRuntimeEventListener listener) {
        this.listeners.remove( listener );
    }

    @Override
    public Set<DMNRuntimeEventListener> getListeners() {
        return listeners;
    }

    @Override
    public boolean hasListeners() {
        return !listeners.isEmpty();
    }

    @Override
    public DMNRuntime getRuntime() {
        return dmnRuntime;
    }

}
