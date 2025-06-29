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

import java.util.Date;
import java.util.stream.Stream;

import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieUtil;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
* Tests compilation of facts extending java.util.Date (BZ 1072629).
*/
public class DateExtendingFactTest {

    private static final String FACT_CLASS_NAME = MyDate.class.getCanonicalName();

    private static final String DRL =
            "package org.test\n" +
            "rule 'sample rule'\n" +
            "when\n" +
            "  $date:" + FACT_CLASS_NAME + "()\n" +
            "then\n" +
            "$date.setDescription(\"test\");\n" +
            "end\n";

    public static Stream<KieBaseTestConfiguration> parameters() {
        return TestParametersUtil2.getKieBaseConfigurations().stream();
    }

    /**
     * Tests compiling DRL with a fact extending java.util.Date.
     */
    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testDateExtendingFact(KieBaseTestConfiguration kieBaseTestConfiguration) {
        final KieBuilder kbuilder = KieUtil.getKieBuilderFromDrls(kieBaseTestConfiguration, true, DRL);
        assertThat(kbuilder.getResults().getMessages(Message.Level.ERROR)).isEmpty();
    }


    /**
     * Sample fact extending java.util.Date.
     */
    public static class MyDate extends Date {

        private String description;

        public MyDate() {
            super();
        }

        public String getDescription() {
            return this.description;
        }

        public void setDescription(final String desc) {
            this.description = desc;
        }
    }
}
