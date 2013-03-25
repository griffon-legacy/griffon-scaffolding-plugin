/*
 * Copyright 2009-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package griffon.plugins.scaffolding;

import griffon.core.resources.editors.ExtendedPropertyEditor;
import griffon.core.resources.editors.PropertyEditorResolver;
import griffon.core.resources.editors.ValueConversionException;
import griffon.exceptions.GriffonException;
import griffon.plugins.validation.constraints.ConstrainedProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;

import static griffon.util.GriffonClassUtils.getPropertyDescriptor;
import static griffon.util.GriffonExceptionHandler.sanitize;

/**
 * @author Andres Almiray
 */
public abstract class AbstractPropertyBinding implements Disposable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPropertyBinding.class);

    protected ConstrainedProperty constrainedProperty;
    private final Object LOCK = new Object[0];
    private boolean firing = false;

    protected AbstractPropertyBinding(ConstrainedProperty constrainedProperty) {
        this.constrainedProperty = constrainedProperty;
    }

    protected void bind() {
        bindSource();
        bindTarget();

        if (getTargetPropertyValue() != null) {
            updateSource();
        } else {
            updateTarget();
        }
    }

    public void dispose() {
        constrainedProperty = null;
    }

    protected void updateSource() {
        synchronized (LOCK) {
            if (firing) return;
            firing = true;
            try {
                PropertyEditor targetEditor = resolveTargetPropertyEditor();
                PropertyEditor sourceEditor = resolveSourcePropertyEditor();
                targetEditor.setValue(getTargetPropertyValue());
                Object targetValue = targetEditor.getValue();
                if (targetEditor instanceof ExtendedPropertyEditor) {
                    targetValue = ((ExtendedPropertyEditor) targetEditor).getFormattedValue();
                }
                sourceEditor.setValue(targetValue);
                setSourcePropertyValue(sourceEditor.getValue());
            } catch (ValueConversionException e) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Could not update target property '" + constrainedProperty.getPropertyName() + "'", sanitize(e));
                }
            } finally {
                firing = false;
            }
        }
    }

    protected void updateTarget() {
        synchronized (LOCK) {
            if (firing) return;
            firing = true;
            try {
                PropertyEditor targetEditor = resolveTargetPropertyEditor();
                targetEditor.setValue(getSourcePropertyValue());
                setTargetPropertyValue(targetEditor.getValue());
            } catch (ValueConversionException e) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Could not update source property", sanitize(e));
                }
                setTargetPropertyValue(null);
            } finally {
                firing = false;
            }
        }
    }

    protected abstract void bindSource();

    protected abstract void bindTarget();

    protected abstract Object getTargetPropertyValue();

    protected abstract void setTargetPropertyValue(Object value);

    protected abstract void setSourcePropertyValue(Object value);

    protected abstract Object getSourcePropertyValue();

    protected PropertyEditor resolveTargetPropertyEditor() {
        PropertyEditor editor = doResolveTargetPropertyEditor();
        configureTargetPropertyEditor(editor);
        return editor;
    }

    protected PropertyEditor doResolveTargetPropertyEditor() {
        return PropertyEditorResolver.findEditor(constrainedProperty.getPropertyType());
    }

    protected void configureTargetPropertyEditor(PropertyEditor editor) {
        if (editor instanceof ExtendedPropertyEditor) {
            ((ExtendedPropertyEditor) editor).setFormat(constrainedProperty.getFormat());
        }
    }

    protected abstract PropertyEditor resolveSourcePropertyEditor();

    protected PropertyDescriptor resolvePropertyDescriptor(Object source, String sourcePropertyName) {
        try {
            return getPropertyDescriptor(source, sourcePropertyName);
        } catch (IllegalAccessException e) {
            throw new GriffonException(e);
        } catch (InvocationTargetException e) {
            throw new GriffonException(e.getTargetException());
        } catch (NoSuchMethodException e) {
            throw new GriffonException(e);
        }
    }
}
