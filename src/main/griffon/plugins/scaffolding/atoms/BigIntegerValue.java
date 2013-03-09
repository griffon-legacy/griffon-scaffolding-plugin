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

import java.math.BigInteger;

/**
 * @author Andres Almiray
 */
public class BigIntegerValue extends AbstractAtomicValue implements NumericAtomicValue {
    public BigIntegerValue() {
    }

    public BigIntegerValue(BigInteger arg) {
        setValue(arg);
    }

    public BigIntegerValue(Number arg) {
        setValue(arg);
    }
    
    public BigInteger bigIntegerValue() {
        return (BigInteger) value;
    }

    @Override
    public void setValue(Object value) {
        if (value == null || value instanceof BigInteger) {
            super.setValue(value);
        } else if (value instanceof Number) {
            long val = ((Number) value).longValue();
            super.setValue(BigInteger.valueOf(val));
        } else {
            throw new IllegalArgumentException("Invalid value " + value);
        }
    }

    public Class getValueType() {
        return BigInteger.class;
    }
}
