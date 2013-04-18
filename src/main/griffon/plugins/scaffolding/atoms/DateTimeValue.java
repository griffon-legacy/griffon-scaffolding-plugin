/*
 * Copyright 2009-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package griffon.plugins.scaffolding.atoms;

import org.joda.time.*;

import java.util.Calendar;
import java.util.Date;

/**
 * @author Andres Almiray
 */
public class DateTimeValue extends AbstractAtomicValue implements NumericAtomicValue {
    public DateTimeValue() {
    }

    public DateTimeValue(DateTime arg) {
        setValue(arg);
    }

    public DateTimeValue(DateMidnight arg) {
        setValue(arg);
    }

    public DateTimeValue(Instant arg) {
        setValue(arg);
    }

    public DateTimeValue(LocalDate arg) {
        setValue(arg);
    }

    public DateTimeValue(LocalDateTime arg) {
        setValue(arg);
    }

    public DateTimeValue(LocalTime arg) {
        setValue(arg);
    }

    public DateTimeValue(Date arg) {
        setValue(arg);
    }

    public DateTimeValue(Number arg) {
        setValue(arg);
    }

    public DateTime dateTimeValue() {
        return (DateTime) value;
    }

    @Override
    public void setValue(Object value) {
        if (value == null || value instanceof DateTime) {
            super.setValue(value);
        } else if (value instanceof DateMidnight) {
            super.setValue(((DateMidnight) value).toDateTime());
        } else if (value instanceof Instant) {
            super.setValue(((Instant) value).toDateTime());
        } else if (value instanceof LocalDate) {
            super.setValue(((LocalDate) value).toDateTimeAtStartOfDay());
        } else if (value instanceof LocalDateTime) {
            super.setValue(((LocalDateTime) value).toDateTime());
        } else if (value instanceof LocalTime) {
            super.setValue(((LocalTime) value).toDateTimeToday());
        } else if (value instanceof Calendar || value instanceof Date) {
            super.setValue(new DateTime(value));
        } else if (value instanceof Number) {
            super.setValue(new DateTime(((Number) value).longValue()));
        } else {
            throw new IllegalArgumentException("Invalid value " + value);
        }
    }

    public Class getValueType() {
        return DateTime.class;
    }
}
