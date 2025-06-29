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

import java.util.Map;
import java.util.stream.Stream;

import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieBaseUtil;
import org.drools.testcoverage.common.util.TestConstants;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.KieBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify BRMS-312 (Allow escaping characters in metadata value) is
 * fixed
 */
public class EscapesInMetadataTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EscapesInMetadataTest.class);

    private static final String RULE_NAME = "hello world";
    private static final String RULE_KEY = "output";
    private static final String RULE_VALUE = "Hello world!";

    public static Stream<KieBaseTestConfiguration> parameters() {
        return TestParametersUtil2.getKieBaseConfigurations().stream();
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testMetadataEscapes(KieBaseTestConfiguration kieBaseTestConfiguration) {
        final String rule = "package " + TestConstants.PACKAGE_REGRESSION + "\n"
                + " rule \"" + RULE_NAME + "\"\n"
                + " @" + RULE_KEY + "(\"\\\""+ RULE_VALUE + "\\\"\")\n"
                + " when\n"
                + " then\n"
                + "     System.out.println(\"Hello world!\");\n"
                + " end";

        final KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl(TestConstants.PACKAGE_REGRESSION,
                                                                           kieBaseTestConfiguration, rule);
        final Map<String, Object> metadata = kieBase.getRule(TestConstants.PACKAGE_REGRESSION, RULE_NAME).getMetaData();
        LOGGER.debug(rule);

        assertThat(metadata.containsKey(RULE_KEY)).isTrue();
        assertThat(metadata.get(RULE_KEY)).isEqualTo("\"" + RULE_VALUE + "\"");
    }

}
