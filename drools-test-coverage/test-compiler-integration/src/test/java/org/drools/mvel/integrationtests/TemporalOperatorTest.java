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
package org.drools.mvel.integrationtests;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieBaseUtil;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import static org.assertj.core.api.Assertions.assertThat;

public class TemporalOperatorTest {

    public static Stream<KieBaseTestConfiguration> parameters() {
        return TestParametersUtil2.getKieBaseCloudConfigurations(true).stream();
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testAfterWithLocalDateTime(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkTemporalConstraint(kieBaseTestConfiguration, "localDateTime after $t1.localDateTime" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testAfterWithZonedDateTime(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkTemporalConstraint(kieBaseTestConfiguration, "zonedDateTime after $t1.zonedDateTime" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testAfterWithDate(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkTemporalConstraint(kieBaseTestConfiguration, "date after $t1.date" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testAfterWithDateUsingOr(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkTemporalConstraint(kieBaseTestConfiguration, "date == null || date after $t1.date" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testAfterMixDateAndLocaldateTime(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkTemporalConstraint(kieBaseTestConfiguration, "date after $t1.localDateTime" );
    }

    public void checkTemporalConstraint(KieBaseTestConfiguration kieBaseTestConfiguration, String constraint) {
        String str = "import " + TimestampedObject.class.getCanonicalName() + ";\n" +
                     "global java.util.List list;\n" +
                     "rule R when\n" +
                     "  $t1 : TimestampedObject()\n" +
                     "  $t2 : TimestampedObject( " + constraint + " )\n" +
                     "then\n" +
                     "  list.add($t2.getName());\n" +
                     "end\n";

        KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("test", kieBaseTestConfiguration, str);
        KieSession ksession = kbase.newKieSession();
        try {
            List<String> list = new ArrayList<String>();
            ksession.setGlobal( "list", list );

            TimestampedObject t1 = new TimestampedObject( "t1", LocalDateTime.now() );
            TimestampedObject t2 = new TimestampedObject( "t2", LocalDateTime.now().plusHours( 1 ) );

            ksession.insert( t1 );
            ksession.insert( t2 );
            ksession.fireAllRules();

            assertThat(list.get(0)).isEqualTo(t2.getName());
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testCompareMaxLocalDates(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // DROOLS-6431
        checkMaxDate(kieBaseTestConfiguration, "localDate == LocalDate.MAX" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testCompareTimeAndMaxLocalDates(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // DROOLS-6431
        checkMaxDate(kieBaseTestConfiguration, "localDateTime == LocalDate.MAX" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testCompareLocalDatesAndMaxTime(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // DROOLS-6431
        checkMaxDate(kieBaseTestConfiguration, "localDate == LocalDateTime.MAX" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testCompareMaxLocalDateTimes(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // DROOLS-6431
        checkMaxDate(kieBaseTestConfiguration, "localDateTime == LocalDateTime.MAX" );
    }

    private void checkMaxDate(KieBaseTestConfiguration kieBaseTestConfiguration, String constraint) {
        String str =
                "import " + TimestampedObject.class.getCanonicalName() + ";\n" +
                "import " + LocalDate.class.getCanonicalName() + ";\n" +
                "import " + LocalDateTime.class.getCanonicalName() + ";\n" +
                "rule R when\n" +
                "  $t1 : TimestampedObject(" + constraint + ")\n" +
                "then\n" +
                "end\n";

        KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("test", kieBaseTestConfiguration, str);
        KieSession ksession = kbase.newKieSession();
        try {
            TimestampedObject t1 = new TimestampedObject( "t1", LocalDateTime.now() );

            ksession.insert( t1 );
            assertThat(ksession.fireAllRules()).isEqualTo(0);
        } finally {
            ksession.dispose();
        }
    }

    public static class TimestampedObject {
        private final String name;
        private final LocalDateTime localDateTime;

        public TimestampedObject( String name, LocalDateTime time ) {
            this.name = name;
            this.localDateTime = time;
        }

        public String getName() {
            return name;
        }

        public LocalDateTime getLocalDateTime() {
            return localDateTime;
        }

        public LocalDate getLocalDate() {
            return localDateTime.toLocalDate();
        }

        public ZonedDateTime getZonedDateTime() {
            return localDateTime.atZone( ZoneId.systemDefault() );
        }

        public Date getDate() {
            return Date.from(localDateTime.atZone( ZoneId.systemDefault() ).toInstant() );
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testAfterWithConstant(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkConstantTemporalConstraint(kieBaseTestConfiguration, "date after \"01-Jan-1970\"" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testAfterWithConstantUsingOR(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // RHBRMS-2799
        checkConstantTemporalConstraint(kieBaseTestConfiguration, "date == null || date after \"01-Jan-1970\"" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testAfterWithLocalDateTimeUsingOr(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // RHBRMS-2799
        checkConstantTemporalConstraint(kieBaseTestConfiguration, "localDateTime == null || localDateTime after \"01-Jan-1970\"" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testAfterWithLocalDateTimeWithLiteral(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // RHBRMS-2799
        checkConstantTemporalConstraint(kieBaseTestConfiguration, "localDateTime after \"01-Jan-1970\"" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testDateAfterWithLiteral(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkConstantTemporalConstraint(kieBaseTestConfiguration, "date after \"01-Jan-1970\"" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testAfterWithLocalDateWithLiteral(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkConstantTemporalConstraint(kieBaseTestConfiguration, "localDate after \"01-Jan-1970\"" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testComparisonWithLocalDateTimeAndLiteral(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkConstantTemporalConstraint(kieBaseTestConfiguration, "localDate > \"01-Jan-1970\"" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testComparisonWithLocalDate(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkConstantTemporalConstraint(kieBaseTestConfiguration, "localDate > org.drools.mvel.integrationtests.TemporalOperatorTest.parseDateAsLocal(\"01-Jan-1970\")" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testComparisonWithLocalDateAndLiteral(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkConstantTemporalConstraint(kieBaseTestConfiguration, "localDateTime > \"01-Jan-1970\"" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testComparisonWithLocalDateTime(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkConstantTemporalConstraint(kieBaseTestConfiguration, "localDateTime > org.drools.mvel.integrationtests.TemporalOperatorTest.parseTimeAsLocal(\"01-Jan-1970\")" );
    }

    public void checkConstantTemporalConstraint(KieBaseTestConfiguration kieBaseTestConfiguration, String constraint) {
        String str = "import " + TimestampedObject.class.getCanonicalName() + ";\n" +
                     "global java.util.List list;\n" +
                     "rule R when\n" +
                     "  $t1 : TimestampedObject( " + constraint + " )\n" +
                     "then\n" +
                     "  list.add($t1.getName());\n" +
                     "end\n";

        KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("test", kieBaseTestConfiguration, str);
        KieSession ksession = kbase.newKieSession();
        try {
            List<String> list = new ArrayList<String>();
            ksession.setGlobal( "list", list );

            TimestampedObject t1 = new TimestampedObject( "t1", LocalDateTime.now() );

            ksession.insert( t1 );
            ksession.fireAllRules();

            assertThat(list.get(0)).isEqualTo(t1.getName());
        } finally {
            ksession.dispose();
        }
    }

    public static LocalDate parseDateAsLocal( String droolsDate ) {
        if (droolsDate == null) {
            return null;
        }
        try {
            return (new SimpleDateFormat("dd-MMM-yyyy", Locale.UK).parse(droolsDate))
                    .toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (ParseException e) {
            return null;
        }
    }

    public static LocalDateTime parseTimeAsLocal( String droolsDate ) {
        if (droolsDate == null) {
            return null;
        }
        try {
            return (new SimpleDateFormat("dd-MMM-yyyy", Locale.UK).parse(droolsDate))
                    .toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (ParseException e) {
            return null;
        }
    }
}
