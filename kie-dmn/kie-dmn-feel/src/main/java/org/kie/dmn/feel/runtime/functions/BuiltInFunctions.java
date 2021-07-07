/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.dmn.feel.runtime.functions;

import java.util.stream.Stream;

import org.kie.dmn.feel.runtime.FEELFunction;
import org.kie.dmn.feel.runtime.functions.interval.AfterFunction;
import org.kie.dmn.feel.runtime.functions.interval.BeforeFunction;
import org.kie.dmn.feel.runtime.functions.interval.CoincidesFunction;
import org.kie.dmn.feel.runtime.functions.interval.DuringFunction;
import org.kie.dmn.feel.runtime.functions.interval.FinishedByFunction;
import org.kie.dmn.feel.runtime.functions.interval.FinishesFunction;
import org.kie.dmn.feel.runtime.functions.interval.IncludesFunction;
import org.kie.dmn.feel.runtime.functions.interval.MeetsFunction;
import org.kie.dmn.feel.runtime.functions.interval.MetByFunction;
import org.kie.dmn.feel.runtime.functions.interval.OverlapsAfterFunction;
import org.kie.dmn.feel.runtime.functions.interval.OverlapsBeforeFunction;
import org.kie.dmn.feel.runtime.functions.interval.OverlapsFunction;
import org.kie.dmn.feel.runtime.functions.interval.StartedByFunction;
import org.kie.dmn.feel.runtime.functions.interval.StartsFunction;
import org.kie.dmn.feel.util.ClassUtil;

public class BuiltInFunctions {

    protected static final FEELFunction[] FUNCTIONS = new FEELFunction[]{
            DateFunction.INSTANCE,
            TimeFunction.INSTANCE,
            DateAndTimeFunction.INSTANCE,
            DurationFunction.INSTANCE,
            YearsAndMonthsFunction.INSTANCE,
            new StringFunction(),
            new NumberFunction(),
            new SubstringFunction(),
            new SubstringBeforeFunction(),
            new SubstringAfterFunction(),
            new StringLengthFunction(),
            new StringUpperCaseFunction(),
            new StringLowerCaseFunction(),
            new ContainsFunction(),
            new StartsWithFunction(),
            new EndsWithFunction(),
            new MatchesFunction(),
            new ReplaceFunction(),
            new ListContainsFunction(),
            new CountFunction(),
            new MinFunction(),
            new MaxFunction(),
            new SumFunction(),
            new MeanFunction(),
            new SublistFunction(),
            new AppendFunction(),
            new ConcatenateFunction(),
            new InsertBeforeFunction(),
            new RemoveFunction(),
            new ReverseFunction(),
            new IndexOfFunction(),
            new UnionFunction(),
            new DistinctValuesFunction(),
            new FlattenFunction(),
            new DecimalFunction(),
            new FloorFunction(),
            new CeilingFunction(),
            new DecisionTableFunction(),
            new NotFunction(),
            new SortFunction(),
            new GetEntriesFunction(),
            new GetValueFunction(),

            new AllFunction(),
            new AnyFunction(),
            AbsFunction.INSTANCE,
            ModuloFunction.INSTANCE,
            ProductFunction.INSTANCE,
            SplitFunction.INSTANCE,
            StddevFunction.INSTANCE,
            ModeFunction.INSTANCE,
            SqrtFunction.INSTANCE,
            LogFunction.INSTANCE,
            ExpFunction.INSTANCE,
            EvenFunction.INSTANCE,
            OddFunction.INSTANCE,
            MedianFunction.INSTANCE,

            DayOfWeekFunction.INSTANCE,
            DayOfYearFunction.INSTANCE,
            MonthOfYearFunction.INSTANCE,
            WeekOfYearFunction.INSTANCE,

            IsFunction.INSTANCE,

            // Interval based logic
            AfterFunction.INSTANCE,
            BeforeFunction.INSTANCE,
            CoincidesFunction.INSTANCE,
            StartsFunction.INSTANCE,
            StartedByFunction.INSTANCE,
            FinishesFunction.INSTANCE,
            FinishedByFunction.INSTANCE,
            DuringFunction.INSTANCE,
            IncludesFunction.INSTANCE,
            OverlapsFunction.INSTANCE,
            OverlapsBeforeFunction.INSTANCE,
            OverlapsAfterFunction.INSTANCE,
            MeetsFunction.INSTANCE,
            MetByFunction.INSTANCE
    };

    public static FEELFunction[] getFunctions() {
        return FUNCTIONS;
    }

    public static <T extends FEELFunction> T getFunction(Class<T> functionClazz) {
        return (T) Stream.of(FUNCTIONS)
                .filter(f -> ClassUtil.isAssignableFrom(functionClazz, f.getClass()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cannot find function by class " + functionClazz.getCanonicalName() + "!"));
    }
}
