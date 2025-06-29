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
package org.drools.kiesession.debug;

import org.drools.base.common.NetworkNode;
import org.drools.core.reteoo.BetaMemory;
import org.drools.core.reteoo.BetaNode;

import java.util.Collection;

public class BetaNodeVisitor extends AbstractNetworkNodeVisitor {
    
    public static final BetaNodeVisitor INSTANCE = new BetaNodeVisitor();
    
    protected BetaNodeVisitor() {
    }

    @Override
    protected void doVisit(NetworkNode node,
                           Collection<NetworkNode> nodeStack,
                           StatefulKnowledgeSessionInfo info) {
        BetaNode bn = (BetaNode) node;
        DefaultNodeInfo ni = info.getNodeInfo(node);
        final BetaMemory memory = (BetaMemory) info.getSession().getNodeMemory( bn );
        
        if( bn.isObjectMemoryEnabled() ) {
            ni.setFactMemorySize( memory.getRightTupleMemory().size() );
        }
        if( bn.isLeftTupleMemoryEnabled() ) {
            ni.setTupleMemorySize( memory.getLeftTupleMemory().size() );
        }

    }

}
