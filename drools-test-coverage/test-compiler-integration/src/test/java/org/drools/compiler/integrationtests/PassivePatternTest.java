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
package org.drools.compiler.integrationtests;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieBaseUtil;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class PassivePatternTest {

    public static Stream<KieBaseTestConfiguration> parameters() {
        return TestParametersUtil2.getKieBaseCloudConfigurations(true).stream();
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
    public void testPassiveInsert(KieBaseTestConfiguration kieBaseTestConfiguration) {
        final String drl =
                "global java.util.List list\n" +
                        "rule R when\n" +
                        "    $i : Integer()\n" +
                        "    ?String( this == $i.toString() )\n" +
                        "then\n" +
                        "    list.add( $i );\n" +
                        "end\n";

        final KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("pasive-pattern-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession();
        try {
            final List<Integer> list = new ArrayList<>();
            ksession.setGlobal("list", list);

            ksession.insert(1);
            ksession.insert("2");
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            ksession.insert("1");
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            ksession.insert(2);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);
            assertThat(list.containsAll(asList(1, 2))).isTrue();
        } finally {
            ksession.dispose();
        }
    }
}
