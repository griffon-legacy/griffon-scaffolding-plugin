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
import griffon.util.GriffonNameUtils;

import java.util.ArrayList;
import java.util.List;

import static griffon.util.GriffonNameUtils.*;

/**
 * @author Andres Almiray
 */
public final class CommandObjectUtils {
    public static final String COMMAND_OBJECT_SUFFIX = "CommandObject";
    private static final String DEFAULT_COMMAND_OBJECT_TEMPLATE_PATH = "templates.scaffolding";
    private static final String KEY_DEFAULT = "default";

    private CommandObjectUtils() {

    }

    public static String[] mvcMemberCodes(GriffonController controller, String actionName, CommandObject commandObject, String suffix) {
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
        String commandObjectName = capitalize(getLogicalPropertyName(commandObject.getClass().getName(), COMMAND_OBJECT_SUFFIX) + COMMAND_OBJECT_SUFFIX);
        // com.acme.commands
        String commandObjectPackageName = commandObject.getClass().getPackage().getName();

        List<String> codes = new ArrayList<String>();
        // com.acme.mail.sendmail.MailCommandObject<suffix>
        codes.add(dot(controllerPackageName, controllerName, normalizedActionName, commandObjectName) + suffix);
        // com.acme.mail.MailCommandObject<suffix>
        codes.add(dot(controllerPackageName, controllerName, commandObjectName) + suffix);
        // com.acme.MailCommandObject<suffix>
        codes.add(dot(controllerPackageName, commandObjectName) + suffix);
        // com.acme != com.acme.commands ?
        if (!controllerPackageName.equals(commandObjectPackageName)) {
            // com.acme.commands.MailCommandObject<suffix>
            codes.add(dot(commandObjectPackageName, commandObjectName) + suffix);
        }
        codes.add(dot(DEFAULT_COMMAND_OBJECT_TEMPLATE_PATH, COMMAND_OBJECT_SUFFIX) + suffix);

        return codes.toArray(new String[codes.size()]);
    }

    public static String[] messageCodes(GriffonController controller, String actionName, CommandObject commandObject, String property) {
        // Given the following values
        //   controller    = com.acme.MailController
        //   actionName    = sendMail
        //   commandObject = com.acme.commands.MailCommandObject

        String controllerName = controller.getClass().getName();
        // com.acme
        String controllerPackageName = controller.getClass().getPackage().getName();
        // MailCommandObject
        String commandObjectName = capitalize(getLogicalPropertyName(commandObject.getClass().getName(), COMMAND_OBJECT_SUFFIX) + COMMAND_OBJECT_SUFFIX);
        // com.acme.commands
        String commandObjectPackageName = commandObject.getClass().getPackage().getName();

        List<String> codes = new ArrayList<String>();
        // com.acme.MailController.sendMail.MailCommandObject.<property>
        codes.add(dot(controllerName, actionName, commandObjectName, property));
        // com.acme.MailController.MailCommandObject.<property>
        codes.add(dot(controllerName, commandObjectName, property));
        // com.acme.MailCommandObject.<property>
        codes.add(dot(controllerPackageName, commandObjectName, property));
        // com.acme != com.acme.commands ?
        if (!controllerPackageName.equals(commandObjectPackageName)) {
            // com.acme.commands.MailCommandObject.<property>
            codes.add(dot(commandObjectPackageName, commandObjectName, property));
        }
        // default.<property>
        codes.add(dot(KEY_DEFAULT, property));

        return codes.toArray(new String[codes.size()]);
    }

    public static String getNaturalName(CommandObject commandObject) {
        return GriffonNameUtils.getNaturalName(
            getLogicalPropertyName(commandObject.getClass().getName(), COMMAND_OBJECT_SUFFIX));
    }

    public static String resolveMessage(GriffonController controller, String actionName, CommandObject commandObject, String property, String defaultValue) {
        for (String code : messageCodes(controller, actionName, commandObject, property)) {
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

    public static String qualifyCommandObject(GriffonController controller, String actionName, CommandObject commandObject) {
        return qualifyActionName(controller, actionName) + "." + getPropertyName(commandObject.getClass());
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
