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

import java.util.Calendar;
import java.util.Date;

/**
 * @author Andres Almiray
 */
public class CalendarValue extends AbstractAtomicValue implements NumericAtomicValue {
    public CalendarValue() {
    }

    public CalendarValue(Calendar arg) {
        setValue(arg);
    }

    public CalendarValue(Number arg) {
        setValue(arg);
    }

    public Calendar calendarValue() {
        return (Calendar) value;
    }

    @Override
    public void setValue(Object value) {
        if (value == null || value instanceof Calendar) {
            super.setValue(value);
        } else if (value instanceof Date) {
            Calendar c = Calendar.getInstance();
            c.setTime((Date) value);
            super.setValue(c);
        } else if (value instanceof Number) {
            Date d = new Date();
            d.setTime(((Number) value).longValue());
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            super.setValue(c);
        } else {
            throw new IllegalArgumentException("Invalid value " + value);
        }
    }

    public Class getValueType() {
        return Calendar.class;
    }
}
