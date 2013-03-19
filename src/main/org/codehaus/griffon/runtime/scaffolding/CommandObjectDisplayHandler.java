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
import griffon.core.controller.MissingControllerActionException;
import griffon.exceptions.MVCGroupConfigurationException;
import griffon.plugins.scaffolding.CommandObject;
import griffon.plugins.scaffolding.ScaffoldingContext;
import griffon.util.CollectionUtils;
import griffon.util.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static griffon.plugins.scaffolding.ScaffoldingUtils.mvcMemberCodes;
import static griffon.plugins.scaffolding.ScaffoldingUtils.qualifyActionValidatable;
import static griffon.util.GriffonNameUtils.capitalize;
import static org.codehaus.griffon.runtime.util.GriffonApplicationHelper.safeLoadClass;
import static org.codehaus.griffon.runtime.util.GriffonApplicationHelper.safeNewInstance;

/**
 * @author Andres Almiray
 */
public class CommandObjectDisplayHandler implements ApplicationHandler {
    private final Logger LOG = LoggerFactory.getLogger(CommandObjectDisplayHandler.class);
    private final GriffonApplication app;
    private final Map<String, ScaffoldingContext> contexts = new ConcurrentHashMap<String, ScaffoldingContext>();

    public CommandObjectDisplayHandler(GriffonApplication app) {
        this.app = app;
    }

    public GriffonApplication getApp() {
        return app;
    }

    public void display(GriffonController controller, String actionName, CommandObject commandObject) {
        MVCGroupConfiguration mvcGroupConfiguration = fetchMVCGroupConfiguration(controller, actionName, commandObject);
        ScaffoldingContext scaffoldingContext = fetchScaffoldingContext(controller, actionName, commandObject);
        MVCGroup mvcGroup = mvcGroupConfiguration.create(CollectionUtils.<String, Object>map()
            .e("scaffoldingContext", scaffoldingContext));
        scaffoldingContext.setBinding(mvcGroup.getBuilder());
        GriffonControllerAction showAction = app.getActionManager().actionFor(mvcGroup.getController(), "show");
        try {
            if (showAction != null) {
                showAction.execute();
                mvcGroup.destroy();
            } else {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Missing action 'show' in controller " + mvcGroupConfiguration.getMembers().get(GriffonControllerClass.TYPE));
                }
                throw new MissingControllerActionException(controller.getClass(), actionName);
            }
        } finally {
            scaffoldingContext.dispose();
        }
    }

    private ScaffoldingContext fetchScaffoldingContext(GriffonController controller, String actionName, CommandObject commandObject) {
        String fqCommandName = qualifyActionValidatable(controller, actionName, commandObject);

        ScaffoldingContext scaffoldingContext = contexts.get(fqCommandName);
        if (scaffoldingContext == null) {
            scaffoldingContext = newScaffoldingContext();
            scaffoldingContext.setActionName(actionName);
            contexts.put(fqCommandName, scaffoldingContext);
        }
        scaffoldingContext.setController(controller);
        scaffoldingContext.setValidateable(commandObject);

        return scaffoldingContext;
    }

    private ScaffoldingContext newScaffoldingContext() {
        String toolkitName = capitalize(Metadata.getCurrent().getApplicationToolkit());
        String className = ScaffoldingContext.class.getPackage().getName() + "." + toolkitName + ScaffoldingContext.class.getSimpleName();
        Class contextClass = safeLoadClass(className);
        return (ScaffoldingContext) safeNewInstance(contextClass);
    }

    private MVCGroupConfiguration fetchMVCGroupConfiguration(GriffonController controller, String actionName, CommandObject commandObject) {
        String fqCommandName = qualifyActionValidatable(controller, actionName, commandObject);

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

        String fqCommandName = qualifyActionValidatable(controller, actionName, commandObject);
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
            LOG.debug("  Resolving " + suffix + " member for " + qualifyActionValidatable(controller, actionName, commandObject));
        }

        for (String code : mvcMemberCodes(controller, actionName, commandObject, suffix)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("    Resolving template: " + code);
            }
            Class memberClass = safeLoadClass(code);
            if (memberClass != null) return memberClass.getName();
        }

        if (LOG.isWarnEnabled()) {
            LOG.warn("  Could not resolve " + suffix + " member for " + qualifyActionValidatable(controller, actionName, commandObject));
        }

        throw new IllegalArgumentException("Could not resolve " + suffix + " member for " + qualifyActionValidatable(controller, actionName, commandObject));
    }
}
