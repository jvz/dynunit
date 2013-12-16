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
