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
package org.kie.api.definition.process;

import java.util.List;
import java.util.Map;

/**
 * A Node represents an activity in the process flow chart.
 * Many different predefined nodes are supported out-of-the-box.
 */
public interface Node extends WorkflowElement  {

    /**
     * The name of the node
     * @return the name of the node
     */
    String getName();

    /**
     * The incoming connections for this Node.
     * A Node could have multiple entry-points.
     * This map contains the list of incoming connections for each entry-point.
     *
     * @return the incoming connections
     */
    Map<String, List<Connection>> getIncomingConnections();

    /**
     * The outgoing connections for this Node.
     * A Node could have multiple exit-points.
     * This map contains the list of outgoing connections for each exit-point.
     *
     * @return the outgoing connections
     */
    Map<String, List<Connection>> getOutgoingConnections();

    /**
     * The incoming connections for this Node for the given entry-point.
     *
     * @return the incoming connections for the given entry point
     */
    List<Connection> getIncomingConnections(String type);

    /**
     * The outgoing connections for this Node for the given exit-point.
     *
     * @return the outgoing connections for the given exit point
     */
    List<Connection> getOutgoingConnections(String type);

    /**
     * The NodeContainer this Node lives in.
     *
     * @return the NodeContainer
     */
    NodeContainer getNodeContainer();

    /**
     * Meta data associated with this Node.
     */
    Map<String, Object> getMetaData();

    /**
     * Type of this node
     * @return one of the possible type of nodes
     */
    default NodeType getNodeType() {
        return NodeType.INTERNAL;
    }
}
