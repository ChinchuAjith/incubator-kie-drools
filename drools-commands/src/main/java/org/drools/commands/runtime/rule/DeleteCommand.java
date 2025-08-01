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
package org.drools.commands.runtime.rule;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.drools.core.common.DisconnectedFactHandle;
import org.kie.api.command.ExecutableCommand;
import org.kie.api.runtime.Context;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.command.RegistryContext;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class DeleteCommand
        implements ExecutableCommand<Void> {

    @XmlElement(name="handle")
    private DisconnectedFactHandle handle;

    private String factHandle;

    @XmlElement
    private FactHandle.State fhState = FactHandle.State.ALL;

    public DeleteCommand() {
    }

    public DeleteCommand(FactHandle handle) {
        this.handle = DisconnectedFactHandle.newFrom( handle );
    }

    public DeleteCommand(FactHandle handle, FactHandle.State fhState) {
        this(handle);
        this.fhState = fhState;
    }

    public FactHandle getFactHandle() {
        return this.handle;
    }

    public void setHandle(DisconnectedFactHandle factHandle) {
        this.handle = factHandle;
    }

    @XmlElement(name="fact-handle", required=true)
    public void setFactHandleFromString(String factHandleId) {
        handle = new DisconnectedFactHandle(factHandleId);
    }

    public String getFactHandleFromString() {
        return handle.toExternalForm();
    }

    public Void execute(Context context) {
        KieSession ksession = ((RegistryContext) context).lookup( KieSession.class );
        ksession.getEntryPoint( handle.getEntryPointName() ).delete( handle, fhState );
        return null;
    }

    public String toString() {
        return "session.retract( " + handle + " );";
    }

}
