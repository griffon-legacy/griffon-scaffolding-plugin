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

import griffon.exceptions.BeanInstantiationException;
import griffon.plugins.scaffolding.AtomicValue;
import org.codehaus.griffon.runtime.core.AbstractObservable;

import static griffon.util.GriffonExceptionHandler.sanitize;

/**
 * @author Andres Almiray
 */
public abstract class AbstractAtomicValue extends AbstractObservable implements AtomicValue, Comparable<AtomicValue> {
    protected Object value;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        firePropertyChange("value", this.value, this.value = value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtomicValue)) return false;

        AtomicValue that = (AtomicValue) o;

        if (value != null ? !value.equals(that.getValue()) : that.getValue() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    public String toString() {
        return null == value ? "<null>" : String.valueOf(value);
    }

    public int compareTo(AtomicValue other) {
        if (this == other) return 0;
        if (other == null || !getClass().isAssignableFrom(other.getClass())) return -1;
        Object otherValue = other.getValue();

        if (value == otherValue) return 0;
        if (value != null && otherValue == null) return -1;
        if (value == null && otherValue != null) return 1;
        if (value instanceof Comparable) return ((Comparable) value).compareTo(otherValue);
        if (otherValue instanceof Comparable) return ((Comparable) otherValue).compareTo(value);
        return -1;
    }

    public static AtomicValue wrap(Object value, Class atomicValueType) {
        try {
            AtomicValue atom = (AtomicValue) atomicValueType.newInstance();
            atom.setValue(value);
            return atom;
        } catch (InstantiationException e) {
            BeanInstantiationException x = new BeanInstantiationException(e);
            sanitize(x);
            throw x;
        } catch (IllegalAccessException e) {
            BeanInstantiationException x = new BeanInstantiationException(e);
            sanitize(x);
            throw x;
        }
    }
}
