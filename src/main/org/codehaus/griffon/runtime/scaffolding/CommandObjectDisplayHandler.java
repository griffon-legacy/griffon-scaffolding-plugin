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

package org.codehaus.griffon.runtime.scaffolding;

import griffon.core.*;
import griffon.core.controller.GriffonControllerAction;
import griffon.core.controller.GriffonControllerActionManager;
import griffon.core.controller.MissingControllerActionException;
import griffon.exceptions.MVCGroupConfigurationException;
import griffon.plugins.scaffolding.CommandObject;
import griffon.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static griffon.util.GriffonNameUtils.*;
import static org.codehaus.griffon.runtime.util.GriffonApplicationHelper.safeLoadClass;

/**
 * @author Andres Almiray
 */
public class CommandObjectDisplayHandler implements ApplicationHandler {
    private final Logger LOG = LoggerFactory.getLogger(CommandObjectDisplayHandler.class);
    private final GriffonApplication app;
    private static final String DEFAULT_TEMPLATE_NAME = "griffon.plugins.scaffolding.CommandObject";
    private static final String COMMAND_OBJECT_SUFFIX = "CommandObject";

    public CommandObjectDisplayHandler(GriffonApplication app) {
        this.app = app;
    }

    public GriffonApplication getApp() {
        return app;
    }

    public void display(GriffonController controller, String actionName, CommandObject commandObject) {
        MVCGroupConfiguration mvcGroupConfiguration = fetchMVCGroupConfiguration(controller, actionName, commandObject);
        MVCGroup mvcGroup = mvcGroupConfiguration.create(CollectionUtils.<String, Object>map()
            .e("commandObject", commandObject));
        GriffonControllerAction showAction = app.getActionManager().actionFor(mvcGroup.getController(), "show");
        if (showAction != null) {
            showAction.execute();
            mvcGroup.destroy();
        } else {
            if (LOG.isErrorEnabled()) {
                LOG.error("Missing action 'show' in controller " + mvcGroupConfiguration.getMembers().get(GriffonControllerClass.TYPE));
            }
            throw new MissingControllerActionException(controller.getClass(), actionName);
        }
    }

    private MVCGroupConfiguration fetchMVCGroupConfiguration(GriffonController controller, String actionName, CommandObject commandObject) {
        String fqCommandName = qualifyCommandObject(controller, actionName, commandObject);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching MVCGroupConfiguration for " + fqCommandName);
        }

        MVCGroupConfiguration mvcGroupConfiguration = null;
        try {
            mvcGroupConfiguration = app.getMvcGroupManager().findConfiguration(fqCommandName);
        } catch (MVCGroupConfigurationException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Resolving MVCGroupConfiguration for " + fqCommandName);
            }
            mvcGroupConfiguration = resolveMVCGroupConfiguration(controller, actionName, commandObject);
        }
        return mvcGroupConfiguration;
    }

    private MVCGroupConfiguration resolveMVCGroupConfiguration(GriffonController controller, String actionName, CommandObject commandObject) {
        String mtemplate = resolveMember(controller, actionName, commandObject, GriffonModelClass.TRAILING);
        String vtemplate = resolveMember(controller, actionName, commandObject, GriffonViewClass.TRAILING);
        String ctemplate = resolveMember(controller, actionName, commandObject, GriffonControllerClass.TRAILING);

        String fqCommandName = qualifyCommandObject(controller, actionName, commandObject);
        MVCGroupConfiguration mvcGroupConfiguration = app.getMvcGroupManager().newMVCGroupConfiguration(fqCommandName,
            CollectionUtils.<String, String>map()
                .e(GriffonModelClass.TYPE, mtemplate)
                .e(GriffonViewClass.TYPE, vtemplate)
                .e(GriffonControllerClass.TYPE, ctemplate),
            Collections.<String, Object>emptyMap());
        app.getMvcGroupManager().addConfiguration(mvcGroupConfiguration);
        return mvcGroupConfiguration;
    }

    private String resolveMember(GriffonController controller, String actionName, CommandObject commandObject, String suffix) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("  Resolving " + suffix + " member for " + qualifyCommandObject(controller, actionName, commandObject));
        }

        // Given the following values
        //   controller    = com.acme.MailController
        //   actionName    = sendMail
        //   commandObject = com.acme.commands.MailCommandObject

        // mail
        String controllerName = getLogicalPropertyName(controller.getClass().getName(), GriffonControllerClass.TRAILING);
        // sendmail
        String normalizedActionName = normalizeActionName(actionName);
        // com.acme
        String controllerPackageName = controller.getClass().getPackage().getName();
        // Mail
        String commandObjectName = capitalize(getLogicalPropertyName(commandObject.getClass().getName(), COMMAND_OBJECT_SUFFIX) + COMMAND_OBJECT_SUFFIX);
        // com.acme.commands
        String commandObjectPackageName = commandObject.getClass().getPackage().getName();

        // com.acme.mail.sendmail.MailCommandObject<suffix>
        String templateName = qualify(controllerPackageName, controllerName, normalizedActionName, commandObjectName, suffix);
        if (LOG.isDebugEnabled()) {
            LOG.debug("    Resolving template: " + templateName);
        }
        Class memberClass = safeLoadClass(templateName);
        if (memberClass != null) return memberClass.getName();

        // com.acme.mail.MailCommandObject<suffix>
        templateName = qualify(controllerPackageName, controllerName, commandObjectName, suffix);
        if (LOG.isDebugEnabled()) {
            LOG.debug("    Resolving template: " + templateName);
        }
        memberClass = safeLoadClass(templateName);
        if (memberClass != null) return memberClass.getName();

        // com.acme.MailCommandObject<suffix>
        templateName = qualify(controllerPackageName, commandObjectName + COMMAND_OBJECT_SUFFIX, suffix);
        if (LOG.isDebugEnabled()) {
            LOG.debug("    Resolving template: " + templateName);
        }
        memberClass = safeLoadClass(templateName);
        if (memberClass != null) return memberClass.getName();

        // com.acme != com.acme.commands ?
        if (!controllerPackageName.equals(commandObjectPackageName)) {
            // com.acme.commands.MailCommandObject<suffix>
            templateName = qualify(commandObjectPackageName, commandObjectName + COMMAND_OBJECT_SUFFIX, suffix);
            if (LOG.isDebugEnabled()) {
                LOG.debug("    Resolving template: " + templateName);
            }
            memberClass = safeLoadClass(templateName);
            if (memberClass != null) return memberClass.getName();
        }

        templateName = DEFAULT_TEMPLATE_NAME + suffix;
        if (LOG.isDebugEnabled()) {
            LOG.debug("    Falling back to default template: " + templateName);
        }

        return templateName;
    }

    private String qualifyActionName(GriffonController controller, String actionName) {
        return controller.getClass().getName() + "." + actionName;
    }

    private String qualifyCommandObject(GriffonController controller, String actionName, CommandObject commandObject) {
        return qualifyActionName(controller, actionName) + "." + getPropertyName(commandObject.getClass());
    }

    private String normalizeActionName(String actionName) {
        actionName = app.getActionManager().normalizeName(actionName).toLowerCase();
        if (isKeyword(actionName)) {
            actionName += GriffonControllerActionManager.ACTION.toLowerCase();
        }
        return actionName;
    }

    private String qualify(String... parts) {
        StringBuilder b = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i != 0 && i < parts.length - 1) {
                b.append(".");
            }
            b.append(parts[i]);
        }

        return b.toString();
    }
}
