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

import griffon.plugins.scaffolding.CommandObjectUtils;
import griffon.util.ApplicationHolder;
import griffon.util.CollectionUtils;
import org.codehaus.griffon.runtime.core.AbstractGriffonAddon;
import org.codehaus.griffon.runtime.scaffolding.ScaffoldingGriffonControllerActionInterceptor;

/**
 * @author Andres Almiray
 */
public class ScaffoldingGriffonAddon extends AbstractGriffonAddon {
    public ScaffoldingGriffonAddon() {
        super(ApplicationHolder.getApplication());

        actionInterceptors.put(
            "scaffolding",
            CollectionUtils.<String, Object>map()
                .e("interceptor", ScaffoldingGriffonControllerActionInterceptor.class.getName())
        );

        CommandObjectUtils.initializeAtomTypes();
    }
}
