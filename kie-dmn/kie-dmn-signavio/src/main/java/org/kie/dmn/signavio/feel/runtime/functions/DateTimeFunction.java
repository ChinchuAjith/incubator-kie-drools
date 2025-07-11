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
package org.kie.dmn.signavio.feel.runtime.functions;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;

import org.kie.dmn.feel.runtime.functions.BaseFEELFunction;
import org.kie.dmn.feel.runtime.functions.BuiltInFunctions;
import org.kie.dmn.feel.runtime.functions.DateAndTimeFunction;
import org.kie.dmn.feel.runtime.functions.FEELFnResult;
import org.kie.dmn.feel.runtime.functions.ParameterName;

/**
 * This implementation is a signavio profile implementation.
 * For the standard implementation, see DateAndTimeFunction
 */
public class DateTimeFunction
        extends BaseFEELFunction {

    public DateTimeFunction() {
        super( "dateTime" );
    }

    public FEELFnResult<TemporalAccessor> invoke(@ParameterName("from") String val) {
        return BuiltInFunctions.getFunction( DateAndTimeFunction.class ).invoke( val );
    }

    public FEELFnResult<TemporalAccessor> invoke(@ParameterName("date") Temporal date, @ParameterName("time") Temporal time) {
        return BuiltInFunctions.getFunction( DateAndTimeFunction.class ).invoke( date, time );
    }

    public FEELFnResult<TemporalAccessor> invoke(
            @ParameterName("year") Number year, @ParameterName("month") Number month, @ParameterName("day") Number day,
            @ParameterName("hour") Number hour, @ParameterName("minute") Number minute, @ParameterName("second") Number second) {
        return BuiltInFunctions.getFunction( DateAndTimeFunction.class ).invoke( year, month, day, hour, minute, second );
    }

    public FEELFnResult<TemporalAccessor> invoke(
            @ParameterName("year") Number year, @ParameterName("month") Number month, @ParameterName("day") Number day,
            @ParameterName("hour") Number hour, @ParameterName("minute") Number minute, @ParameterName("second") Number second,
            @ParameterName("hour offset") Number hourOffset) {
        return BuiltInFunctions.getFunction( DateAndTimeFunction.class ).invoke( year, month, day, hour, minute, second, hourOffset );
    }

}
