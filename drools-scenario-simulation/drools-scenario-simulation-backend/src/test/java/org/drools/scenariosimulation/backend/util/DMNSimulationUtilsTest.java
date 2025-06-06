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
package org.drools.scenariosimulation.backend.util;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.io.Resource;
import org.kie.dmn.api.core.DMNModel;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DMNSimulationUtilsTest {

    @Test
    void findDMNModel() {
        List<String> pathToFind = List.of(new StringBuilder("to/find").reverse().toString().split("/"));

        List<DMNModel> models = List.of(createDMNModelMock("this/should/not/match"), createDMNModelMock("find"), 
        		createDMNModelMock("something/to/find"));

        DMNSimulationUtils.findDMNModel(models, pathToFind, 1);

        List<String> impossibleToFind = List.of(new StringBuilder("not/find").reverse().toString().split("/"));

        assertThatThrownBy(() -> DMNSimulationUtils.findDMNModel(models, impossibleToFind, 1))
                .isInstanceOf(ImpossibleToFindDMNException.class)
                .hasMessage("Retrieving the DMNModel has failed. Make sure the used DMN asset does not " +
                                    "produce any compilation errors and that the project does not " +
                                    "contain multiple DMN assets with the same name and namespace. " +
                                    "In addition, check if the reference to the DMN file is correct " +
                                    "in the Settings panel. " +
                                    "After addressing the issues, build the project again.");
    }

    private DMNModel createDMNModelMock(String path) {
        DMNModel modelMock = mock(DMNModel.class);
        Resource resourceMock = mock(Resource.class);
        when(resourceMock.getSourcePath()).thenReturn(path);
        when(modelMock.getResource()).thenReturn(resourceMock);
        return modelMock;
    }
}