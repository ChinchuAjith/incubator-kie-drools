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
package org.drools.core.common;

import org.drools.base.definitions.rule.impl.RuleImpl;
import org.drools.base.rule.EntryPointId;
import org.drools.core.marshalling.MarshallerReaderContext;
import org.drools.core.reteoo.TerminalNode;
import org.drools.util.bitmask.BitMask;

public interface PropagationContextFactory {

    PropagationContext createPropagationContext(long number,
                                                PropagationContext.Type type,
                                                RuleImpl rule,
                                                TerminalNode terminalNode,
                                                InternalFactHandle factHandle,
                                                EntryPointId entryPoint,
                                                BitMask modificationMask,
                                                Class<?> modifiedClass,
                                                MarshallerReaderContext readerContext);

    PropagationContext createPropagationContext(long number,
                                                PropagationContext.Type type,
                                                RuleImpl rule,
                                                TerminalNode terminalNode,
                                                InternalFactHandle factHandle,
                                                EntryPointId entryPoint,
                                                MarshallerReaderContext readerContext);

    PropagationContext createPropagationContext(long number,
                                                PropagationContext.Type type,
                                                RuleImpl rule,
                                                TerminalNode terminalNode,
                                                InternalFactHandle factHandle,
                                                EntryPointId entryPoint);

    PropagationContext createPropagationContext(long number,
                                                PropagationContext.Type type,
                                                RuleImpl rule,
                                                TerminalNode terminalNode,
                                                InternalFactHandle factHandle);

}