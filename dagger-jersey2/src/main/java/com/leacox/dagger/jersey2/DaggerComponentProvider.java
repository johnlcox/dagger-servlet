package com.leacox.dagger.jersey2;

import com.leacox.dagger.hk2.bridge.api.DaggerBridge;
import com.leacox.dagger.hk2.bridge.api.DaggerIntoHk2Bridge;
import com.leacox.dagger.servlet.DaggerServletContextListener;
import dagger.ObjectGraph;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.spi.ComponentProvider;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import java.util.Set;

/**
 * @author John Leacox
 */
public class DaggerComponentProvider implements ComponentProvider {

    private volatile ServiceLocator locator;
    private volatile ObjectGraph objectGraph;

    @Override
    public void initialize(ServiceLocator locator) {
        this.locator = locator;

        ServletContext servletContext = locator.getService(ServletContext.class);

        DaggerBridge.getDaggetBridge().initializeDaggerBridge(locator);
        DaggerIntoHk2Bridge daggerBridge = locator.getService(DaggerIntoHk2Bridge.class);

        objectGraph = (ObjectGraph) servletContext.getAttribute(DaggerServletContextListener.OBJECT_GRAPH_NAME);
        daggerBridge.bridgeDaggerObjectGraph(objectGraph);

        ServiceLocatorUtilities.addOneConstant(locator, new DaggerInjectResolver(objectGraph));
    }

    @Override
    public boolean bind(Class<?> component, Set<Class<?>> providerContracts) {
        if (objectGraph == null) {
            return false;
        }

        // TODO: This conditional isn't right
        if (component.isAnnotationPresent(Inject.class)) {
            DynamicConfiguration config = Injections.getConfiguration(locator);

        }

        return false;
    }

    @Override
    public void done() {

    }
}
