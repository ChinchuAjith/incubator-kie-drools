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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.drools.core.base.UndefinedCalendarExcption;
import org.drools.core.time.impl.PseudoClockScheduler;
import org.drools.testcoverage.common.model.Alarm;
import org.drools.testcoverage.common.model.FactA;
import org.drools.testcoverage.common.model.FactB;
import org.drools.testcoverage.common.model.Pet;
import org.drools.testcoverage.common.model.StockTick;
import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieBaseUtil;
import org.drools.testcoverage.common.util.KieSessionTestConfiguration;
import org.drools.testcoverage.common.util.KieUtil;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.drools.util.DateUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.KieBase;
import org.kie.api.builder.KieBuilder;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.TimedRuleExecutionOption;
import org.kie.api.runtime.rule.EntryPoint;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.time.Calendar;
import org.kie.api.time.SessionClock;
import org.kie.api.time.SessionPseudoClock;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class TimerAndCalendarWithPseudoTimeTest {

    public static Stream<KieBaseTestConfiguration> parameters() {
        return TestParametersUtil2.getKieBaseStreamConfigurations(true).stream();
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testDurationMemoryLeakonRepeatedUpdate(KieBaseTestConfiguration kieBaseTestConfiguration) {
        final String drl =
                "package org.drools.compiler.test\n" +
                           "import " + Alarm.class.getCanonicalName() + "\n" +
                           "global java.util.List list;" +
                           "rule \"COMPTEUR\"\n" +
                           "  timer (int: 50s)\n" +
                           "  when\n" +
                           "    $alarm : Alarm( number < 5 )\n" +
                           "  then\n" +
                           "    $alarm.incrementNumber();\n" +
                           "    list.add( $alarm );\n" +
                           "    update($alarm);\n" +
                           "end\n";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime(new Date().getTime(), TimeUnit.MILLISECONDS);

            final List list = new ArrayList();
            ksession.setGlobal("list", list);
            ksession.insert(new Alarm());

            ksession.fireAllRules();

            for (int i = 0; i < 6; i++) {
                timeService.advanceTime(55, TimeUnit.SECONDS);
                ksession.fireAllRules();
            }

            assertThat(list.size()).isEqualTo(5);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testNoProtocolIntervalTimer(KieBaseTestConfiguration kieBaseTestConfiguration) {
        testIntervalTimer(kieBaseTestConfiguration, true);
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testIntervalTimer(KieBaseTestConfiguration kieBaseTestConfiguration) {
        testIntervalTimer(kieBaseTestConfiguration, false);
    }

    private void testIntervalTimer(KieBaseTestConfiguration kieBaseTestConfiguration, final boolean noProtocol) {
        final String drl =
                "package org.simple \n" +
                           "global java.util.List list \n" +
                           "rule xxx \n" +
                           (noProtocol ? "  duration (30s 10s) " : "  timer (int:30s 10s) ") +
                           "when \n" +
                           "then \n" +
                           "  list.add(\"fired\"); \n" +
                           "end  \n";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime(new Date().getTime(), TimeUnit.MILLISECONDS);

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(15, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(3, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(2, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);

            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(3);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testIntervalTimerWithoutFire(KieBaseTestConfiguration kieBaseTestConfiguration) {
        final String drl =
                "package org.simple \n" +
                           "global java.util.List list \n" +
                           "rule xxx \n" +
                           "  timer (int:30s 10s) " +
                           "when \n" +
                           "then \n" +
                           "  list.add(\"fired\"); \n" +
                           "end  \n";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSessionConfiguration kieSessionConfiguration =
                KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration();
        kieSessionConfiguration.setOption(TimedRuleExecutionOption.YES);
        final KieSession ksession = kbase.newKieSession(kieSessionConfiguration, null);
        try {
            final List list = new ArrayList();

            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime(new Date().getTime(), TimeUnit.MILLISECONDS);

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(35, TimeUnit.SECONDS);
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(10, TimeUnit.SECONDS);
            assertThat(list.size()).isEqualTo(2);

            timeService.advanceTime(10, TimeUnit.SECONDS);
            assertThat(list.size()).isEqualTo(3);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testExprIntervalTimerRaceCondition(KieBaseTestConfiguration kieBaseTestConfiguration) {
        final String drl =
                "package org.simple \n" +
                           "global java.util.List list \n" +
                           "rule xxx \n" +
                           "  timer (expr: $i, $i) \n" +
                           "when \n" +
                           "   $i : Long() \n" +
                           "then \n" +
                           "  list.add(\"fired\"); \n" +
                           "end  \n";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();

            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime(new Date().getTime(), TimeUnit.MILLISECONDS);

            ksession.setGlobal("list", list);
            final FactHandle fh = ksession.insert(10000L);

            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(17, TimeUnit.SECONDS);
            ksession.update(fh, 5000L);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testUnknownProtocol(KieBaseTestConfiguration kieBaseTestConfiguration) {
        wrongTimerExpression(kieBaseTestConfiguration, "xyz:30");
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testMissingColon(KieBaseTestConfiguration kieBaseTestConfiguration) {
        wrongTimerExpression(kieBaseTestConfiguration, "int 30");
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testMalformedExpression(KieBaseTestConfiguration kieBaseTestConfiguration) {
        wrongTimerExpression(kieBaseTestConfiguration, "30s s30");
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testMalformedIntExpression(KieBaseTestConfiguration kieBaseTestConfiguration) {
        wrongTimerExpression(kieBaseTestConfiguration, "int 30s");
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testMalformedCronExpression(KieBaseTestConfiguration kieBaseTestConfiguration) {
        wrongTimerExpression(kieBaseTestConfiguration, "cron: 0/30 * * * * *");
    }

    private void wrongTimerExpression(KieBaseTestConfiguration kieBaseTestConfiguration, final String timer) {
        final String drl =
                "package org.simple \n" +
                           "rule xxx \n" +
                           "  timer (" + timer + ") " +
                           "when \n" +
                           "then \n" +
                           "end  \n";

        final KieBuilder kieBuilder = KieUtil
            .getKieBuilderFromDrls(
                                   kieBaseTestConfiguration,
                                       false,
                                       drl);
        assertThat(kieBuilder.getResults().getMessages()).isNotEmpty();
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testCronTimer(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        final String drl =
                "package org.simple \n" +
                           "global java.util.List list \n" +
                           "rule xxx \n" +
                           "  timer (cron:15 * * * * ?) " +
                           "when \n" +
                           "then \n" +
                           "  list.add(\"fired\"); \n" +
                           "end  \n";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            final Date date = df.parse("2009-01-01T00:00:00.000-0000");

            timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(30, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(30, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testCalendarNormalRuleSingleCalendar(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        final String drl =
                "package org.simple \n" +
                           "global java.util.List list \n" +
                           "rule xxx \n" +
                           "  calendars \"cal1\"\n" +
                           "when \n" +
                           "  String()\n" +
                           "then \n" +
                           "  list.add(\"fired\"); \n" +
                           "end  \n";

        final Calendar calFalse = timestamp -> false;
        final Calendar calTrue = timestamp -> true;

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            final Date date = df.parse("2009-01-01T00:00:00.000-0000");

            ksession.getCalendars().set("cal1", calTrue);

            timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);
            ksession.setGlobal("list", list);
            ksession.insert("o1");
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.insert("o2");
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);

            ksession.getCalendars().set("cal1", calFalse);
            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.insert("o3");
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);

            ksession.getCalendars().set("cal1", calTrue);
            timeService.advanceTime(30, TimeUnit.SECONDS);
            ksession.insert("o4");
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(3);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
    public void testUndefinedCalendar(KieBaseTestConfiguration kieBaseTestConfiguration) {
        final String drl =
                "rule xxx \n" +
                           "  calendars \"cal1\"\n" +
                           "when \n" +
                           "then \n" +
                           "end  \n";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession();
        try {
            try {
                ksession.fireAllRules();
                fail("should throw UndefinedCalendarExcption");
            } catch (final UndefinedCalendarExcption ignored) {
            }
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testCalendarNormalRuleMultipleCalendars(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        final String drl =
                "package org.simple \n" +
                           "global java.util.List list \n" +
                           "rule xxx \n" +
                           "  calendars \"cal1\", \"cal2\"\n" +
                           "when \n" +
                           "  String()\n" +
                           "then \n" +
                           "  list.add(\"fired\"); \n" +
                           "end  \n";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final Calendar calFalse = timestamp -> false;

            final Calendar calTrue = timestamp -> true;

            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            final Date date = df.parse("2009-01-01T00:00:00.000-0000");

            ksession.getCalendars().set("cal1", calTrue);
            ksession.getCalendars().set("cal2", calTrue);

            timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);
            ksession.setGlobal("list", list);
            ksession.insert("o1");
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            ksession.getCalendars().set("cal2", calFalse);
            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.insert("o2");
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            ksession.getCalendars().set("cal1", calFalse);
            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.insert("o3");
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            ksession.getCalendars().set("cal1", calTrue);
            ksession.getCalendars().set("cal2", calTrue);
            timeService.advanceTime(30, TimeUnit.SECONDS);
            ksession.insert("o4");
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testCalendarsWithCron(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        final String drl =
                "package org.simple \n" +
                           "global java.util.List list \n" +
                           "rule xxx \n" +
                           "  calendars \"cal1\", \"cal2\"\n" +
                           "  timer (cron:15 * * * * ?) " +
                           "when \n" +
                           "then \n" +
                           "  list.add(\"fired\"); \n" +
                           "end  \n";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            final Date date = df.parse("2009-01-01T00:00:00.000-0000");

            timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);

            final Date date1 = new Date(date.getTime() + (15 * 1000));
            final Date date2 = new Date(date1.getTime() + (60 * 1000));
            final Date date3 = new Date(date2.getTime() + (60 * 1000));
            final Date date4 = new Date(date3.getTime() + (60 * 1000));

            final Calendar cal1 = timestamp -> {
                if (timestamp == date1.getTime()) {
                    return true;
                } else {
                    return timestamp != date4.getTime();
                }
            };

            final Calendar cal2 = timestamp -> {
                if (timestamp == date2.getTime()) {
                    return false;
                } else if (timestamp == date3.getTime()) {
                    return true;
                } else {
                    return true;
                }
            };

            ksession.getCalendars().set("cal1", cal1);
            ksession.getCalendars().set("cal2", cal2);

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(3);

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(4);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testCalendarsWithIntervals(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        final String drl =
                "package org.simple \n" +
                           "global java.util.List list \n" +
                           "rule xxx \n" +
                           "  calendars \"cal1\", \"cal2\"\n" +
                           "  timer (15s 60s) " + //int: protocol is assume
                           "when \n" +
                           "then \n" +
                           "  list.add(\"fired\"); \n" +
                           "end  \n";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            final Date date = df.parse("2009-01-01T00:00:00.000-0000");

            timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);

            final Date date1 = new Date(date.getTime() + (15 * 1000));
            final Date date2 = new Date(date1.getTime() + (60 * 1000));
            final Date date3 = new Date(date2.getTime() + (60 * 1000));
            final Date date4 = new Date(date3.getTime() + (60 * 1000));

            final Calendar cal1 = timestamp -> {
                if (timestamp == date1.getTime()) {
                    return true;
                } else {
                    return timestamp != date4.getTime();
                }
            };

            final Calendar cal2 = timestamp -> {
                if (timestamp == date2.getTime()) {
                    return false;
                } else if (timestamp == date3.getTime()) {
                    return true;
                } else {
                    return true;
                }
            };

            ksession.getCalendars().set("cal1", cal1);
            ksession.getCalendars().set("cal2", cal2);

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(3);

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(4);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testCalendarsWithIntervalsAndStartAndEnd(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        final String drl =
                "package org.simple \n" +
                           "global java.util.List list \n" +
                           "rule xxx \n" +
                           "  calendars \"cal1\"\n" +
                           "  timer (0d 1d; start=3-JAN-2010, end=5-JAN-2010) " + //int: protocol is assume
                           "when \n" +
                           "then \n" +
                           "  list.add(\"fired\"); \n" +
                           "end  \n";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.UK);
            final Date date = df.parse("1-JAN-2010");

            final Calendar cal1 = timestamp -> true;

            final long oneDay = 60 * 60 * 24;
            ksession.getCalendars().set("cal1", cal1);
            ksession.setGlobal("list", list);

            timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(oneDay, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(oneDay, TimeUnit.SECONDS); // day 3
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(oneDay, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);

            timeService.advanceTime(oneDay, TimeUnit.SECONDS); // day 5
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(3);

            timeService.advanceTime(oneDay, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(3);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testCalendarsWithIntervalsAndStartAndLimit(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        final String drl =
                "package org.simple \n" +
                           "global java.util.List list \n" +
                           "rule xxx \n" +
                           "  calendars \"cal1\"\n" +
                           "  timer (0d 1d; start=3-JAN-2010, repeat-limit=4) " + //int: protocol is assume
                           "when \n" +
                           "then \n" +
                           "  list.add(\"fired\"); \n" +
                           "end  \n";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.UK);
            final Date date = df.parse("1-JAN-2010");

            final Calendar cal1 = timestamp -> true;

            final long oneDay = 60 * 60 * 24;
            ksession.getCalendars().set("cal1", cal1);
            ksession.setGlobal("list", list);

            timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(oneDay, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(oneDay, TimeUnit.SECONDS); // day 3
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(oneDay, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);

            timeService.advanceTime(oneDay, TimeUnit.SECONDS); // day 5
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(3);

            timeService.advanceTime(oneDay, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(3);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testCalendarsWithCronAndStartAndEnd(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        final Locale defaultLoc = Locale.getDefault();
        try {
            Locale.setDefault(Locale.UK); // Because of the date strings in the DRL, fixable with JBRULES-3444
            final String drl =
                    "package org.simple \n" +
                               "global java.util.List list \n" +
                               "rule xxx \n" +
                               "  date-effective \"2-JAN-2010\"\n" +
                               "  date-expires \"6-JAN-2010\"\n" +
                               "  calendars \"cal1\"\n" +
                               "  timer (cron: 0 0 0 * * ?) " +
                               "when \n" +
                               "then \n" +
                               "  list.add(\"fired\"); \n" +
                               "end  \n";

            final KieBase kbase = KieBaseUtil
                .getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
            final KieSession ksession =
                    kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
            try {
                final List list = new ArrayList();
                final PseudoClockScheduler timeService = ksession.getSessionClock();
                final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.UK);
                final Date date = df.parse("1-JAN-2010");

                final Calendar cal1 = timestamp -> true;

                final long oneDay = 60 * 60 * 24;
                ksession.getCalendars().set("cal1", cal1);
                ksession.setGlobal("list", list);

                timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);
                ksession.fireAllRules();
                assertThat(list.size()).isEqualTo(0);

                timeService.advanceTime(oneDay, TimeUnit.SECONDS);
                ksession.fireAllRules();
                assertThat(list.size()).isEqualTo(0);

                timeService.advanceTime(oneDay, TimeUnit.SECONDS); // day 3
                ksession.fireAllRules();
                assertThat(list.size()).isEqualTo(1);

                timeService.advanceTime(oneDay, TimeUnit.SECONDS);
                ksession.fireAllRules();
                assertThat(list.size()).isEqualTo(2);

                timeService.advanceTime(oneDay, TimeUnit.SECONDS); // day 5
                ksession.fireAllRules();
                assertThat(list.size()).isEqualTo(3);

                timeService.advanceTime(oneDay, TimeUnit.SECONDS);
                ksession.fireAllRules();
                assertThat(list.size()).isEqualTo(3);
            } finally {
                ksession.dispose();
            }
        } finally {
            Locale.setDefault(defaultLoc);
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testCalendarsWithCronAndStartAndLimit(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        final Locale defaultLoc = Locale.getDefault();
        try {
            Locale.setDefault(Locale.UK); // Because of the date strings in the DRL, fixable with JBRULES-3444
            final String drl =
                    "package org.simple \n" +
                               "global java.util.List list \n" +
                               "rule xxx \n" +
                               "  date-effective \"2-JAN-2010\"\n" +
                               "  calendars \"cal1\"\n" +
                               // FIXME: I have to set the repeate-limit to 6 instead of 4 becuase
                               // it is incremented regardless of the effective date
                               "  timer (cron: 0 0 0 * * ?; repeat-limit=6) " +
                               "when \n" +
                               "then \n" +
                               "  list.add(\"fired\"); \n" +
                               "end  \n";

            final KieBase kbase = KieBaseUtil
                .getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
            final KieSession ksession =
                    kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
            try {
                final List list = new ArrayList();
                final PseudoClockScheduler timeService = ksession.getSessionClock();
                final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.UK);
                final Date date = df.parse("1-JAN-2010");

                final Calendar cal1 = timestamp -> true;

                final long oneDay = 60 * 60 * 24;
                ksession.getCalendars().set("cal1", cal1);
                ksession.setGlobal("list", list);

                timeService.advanceTime(date.getTime(), TimeUnit.MILLISECONDS);
                ksession.fireAllRules();
                assertThat(list.size()).isEqualTo(0);

                timeService.advanceTime(oneDay, TimeUnit.SECONDS);
                ksession.fireAllRules();
                assertThat(list.size()).isEqualTo(0);

                timeService.advanceTime(oneDay, TimeUnit.SECONDS); // day 3
                ksession.fireAllRules();
                assertThat(list.size()).isEqualTo(1);

                timeService.advanceTime(oneDay, TimeUnit.SECONDS);
                ksession.fireAllRules();
                assertThat(list.size()).isEqualTo(2);

                timeService.advanceTime(oneDay, TimeUnit.SECONDS); // day 5
                ksession.fireAllRules();
                assertThat(list.size()).isEqualTo(3);

                timeService.advanceTime(oneDay, TimeUnit.SECONDS);
                ksession.fireAllRules();
                assertThat(list.size()).isEqualTo(4);
            } finally {
                ksession.dispose();
            }
        } finally {
            Locale.setDefault(defaultLoc);
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testIntervalTimerWithLongExpressions(KieBaseTestConfiguration kieBaseTestConfiguration) {
        final String drl = "package org.simple;\n" +
                           "global java.util.List list;\n" +
                           "\n" +
                           "declare Bean\n" +
                           "  delay   : long = 30000\n" +
                           "  period  : long = 10000\n" +
                           "end\n" +
                           "\n" +
                           "rule init \n" +
                           "when \n" +
                           "then \n" +
                           " insert( new Bean() );\n" +
                           "end \n" +
                           "\n" +
                           "rule xxx\n" +
                           "  salience ($d) \n" +
                           "  timer( expr: $d, $p; start=3-JAN-2010 )\n" +
                           "when\n" +
                           "  Bean( $d : delay, $p : period )\n" +
                           "then\n" +
                           "  list.add( \"fired\" );\n" +
                           "end";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();

            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.setStartupTime(DateUtils.parseDate("3-JAN-2010").getTime());

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(15, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(3, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(2, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);

            timeService.advanceTime(10, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(3);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testIntervalTimerAfterEnd(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // DROOLS-5908
        final String drl = "package org.simple;\n" +
                           "global java.util.List list;\n" +
                           "\n" +
                           "declare Bean\n" +
                           "  delay   : long = 30000\n" +
                           "  period  : long = 10000\n" +
                           "end\n" +
                           "\n" +
                           "rule init \n" +
                           "when \n" +
                           "then \n" +
                           " insert( new Bean() );\n" +
                           "end \n" +
                           "\n" +
                           "rule xxx\n" +
                           "  salience ($d) \n" +
                           "  timer( expr: $d, $p; end=3-JAN-2010 )\n" +
                           "when\n" +
                           "  Bean( $d : delay, $p : period )\n" +
                           "then\n" +
                           "  list.add( \"fired\" );\n" +
                           "end";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();

            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.setStartupTime(DateUtils.parseDate("3-JAN-2010").getTime());

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testIntervalTimerWithStringExpressions(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkIntervalTimerWithStringExpressions(kieBaseTestConfiguration, false, "3-JAN-2010");
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testIntervalTimerWithAllExpressions(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkIntervalTimerWithStringExpressions(kieBaseTestConfiguration, true, "3-JAN-2010");
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testIntervalTimerWithStringExpressionsAfterStart(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkIntervalTimerWithStringExpressions(kieBaseTestConfiguration, false, "3-FEB-2010");
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testIntervalTimerWithAllExpressionsAfterStart(KieBaseTestConfiguration kieBaseTestConfiguration) {
        checkIntervalTimerWithStringExpressions(kieBaseTestConfiguration, true, "3-FEB-2010");
    }

    private void checkIntervalTimerWithStringExpressions(KieBaseTestConfiguration kieBaseTestConfiguration, final boolean useExprForStart, final String startTime) {
        final String drl = "package org.simple;\n" +
                           "global java.util.List list;\n" +
                           "\n" +
                           "declare Bean\n" +
                           "  delay   : String = \"30s\"\n" +
                           "  period  : long = 60000\n" +
                           "  start   : String = \"3-JAN-2010\"\n" +
                           "end\n" +
                           "\n" +
                           "rule init \n" +
                           "when \n" +
                           "then \n" +
                           " insert( new Bean() );\n" +
                           "end \n" +
                           "\n" +
                           "rule xxx\n" +
                           "  salience ($d) \n" +
                           "  timer( expr: $d, $p; start=" + (useExprForStart ? "$s" : "3-JAN-2010") + " )\n" +
                           "when\n" +
                           "  Bean( $d : delay, $p : period, $s : start )\n" +
                           "then\n" +
                           "  list.add( \"fired\" );\n" +
                           "end";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final List list = new ArrayList();

            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.setStartupTime(DateUtils.parseDate(startTime).getTime());

            ksession.setGlobal("list", list);

            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);

            timeService.advanceTime(40, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(3);

            // simulate a pause in the use of the engine by advancing the system clock
            timeService.setStartupTime(DateUtils.parseDate("3-MAR-2010").getTime());
            list.clear();

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1); // fires once to recover from missing activation

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);

            timeService.advanceTime(20, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);

            timeService.advanceTime(40, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(3);

            timeService.advanceTime(60, TimeUnit.SECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(4);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testIntervalTimerExpressionWithOr(KieBaseTestConfiguration kieBaseTestConfiguration) {
        final String drl = "package org.kie.test\n" + "global java.util.List list\n" + "import " +
                           FactA.class.getCanonicalName() + "\n" + "import " + FactB.class.getCanonicalName() + "\n" +
                           "import " + Pet.class.getCanonicalName() + "\n" +
                           "rule r1 timer (expr: f1.field2, f1.field2; repeat-limit=3)\n" + "when\n" +
                           "    foo: FactB()\n" + "    ( Pet()  and f1 : FactA( field1 == 'f1') ) or \n" +
                           "    f1 : FactA(field1 == 'f2') \n" + "then\n" + "    list.add( f1 );\n" +
                           "    foo.setF1( 'xxx' );\n" + "end\n" + "\n";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime(new Date().getTime(), TimeUnit.MILLISECONDS);

            final List list = new ArrayList();
            ksession.setGlobal("list", list);
            ksession.insert(new FactB());
            ksession.insert(new Pet("cinchilla"));

            final FactA fact1 = new FactA();
            fact1.setField1("f1");
            fact1.setField2(250);

            final FactA fact3 = new FactA();
            fact3.setField1("f2");
            fact3.setField2(1000);

            ksession.insert(fact1);
            ksession.insert(fact3);

            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(300, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);
            assertThat(list.get(0)).isEqualTo(fact1);

            timeService.advanceTime(300, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);
            assertThat(list.get(1)).isEqualTo(fact1);

            timeService.advanceTime(300, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2); // did not change, repeat-limit kicked in

            timeService.advanceTime(300, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(3);
            assertThat(list.get(2)).isEqualTo(fact3);

            timeService.advanceTime(1000, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(4);
            assertThat(list.get(3)).isEqualTo(fact3);

            timeService.advanceTime(1000, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(4); // did not change, repeat-limit kicked in
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    public void testExprTimeRescheduled(KieBaseTestConfiguration kieBaseTestConfiguration) {
        final String drl = "package org.kie.test\n" + "global java.util.List list\n" + "import " +
                           FactA.class.getCanonicalName() + "\n" + "rule r1 timer (expr: f1.field2, f1.field4)\n" +
                           "when\n" + "    f1 : FactA() \n" + "then\n" + "    list.add( f1 );\n" + "end\n" + "\n";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final PseudoClockScheduler timeService = ksession.getSessionClock();
            timeService.advanceTime(new Date().getTime(), TimeUnit.MILLISECONDS);

            final List list = new ArrayList();
            ksession.setGlobal("list", list);

            final FactA fact1 = new FactA();
            fact1.setField1("f1");
            fact1.setField2(500);
            fact1.setField4(1000);
            final FactHandle fh = ksession.insert(fact1);

            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(1100, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);
            assertThat(list.get(0)).isEqualTo(fact1);

            timeService.advanceTime(1100, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(2);
            assertThat(list.get(1)).isEqualTo(fact1);

            timeService.advanceTime(400, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(3);
            assertThat(list.get(2)).isEqualTo(fact1);
            list.clear();

            // the activation state of the rule is not changed so the timer isn't reset
            // since the timer alredy fired it will only use only the period that now will be set to 2000
            fact1.setField2(300);
            fact1.setField4(2000);
            ksession.update(fh, fact1);
            ksession.fireAllRules();

            // 100 has passed of the 1000, from the previous schedule
            // so that should be deducted from the 2000 period above, meaning
            //  we only need to increment another 1950
            timeService.advanceTime(1950, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);
            assertThat(list.get(0)).isEqualTo(fact1);
            list.clear();

            timeService.advanceTime(1000, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(700, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(0);

            timeService.advanceTime(300, TimeUnit.MILLISECONDS);
            ksession.fireAllRules();
            assertThat(list.size()).isEqualTo(1);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
    public void testExpiredPropagations(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // DROOLS-244
        final String drl = "package org.drools.test;\n" +
                           "\n" +
                           "import " + StockTick.class.getCanonicalName() + ";\n" +
                           "global java.util.List list;\n" +
                           "\n" +
                           "declare StockTick\n" +
                           "    @role( event )\n" +
                           "    @timestamp( time )\n" +
                           "end\n" +
                           "\n" +
                           "declare window ATicks\n" +
                           " StockTick( company == \"AAA\" ) over window:time( 1s ) " +
                           " from entry-point \"AAA\"\n" +
                           "end\n" +
                           "\n" +
                           "declare window BTicks\n" +
                           " StockTick( company == \"BBB\" ) over window:time( 1s ) " +
                           " from entry-point \"BBB\"\n" +
                           "end\n" +
                           "\n" +
                           "rule Ticks \n" +
                           " when\n" +
                           " String()\n" +
                           " accumulate( $x : StockTick() from window ATicks, $a : count( $x ) )\n" +
                           " accumulate( $y : StockTick() from window BTicks, $b : count( $y ) )\n" +
                           " accumulate( $z : StockTick() over window:time( 1s ), $c : count( $z ) )\n" +
                           " then\n" +
                           " list.add( $a );\n" +
                           " list.add( $b );\n" +
                           " list.add( $c );\n" +
                           "end";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession =
                kbase.newKieSession(KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration(), null);
        try {
            final ArrayList list = new ArrayList();
            ksession.setGlobal("list", list);

            final SessionPseudoClock clock = ksession.getSessionClock();

            clock.advanceTime(1100, TimeUnit.MILLISECONDS);

            final StockTick tick = new StockTick(0, "AAA", 1.0, 0);
            final StockTick tock = new StockTick(1, "BBB", 1.0, 2500);
            final StockTick tack = new StockTick(1, "CCC", 1.0, 2700);

            final EntryPoint epa = ksession.getEntryPoint("AAA");
            final EntryPoint epb = ksession.getEntryPoint("BBB");

            epa.insert(tick);
            epb.insert(tock);
            ksession.insert(tack);

            FactHandle handle = ksession.insert("go1");
            ksession.fireAllRules();
            assertThat(list).isEqualTo(asList(0L, 1L, 1L));
            list.clear();
            ksession.delete(handle);

            clock.advanceTime(2550, TimeUnit.MILLISECONDS);

            handle = ksession.insert("go2");
            ksession.fireAllRules();
            assertThat(list).isEqualTo(asList(0L, 0L, 1L));
            list.clear();
            ksession.delete(handle);

            clock.advanceTime(500, TimeUnit.MILLISECONDS);

            handle = ksession.insert("go3");
            ksession.fireAllRules();
            assertThat(list).isEqualTo(asList(0L, 0L, 0L));
            list.clear();
            ksession.delete(handle);
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
    public void testCronFire(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // BZ-1059372
        final String drl = "package test.drools\n" +
                           "rule TestRule " +
                           "  timer (cron:* * * * * ?) " +
                           "when\n" +
                           "    String() " +
                           "    Integer() " +
                           "then\n" +
                           "end\n";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSession ksession = kbase.newKieSession();
        try {
            final int repetitions = 10000;
            for (int j = 0; j < repetitions; j++) {
                ksession.insert(j);
            }

            ksession.insert("go");
            ksession.fireAllRules();
        } finally {
            ksession.dispose();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
	@MethodSource("parameters")
	@Timeout(10000)
    @Disabled("the listener callback holds some locks so blocking in it is not safe")
    public void testRaceConditionWithTimedRuleExectionOption(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        // BZ-1073880
        final String drl = "package org.simple \n" +
                           "global java.util.List list \n" +
                           "rule xxx @Propagation(EAGER)\n" +
                           "  timer (int:30s 10s) " + "when \n" +
                           "  $s: String()\n" +
                           "then \n" +
                           "  list.add($s); \n" +
                           "end  \n";

        final KieBase kbase =
                KieBaseUtil.getKieBaseFromKieModuleFromDrl("timer-and-calendar-test", kieBaseTestConfiguration, drl);
        final KieSessionConfiguration kieSessionConfiguration =
                KieSessionTestConfiguration.STATEFUL_PSEUDO.getKieSessionConfiguration();
        kieSessionConfiguration.setOption(TimedRuleExecutionOption.YES);
        final KieSession ksession = kbase.newKieSession(kieSessionConfiguration, null);
        try {
            final CyclicBarrier barrier = new CyclicBarrier(2);
            final AtomicBoolean aBool = new AtomicBoolean(true);
            final AgendaEventListener agendaEventListener = new DefaultAgendaEventListener() {

                @Override
                public void afterMatchFired(final org.kie.api.event.rule.AfterMatchFiredEvent event) {
                    try {
                        if (aBool.get()) {
                            barrier.await();
                            aBool.set(false);
                        }
                    } catch (final Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            ksession.addEventListener(agendaEventListener);

            final List list = new ArrayList();
            ksession.setGlobal("list", list);

            // Using the Pseudo Clock.
            final SessionClock clock = ksession.getSessionClock();
            final SessionPseudoClock pseudoClock = (SessionPseudoClock) clock;

            // Insert the event.
            final String eventOne = "one";
            ksession.insert(eventOne);

            // Advance the time .... so the timer will fire.
            pseudoClock.advanceTime(10000, TimeUnit.MILLISECONDS);

            // Rule doesn't fire in PHREAK. This is because you need to call 'fireAllRules' after you've inserted the fact, otherwise the timer
            // job is not created.

            ksession.fireAllRules();

            // Rule still doesn't fire, because the DefaultTimerJob is created now, and now we need to advance the timer again.

            pseudoClock.advanceTime(30000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();
            aBool.set(true);

            pseudoClock.advanceTime(10000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();
            aBool.set(true);

            final String eventTwo = "two";
            ksession.insert(eventTwo);
            ksession.fireAllRules();

            // 60
            pseudoClock.advanceTime(10000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();
            aBool.set(true);

            // 70
            pseudoClock.advanceTime(10000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();
            aBool.set(true);

            //From here, the second rule should fire.
            //phaser.register();
            pseudoClock.advanceTime(10000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();
            aBool.set(true);

            // Now 2 rules have fired, and those will now fire every 10 seconds.
            pseudoClock.advanceTime(20000, TimeUnit.MILLISECONDS);
            barrier.await();
            barrier.reset();

            pseudoClock.advanceTime(20000, TimeUnit.MILLISECONDS);
            aBool.set(true);
            barrier.await();
            barrier.reset();

            pseudoClock.advanceTime(20000, TimeUnit.MILLISECONDS);
            aBool.set(true);
            barrier.await();
            barrier.reset();
        } finally {
            ksession.dispose();
        }
    }

}
