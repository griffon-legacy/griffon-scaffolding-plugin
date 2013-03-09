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

import griffon.core.GriffonApplication;
import griffon.core.GriffonController;
import griffon.core.controller.MissingControllerActionException;
import griffon.plugins.scaffolding.CommandObject;
import griffon.util.GriffonClassUtils;
import groovy.lang.Closure;
import org.codehaus.griffon.runtime.core.controller.AbstractGriffonControllerActionInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static griffon.util.GriffonExceptionHandler.sanitize;
import static org.codehaus.griffon.runtime.util.GriffonApplicationHelper.safeNewInstance;

/**
 * @author Andres Almiray
 */
public class ScaffoldingGriffonControllerActionInterceptor extends AbstractGriffonControllerActionInterceptor {
    private final Logger LOG = LoggerFactory.getLogger(ScaffoldingGriffonControllerActionInterceptor.class);
    private final Map<String, Class> commandObjectMappings = new ConcurrentHashMap<String, Class>();
    private CommandObjectDisplayHandler commandObjectDisplayHandler;

    @Override
    public void setApp(GriffonApplication app) {
        super.setApp(app);
        commandObjectDisplayHandler = new CommandObjectDisplayHandler(app);
    }

    public void configure(GriffonController controller, String actionName, Method method) {
        configureAction(controller, actionName, method.getParameterTypes());
    }

    public void configure(GriffonController controller, String actionName, Field field) {
        try {
            Closure closure = (Closure) GriffonClassUtils.getProperty(controller, actionName);
            configureAction(controller, actionName, closure.getParameterTypes());
        } catch (IllegalAccessException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("An error occurred while configuring action " + qualifyActionName(controller, actionName), sanitize(e));
            }
        } catch (NoSuchMethodException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("An error occurred while configuring action " + qualifyActionName(controller, actionName), sanitize(e));
            }
        } catch (InvocationTargetException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("An error occurred while configuring action " + qualifyActionName(controller, actionName), sanitize(e));
            }
        }
    }

    public Object[] before(GriffonController controller, String actionName, Object[] args) {
        String fqActionName = qualifyActionName(controller, actionName);
        Class commandObjectClass = commandObjectMappings.get(fqActionName);
        if (commandObjectClass != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Instantiating command object of type " + commandObjectClass.getName() + " for action " + fqActionName);
            }
            CommandObject commandObject = (CommandObject) safeNewInstance(commandObjectClass);
            try {
                commandObjectDisplayHandler.display(controller, actionName, commandObject);
            } catch (MissingControllerActionException mcae) {
                throw abortActionExecution();
            }
            if (commandObject.getErrors().hasErrors()) {
                throw abortActionExecution();
            }
            args = new Object[]{commandObject};
        }
        return args;
    }

    // ===================================================

    private void configureAction(GriffonController controller, String actionName, Class[] parameterTypes) {
        String fqActionName = qualifyActionName(controller, actionName);
        if (parameterTypes != null && parameterTypes.length == 1 && CommandObject.class.isAssignableFrom(parameterTypes[0])) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Action " + fqActionName + " requires a command object of type " + parameterTypes[0].getName());
            }
            commandObjectMappings.put(fqActionName, parameterTypes[0]);
        }
    }
}
