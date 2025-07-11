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
package org.drools.testcoverage.regression;

import java.util.stream.Stream;

import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieUtil;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.definition.type.Position;

/**
 * Tests merging the POJO annotations (e.g. @Position) with fact declaration in
 * DRL.
 */
public class POJOAnnotationMergeTest {

    private static final String EVENT_CLASS_NAME = PositionAnnotatedEvent.class.getCanonicalName();

    public static Stream<KieBaseTestConfiguration> parameters() {
        return TestParametersUtil2.getKieBaseConfigurations().stream();
    }

    // should add metadata to metadata already defined in POJO
    private static final String DRL =
            "package org.test\n" +
            "declare " + EVENT_CLASS_NAME + "\n" +
            "    @role(event)\n" +
            "end \n" +
            "rule 'sample rule' \n" +
            "when \n" +
            "  " + EVENT_CLASS_NAME + "( 'value1', 'value2'; )\n" +
            "then\n" +
            "end\n";

    /**
     * Tests adding metadata in DRL to the metadata already declared in a POJO.
     */
    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testPositionFromPOJOIgnored(KieBaseTestConfiguration kieBaseTestConfiguration) {
        KieUtil.getKieBuilderFromDrls(kieBaseTestConfiguration,true, DRL);
    }

    /**
     * Sample event annotated with @Position metadata.
     */
    public static class PositionAnnotatedEvent {
        @Position(1)
        private String arg1;

        @Position(0)
        private String arg0;

        public String getArg1() {
            return arg1;
        }

        public void setArg1(final String arg1) {
            this.arg1 = arg1;
        }

        public String getArg0() {
            return arg0;
        }

        public void setArg0(final String arg0) {
            this.arg0 = arg0;
        }

    }
}
