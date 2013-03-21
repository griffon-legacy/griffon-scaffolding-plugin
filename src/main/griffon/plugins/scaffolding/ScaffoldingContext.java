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
import griffon.plugins.validation.FieldObjectError;
import griffon.plugins.validation.ObjectError;
import griffon.plugins.validation.Validateable;
import griffon.plugins.validation.constraints.ConstrainedProperty;
import griffon.util.ApplicationClassLoader;
import groovy.lang.Binding;
import groovy.util.ConfigObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static griffon.plugins.scaffolding.ScaffoldingUtils.*;
import static griffon.util.ConfigUtils.getConfigValue;
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
    private final Map<String, String> errorCodes = new TreeMap<String, String>();

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

    @SuppressWarnings("unchecked")
    public Map<String, Object> widgetAttributes(String widget, ConstrainedProperty constrainedProperty) {
        ConfigObject config = new ConfigObject();
        config.putAll((ConfigObject) getConfigValue(getUiDefaults(), widget, new ConfigObject()));
        config.putAll(constrainedProperty.getAttributes());
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.putAll(config);
        attributes.put("id", constrainedProperty.getPropertyName());
        attributes.put("enabled", constrainedProperty.isEnabled());
        return attributes;
    }

    public String resolveMessage(String key, String defaultValue) {
        return ScaffoldingUtils.resolveMessage(controller, actionName, validateable, key, defaultValue);
    }

    public Class resolveWidget(String property) {
        Class widgetTemplate = widgetTemplates.get(property);

        if (widgetTemplate == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Resolving widget template for " + qualify(property));
            }
            ConstrainedProperty constrainedProperty = validateable.constrainedProperties().get(property);
            if (!isBlank(constrainedProperty.getWidget())) {
                widgetTemplate = resolveWidgetTemplateByWidget(constrainedProperty);
            }

            if (widgetTemplate == null) {
                widgetTemplate = resolveWidgetTemplateByProperty(property);
            }

            if (widgetTemplate == null) {
                widgetTemplate = resolveWidgetTemplateByUnknown();
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

    private Class resolveWidgetTemplateByWidget(ConstrainedProperty constrainedProperty) {
        Class widgetTemplate = null;

        // attempt i18n resolution first
        if (LOG.isDebugEnabled()) {
            LOG.debug("  [I18N]");
        }
        for (String resourceKey : ScaffoldingUtils.widgetTemplates(controller, actionName, validateable, constrainedProperty.getWidget())) {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("  Resolving " + resourceKey);
                }
                String widgetTemplateClassName = getController().getApp().getMessage(resourceKey);
                widgetTemplate = ApplicationClassLoader.get().loadClass(widgetTemplateClassName);
                break;
            } catch (Exception e) {
                // continue
            }
        }

        // attempt direct class load
        if (widgetTemplate == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("  [CLASS]");
            }
            for (String widgetName : ScaffoldingUtils.widgetTemplates(controller, actionName, validateable, constrainedProperty.getWidget())) {
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

        return widgetTemplate;
    }

    private Class resolveWidgetTemplateByProperty(String property) {
        Class widgetTemplate = null;

        // attempt i18n resolution first
        if (LOG.isDebugEnabled()) {
            LOG.debug("  [I18N]");
        }
        for (String resourceKey : ScaffoldingUtils.propertyTemplates(controller, actionName, validateable, property)) {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("  Resolving " + resourceKey);
                }
                String widgetTemplateClassName = getController().getApp().getMessage(resourceKey);
                widgetTemplate = ApplicationClassLoader.get().loadClass(widgetTemplateClassName);
                break;
            } catch (Exception e) {
                // continue
            }
        }

        // attempt direct class load
        if (LOG.isDebugEnabled()) {
            LOG.debug("  [CLASS]");
        }
        if (widgetTemplate == null) {
            for (String widgetName : ScaffoldingUtils.propertyTemplates(controller, actionName, validateable, property)) {
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

        return widgetTemplate;
    }

    private Class resolveWidgetTemplateByUnknown() {
        Class widgetTemplate = null;

        // attempt i18n resolution first
        if (LOG.isDebugEnabled()) {
            LOG.debug("  [I18N]");
        }
        for (String resourceKey : ScaffoldingUtils.unknownWidgetTemplates()) {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("  Resolving " + resourceKey);
                }
                String widgetTemplateClassName = getController().getApp().getMessage(resourceKey);
                widgetTemplate = ApplicationClassLoader.get().loadClass(widgetTemplateClassName);
                break;
            } catch (Exception e) {
                // continue
            }
        }

        // attempt direct class load
        if (widgetTemplate == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("  [CLASS]");
            }
            for (String widgetName : ScaffoldingUtils.unknownWidgetTemplates()) {
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

        return widgetTemplate;
    }

    public String[] resolveErrorMessages() {
        List<String> errors = new ArrayList<String>();

        for (ObjectError error : validateable.getErrors().getAllErrors()) {
            String errorKey = errorKey(error);
            String errorCode = errorCodes.get(errorKey);
            if (!isBlank(errorCode)) {
                errors.add(controller.getApp().formatMessage(errorCode, error.getArguments()));
                continue;
            }

            boolean messageResolved = false;
            for (String code : error.getCodes()) {
                try {
                    String message = controller.getApp().getMessage(code, error.getArguments());
                    errors.add(message);
                    errorCodes.put(errorKey, code);
                    messageResolved = true;
                    break;
                } catch (NoSuchMessageException e) {
                    // continue;
                }
            }
            if (!messageResolved) {
                errors.add(controller.getApp().formatMessage(error.getDefaultMessage(), error.getArguments()));
                errorCodes.put(errorKey, error.getDefaultMessage());
            }
        }

        return errors.toArray(new String[errors.size()]);
    }

    private String qualify() {
        return qualifyActionValidatable(controller, actionName, validateable);
    }

    private String qualify(String extra) {
        return dot(qualifyActionValidatable(controller, actionName, validateable), extra);
    }

    private String errorKey(ObjectError error) {
        StringBuilder b = new StringBuilder(error.getClass().getName())
            .append(Arrays.toString(error.getCodes()))
            .append(error.getDefaultMessage());
        if (error instanceof FieldObjectError) {
            b.append(((FieldObjectError) error).getFieldName());
        }
        return hash(b.toString());
    }

    protected String hash(String str) throws IllegalArgumentException {
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("String to encode cannot be null or have zero length");
        }

        return hash(str.getBytes());
    }

    protected String hash(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Byte array to encode cannot be null or have zero length");
        }

        MessageDigest digester = createDigester();
        digester.update(bytes);
        byte[] hash = digester.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            if ((0xff & hash[i]) < 0x10) {
                hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
            } else {
                hexString.append(Integer.toHexString(0xFF & hash[i]));
            }
        }
        return hexString.toString();
    }

    protected MessageDigest createDigester() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
