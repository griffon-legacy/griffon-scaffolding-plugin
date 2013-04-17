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

import org.joda.time.Days;

import static java.lang.Math.abs;

/**
 * @author Andres Almiray
 */
public class DaysValue extends AbstractAtomicValue {
    public DaysValue() {
    }

    public DaysValue(Days arg) {
        setValue(arg);
    }

    public DaysValue(Number arg) {
        setValue(arg);
    }

    public Days daysValue() {
        return (Days) value;
    }

    @Override
    public void setValue(Object value) {
        if (value == null || value instanceof Days) {
            super.setValue(value);
        } else if (value instanceof Number) {
            super.setValue(Days.days(abs(((Number) value).intValue())));
        } else {
            throw new IllegalArgumentException("Invalid value " + value);
        }
    }

    public Class getValueType() {
        return Days.class;
    }
}
