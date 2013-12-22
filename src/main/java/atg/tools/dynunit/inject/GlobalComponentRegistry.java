/*
 * Copyright 2013 Matt Sicker and Contributors
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

package atg.tools.dynunit.inject;

import atg.nucleus.ComponentEvent;
import atg.nucleus.ComponentListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author msicker
 * @version 1.0.0
 */
public class GlobalComponentRegistry
        implements ComponentListener {

    private static final Logger logger = LogManager.getLogger();
    private final ConcurrentMap<Class<?>, Object> globalRegistry;

    public GlobalComponentRegistry() {
        globalRegistry = new ConcurrentHashMap<Class<?>, Object>();
    }

    @Override
    public void componentActivated(final ComponentEvent componentEvent) {
        logger.entry(componentEvent);
        final Object component = componentEvent.getComponent();
        final Class<?> klass = component.getClass();
        globalRegistry.putIfAbsent(klass, component);
        logger.exit();
    }

    @Override
    public void componentDeactivated(final ComponentEvent componentEvent) {
        logger.entry(componentEvent);
        final Object component = componentEvent.getComponent();
        globalRegistry.remove(component.getClass());
        logger.exit();
    }

    public Object getComponentForClass(final Class<?> componentClass) {
        return globalRegistry.get(componentClass);
    }

    public boolean isComponentRegistered(final Class<?> componentClass) {
        return globalRegistry.containsKey(componentClass);
    }
}
