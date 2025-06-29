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
package org.drools.testcoverage.functional;

import java.util.stream.Stream;

import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieUtil;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.KieServices;
import org.kie.api.io.Resource;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.fail;

/**
 * Testing of duplicities in rule files.
 * https://bugzilla.redhat.com/show_bug.cgi?id=724753
 */
public class DuplicityTest {

    public static Stream<KieBaseTestConfiguration> parameters() {
        return TestParametersUtil2.getKieBaseConfigurations().stream();
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testTwoRulesWithSameNameInOneFile(KieBaseTestConfiguration kieBaseTestConfiguration) {
        try {
            final Resource resource =
                    KieServices.Factory.get().getResources().newClassPathResource("rule-name.drl", getClass());
            KieUtil.getKieBuilderFromResources(kieBaseTestConfiguration, true, resource);
            fail("Builder should have had errors, two rules of the same name are not allowed in one file together!");
        } catch (AssertionError e) {
            // expected
            LoggerFactory.getLogger(getClass()).info("", e);
        }
    }
}
