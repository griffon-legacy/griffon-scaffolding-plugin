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
import griffon.core.GriffonControllerClass;
import griffon.core.controller.GriffonControllerActionManager;
import griffon.core.i18n.NoSuchMessageException;
import griffon.plugins.scaffolding.atoms.*;
import griffon.plugins.scaffolding.atoms.StringValue;
import griffon.plugins.validation.Validateable;
import griffon.plugins.validation.constraints.ConstrainedProperty;
import griffon.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;

import static griffon.util.GriffonExceptionHandler.sanitize;
import static griffon.util.GriffonNameUtils.*;
import static org.codehaus.groovy.runtime.ResourceGroovyMethods.eachLine;

/**
 * @author Andres Almiray
 */
public final class ScaffoldingUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ScaffoldingUtils.class);

    public static final String COMMAND_OBJECT_SUFFIX = "CommandObject";
    public static final String VALIDATABLE_SUFFIX = "Validatable";
    private static final String DEFAULT_APPLICATION_TEMPLATE_PATH = "templates.scaffolding";
    private static final String DEFAULT_TEMPLATE_PATH = "griffon.plugins.scaffolding.templates";
    private static final String KEY_DEFAULT = "default";
    private static final String KEY_UNKNOWN = "Unknown";
    private static final String KEY_TEMPLATE = "Template";

    private static Map<Class, Class> SUPPORTED_ATOM_TYPES = CollectionUtils.<Class, Class>map()
        .e(BigDecimal.class, BigDecimalValue.class)
        .e(BigInteger.class, BigIntegerValue.class)
        .e(Boolean.class, BooleanValue.class)
        .e(Byte.class, ByteValue.class)
        .e(Calendar.class, CalendarValue.class)
        .e(Date.class, DateValue.class)
        .e(Double.class, DoubleValue.class)
        .e(Float.class, FloatValue.class)
        .e(Integer.class, IntegerValue.class)
        .e(Long.class, LongValue.class)
        .e(Short.class, ShortValue.class)
        .e(String.class, StringValue.class)
        .e(Boolean.TYPE, BooleanValue.class)
        .e(Byte.TYPE, ByteValue.class)
        .e(Double.TYPE, DoubleValue.class)
        .e(Float.TYPE, FloatValue.class)
        .e(Integer.TYPE, IntegerValue.class)
        .e(Long.TYPE, LongValue.class)
        .e(Short.TYPE, ShortValue.class);

    public static Map<Class, Class> initializeAtomTypes() {
        Enumeration<URL> urls = null;

        try {
            urls = ApplicationClassLoader.get().getResources("META-INF/services/" + AtomicValue.class.getName());
        } catch (IOException ioe) {
            return SUPPORTED_ATOM_TYPES;
        }

        if (urls == null) return SUPPORTED_ATOM_TYPES;

        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Reading " + AtomicValue.class.getName() + " definitions from " + url);
            }

            try {
                eachLine(url, new RunnableWithArgsClosure(new RunnableWithArgs() {
                    @Override
                    public void run(Object[] args) {
                        String line = (String) args[0];
                        if (line.startsWith("#") || isBlank(line)) return;
                        try {
                            String[] parts = line.trim().split("=");
                            Class targetType = loadClass(parts[0].trim());
                            Class atomicValueClass = loadClass(parts[1].trim());
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Registering " + atomicValueClass.getName() + " as AtomicValue for " + targetType.getName());
                            }
                            SUPPORTED_ATOM_TYPES.put(targetType, atomicValueClass);
                        } catch (Exception e) {
                            if (LOG.isWarnEnabled()) {
                                LOG.warn("Could not load AtomicValue with " + line, sanitize(e));
                            }
                        }
                    }
                }));
            } catch (IOException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Could not load AtomicValue definitions from " + url, sanitize(e));
                }
            }
        }

        return SUPPORTED_ATOM_TYPES;
    }

    private static Class<?> loadClass(String className) throws ClassNotFoundException {
        ClassNotFoundException cnfe = null;

        ClassLoader cl = ScaffoldingUtils.class.getClassLoader();
        try {
            return cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            cnfe = e;
        }

        cl = Thread.currentThread().getContextClassLoader();
        try {
            return cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            cnfe = e;
        }

        if (cnfe != null) throw cnfe;
        return null;
    }

    private ScaffoldingUtils() {

    }

    public static String[] mvcMemberCodes(GriffonController controller, String actionName, Validateable validateable, String suffix) {
        // Given the following values
        //   controller    = com.acme.MailController
        //   actionName    = sendMail
        //   commandObject = com.acme.commands.MailCommandObject

        // mail
        String controllerName = getLogicalPropertyName(controller.getClass().getName(), GriffonControllerClass.TRAILING);
        // sendmail
        String normalizedActionName = normalizeActionName(controller, actionName);
        // com.acme
        String controllerPackageName = controller.getClass().getPackage().getName();
        // MailCommandObject
        String validateableName = capitalize(getLogicalPropertyName(validateable.getClass().getName(), COMMAND_OBJECT_SUFFIX));
        if (validateable instanceof CommandObject) {
            validateableName += COMMAND_OBJECT_SUFFIX;
        }
        // com.acme.commands
        String validateablePackageName = validateable.getClass().getPackage().getName();

        List<String> codes = new ArrayList<String>();
        // com.acme.mail.sendmail.MailCommandObject<suffix> | com.acme.mail.sendmail.Mail<suffix>
        codes.add(dot(controllerPackageName, controllerName, normalizedActionName, validateableName) + suffix);
        // com.acme.mail.MailCommandObject<suffix> | com.acme.mail.Mail<suffix>
        codes.add(dot(controllerPackageName, controllerName, validateableName) + suffix);
        // com.acme.MailCommandObject<suffix> | com.acme.Mail<suffix>
        codes.add(dot(controllerPackageName, validateableName) + suffix);
        // com.acme != com.acme.commands ?
        if (!controllerPackageName.equals(validateablePackageName)) {
            // com.acme.commands.MailCommandObject<suffix> | com.acme.commands.Mail<suffix>
            codes.add(dot(validateablePackageName, validateableName) + suffix);
        }
        // templates.scaffolding.CommandObject<suffix> | templates.scaffolding.Validateable<suffix>
        codes.add(dot(DEFAULT_APPLICATION_TEMPLATE_PATH, validateable instanceof CommandObject ? COMMAND_OBJECT_SUFFIX : VALIDATABLE_SUFFIX) + suffix);
        // griffon.plugins.scaffolding.templates.CommandObject<suffix>
        codes.add(dot(DEFAULT_TEMPLATE_PATH, COMMAND_OBJECT_SUFFIX) + suffix);

        return codes.toArray(new String[codes.size()]);
    }

    public static String[] messageCodes(GriffonController controller, String actionName, Validateable validateable, String property) {
        // Given the following values
        //   controller    = com.acme.MailController
        //   actionName    = sendMail
        //   commandObject = com.acme.commands.MailCommandObject

        String controllerName = controller.getClass().getName();
        // com.acme
        String controllerPackageName = controller.getClass().getPackage().getName();
        // MailCommandObject
        String validateableName = capitalize(getLogicalPropertyName(validateable.getClass().getName(), COMMAND_OBJECT_SUFFIX));
        if (validateable instanceof CommandObject) {
            validateableName += COMMAND_OBJECT_SUFFIX;
        }
        // com.acme.commands
        String validateablePackageName = validateable.getClass().getPackage().getName();

        List<String> codes = new ArrayList<String>();
        // com.acme.MailController.sendMail.MailCommandObject.<property> | com.acme.MailController.sendMail.Mail.<property>
        codes.add(dot(controllerName, actionName, validateableName, property));
        // com.acme.MailController.MailCommandObject.<property> | com.acme.MailController.Mail.<property>
        codes.add(dot(controllerName, validateableName, property));
        // com.acme.MailCommandObject.<property> | com.acme.Mail.<property>
        codes.add(dot(controllerPackageName, validateableName, property));
        // com.acme != com.acme.commands ?
        if (!controllerPackageName.equals(validateablePackageName)) {
            // com.acme.commands.MailCommandObject.<property> | com.acme.commands.Mail.<property>
            codes.add(dot(validateablePackageName, validateableName, property));
        }
        // default.<property>
        codes.add(dot(KEY_DEFAULT, property));

        return codes.toArray(new String[codes.size()]);
    }

    public static String[] propertyTemplates(GriffonController controller, String actionName, Validateable validateable, String property) {
        ConstrainedProperty constrainedProperty = validateable.constrainedProperties().get(property);

        // Given the following values
        //   controller    = com.acme.MailController
        //   actionName    = sendMail
        //   commandObject = com.acme.commands.MailCommandObject

        // mail
        String controllerName = getLogicalPropertyName(controller.getClass().getName(), GriffonControllerClass.TRAILING);
        // sendmail
        String normalizedActionName = normalizeActionName(controller, actionName);
        // com.acme
        String controllerPackageName = controller.getClass().getPackage().getName();
        // mail
        String validateableName = getLogicalPropertyName(validateable.getClass().getName(), COMMAND_OBJECT_SUFFIX);
        // com.acme.commands
        String validateablePackageName = validateable.getClass().getPackage().getName();

        property = capitalize(property);
        String simpleType = constrainedProperty.getPropertyType().getSimpleName();
        if ("int".equals(simpleType)) simpleType = "integer";
        String propertyType = capitalize(getLogicalPropertyName(simpleType, "Value"));

        List<String> templates = new ArrayList<String>();
        // com.acme.mail.sendmail.mail.<property>Template
        templates.add(dot(controllerPackageName, controllerName, normalizedActionName, validateableName, property) + KEY_TEMPLATE);
        // com.acme.mail.sendmail.mail.<propertyType>Template
        templates.add(dot(controllerPackageName, controllerName, normalizedActionName, validateableName, propertyType) + KEY_TEMPLATE);
        // com.acme.mail.mail.<property>Template
        templates.add(dot(controllerPackageName, controllerName, validateableName, property) + KEY_TEMPLATE);
        // com.acme.mail.mail.<propertyType>Template
        templates.add(dot(controllerPackageName, controllerName, validateableName, propertyType) + KEY_TEMPLATE);
        // com.acme.commands.mail.<property>Template
        templates.add(dot(validateablePackageName, validateableName, property) + KEY_TEMPLATE);
        // com.acme.commands.mail.<propertyType>Template
        templates.add(dot(validateablePackageName, validateableName, propertyType) + KEY_TEMPLATE);
        // templates.scaffolding.<property>Template
        templates.add(dot(DEFAULT_APPLICATION_TEMPLATE_PATH, property + KEY_TEMPLATE));
        // templates.scaffolding.<propertyType>Template
        templates.add(dot(DEFAULT_APPLICATION_TEMPLATE_PATH, propertyType + KEY_TEMPLATE));
        // templates.scaffolding.UnknownTemplate
        templates.add(dot(DEFAULT_APPLICATION_TEMPLATE_PATH, KEY_UNKNOWN + KEY_TEMPLATE));
        // griffon.plugins.scaffolding.templates.<propertyType>Template
        templates.add(dot(DEFAULT_TEMPLATE_PATH, propertyType + KEY_TEMPLATE));
        // griffon.plugins.scaffolding.templates.UnknownTemplate
        templates.add(dot(DEFAULT_TEMPLATE_PATH, KEY_UNKNOWN + KEY_TEMPLATE));

        return templates.toArray(new String[templates.size()]);
    }

    public static String[] widgetTemplates(GriffonController controller, String actionName, Validateable validateable, String widget) {
        // Given the following values
        //   controller    = com.acme.MailController
        //   actionName    = sendMail
        //   commandObject = com.acme.commands.MailCommandObject

        // mail
        String controllerName = getLogicalPropertyName(controller.getClass().getName(), GriffonControllerClass.TRAILING);
        // sendmail
        String normalizedActionName = normalizeActionName(controller, actionName);
        // com.acme
        String controllerPackageName = controller.getClass().getPackage().getName();
        // mail
        String validateableName = getLogicalPropertyName(validateable.getClass().getName(), COMMAND_OBJECT_SUFFIX);
        // com.acme.commands
        String validateablePackageName = validateable.getClass().getPackage().getName();

        widget = capitalize(widget);

        List<String> templates = new ArrayList<String>();
        // com.acme.mail.sendmail.mail.<widget>Template
        templates.add(dot(controllerPackageName, controllerName, normalizedActionName, validateableName, widget) + KEY_TEMPLATE);
        // com.acme.mail.mail.<widget>Template
        templates.add(dot(controllerPackageName, controllerName, validateableName, widget) + KEY_TEMPLATE);
        // com.acme.commands.mail.<widget>Template
        templates.add(dot(validateablePackageName, validateableName, widget) + KEY_TEMPLATE);
        // templates.scaffolding.<widget>Template
        templates.add(dot(DEFAULT_APPLICATION_TEMPLATE_PATH, widget + KEY_TEMPLATE));
        // templates.scaffolding.UnknownTemplate
        templates.add(dot(DEFAULT_APPLICATION_TEMPLATE_PATH, KEY_UNKNOWN + KEY_TEMPLATE));
        // griffon.plugins.scaffolding.<widget>Template
        templates.add(dot(DEFAULT_TEMPLATE_PATH, widget + KEY_TEMPLATE));
        // griffon.plugins.scaffolding.UnknownTemplate
        templates.add(dot(DEFAULT_TEMPLATE_PATH, KEY_UNKNOWN + KEY_TEMPLATE));

        return templates.toArray(new String[templates.size()]);
    }

    public static String getNaturalName(CommandObject commandObject) {
        return GriffonNameUtils.getNaturalName(
            getLogicalPropertyName(commandObject.getClass().getName(), COMMAND_OBJECT_SUFFIX));
    }

    public static String resolveMessage(GriffonController controller, String actionName, Validateable validateable, String property, String defaultValue) {
        for (String code : messageCodes(controller, actionName, validateable, property)) {
            try {
                return controller.getApp().getMessage(code);
            } catch (NoSuchMessageException e) {
                // ignore, continue with next code
            }
        }

        return defaultValue;
    }

    public static String qualifyActionName(GriffonController controller, String actionName) {
        return controller.getClass().getName() + "." + actionName;
    }

    public static String qualifyActionValidatable(GriffonController controller, String actionName, Validateable validateable) {
        return qualifyActionName(controller, actionName) + "." + getPropertyName(validateable.getClass());
    }

    public static String normalizeActionName(GriffonController controller, String actionName) {
        actionName = controller.getApp().getActionManager().normalizeName(actionName).toLowerCase();
        if (isKeyword(actionName)) {
            actionName += GriffonControllerActionManager.ACTION.toLowerCase();
        }
        return actionName;
    }

    public static String dot(String... parts) {
        StringBuilder b = new StringBuilder();

        boolean first = true;

        for (String part : parts) {
            if (first) {
                first = false;
            } else {
                b.append(".");
            }
            b.append(part);
        }

        return b.toString();
    }
}
