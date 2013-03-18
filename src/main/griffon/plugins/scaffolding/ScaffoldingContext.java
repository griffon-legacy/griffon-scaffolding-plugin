/*
 * Copyright 2013 the original author or authors.
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

package griffon.plugins.scaffolding;

import griffon.core.GriffonController;
import griffon.core.i18n.NoSuchMessageException;
import griffon.plugins.validation.ObjectError;
import griffon.plugins.validation.Validateable;
import griffon.plugins.validation.constraints.ConstrainedProperty;
import griffon.util.ApplicationClassLoader;
import groovy.lang.Binding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static griffon.plugins.scaffolding.CommandObjectUtils.dot;
import static griffon.plugins.scaffolding.CommandObjectUtils.qualifyCommandObject;
import static griffon.util.GriffonNameUtils.isBlank;

/**
 * @author Andres Almiray
 */
public class ScaffoldingContext implements Disposable {
    private final Logger LOG = LoggerFactory.getLogger(ScaffoldingContext.class);
    private Binding binding;
    private GriffonController controller;
    private String actionName;
    private Validateable validateable;

    private final Map<String, Class> widgetTemplates = new LinkedHashMap<String, Class>();
    private final List<Disposable> disposables = new ArrayList<Disposable>();

    protected ScaffoldingContext() {

    }

    public Binding getBinding() {
        return binding;
    }

    public void setBinding(Binding binding) {
        this.binding = binding;
    }

    public GriffonController getController() {
        return controller;
    }

    public void setController(GriffonController controller) {
        this.controller = controller;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public Validateable getValidateable() {
        return validateable;
    }

    public void setValidateable(Validateable validateable) {
        this.validateable = validateable;
    }

    public void addDisposable(Disposable disposable) {
        if (disposable == null || disposables.contains(disposable)) return;
        disposables.add(disposable);
    }

    public void dispose() {
        controller = null;
        validateable = null;
        binding = null;
        for (Disposable disposable : disposables) {
            disposable.dispose();
        }
        disposables.clear();
    }

    public String resolveMessage(String key, String defaultValue) {
        return CommandObjectUtils.resolveMessage(controller, actionName, (CommandObject) validateable, key, defaultValue);
    }

    public Class resolveWidget(String property) {
        Class widgetTemplate = widgetTemplates.get(property);

        if (widgetTemplate == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Resolving widget template for " + qualify(property));
            }
            ConstrainedProperty constrainedProperty = validateable.constrainedProperties().get(property);
            if (!isBlank(constrainedProperty.getWidget())) {
                for (String widgetName : CommandObjectUtils.widgetTemplates(controller, actionName, (CommandObject) validateable, constrainedProperty.getWidget())) {
                    try {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("  Resolving " + widgetName);
                        }
                        widgetTemplate = ApplicationClassLoader.get().loadClass(widgetName);
                        break;
                    } catch (Exception e) {
                        // continue
                    }
                }
            }

            if (widgetTemplate == null) {
                for (String widgetName : CommandObjectUtils.propertyTemplates(controller, actionName, (CommandObject) validateable, property)) {
                    try {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("  Resolving " + widgetName);
                        }
                        widgetTemplate = ApplicationClassLoader.get().loadClass(widgetName);
                        break;
                    } catch (Exception e) {
                        // continue
                    }
                }
            }

            if (widgetTemplate == null) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("  Could not resolve a suitable widget template for " + qualify(property));
                }
                throw new IllegalArgumentException("Could not resolve a suitable widget template for " + qualify(property));
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Resolved widget template for " + qualify(property) + " is " + widgetTemplate.getName());
            }
            widgetTemplates.put(property, widgetTemplate);
        }

        return widgetTemplate;
    }

    public String[] resolveErrorMessages() {
        List<String> errors = new ArrayList<String>();

        for (ObjectError error : validateable.getErrors().getAllErrors()) {
            boolean messageResolved = false;
            for (String errorCode : error.getCodes()) {
                try {
                    String message = controller.getApp().getMessage(errorCode, error.getArguments());
                    errors.add(message);
                    messageResolved = true;
                    break;
                } catch (NoSuchMessageException e) {
                    // continue;
                }
            }
            if (!messageResolved) {
                errors.add(MessageFormat.format(error.getDefaultMessage(), error.getArguments()));
            }
        }

        return errors.toArray(new String[errors.size()]);
    }

    private String qualify() {
        return qualifyCommandObject(controller, actionName, (CommandObject) validateable);
    }

    private String qualify(String extra) {
        return dot(qualifyCommandObject(controller, actionName, (CommandObject) validateable), extra);
    }
}
