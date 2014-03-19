package com.leacox.dagger.jersey2;

import com.leacox.dagger.hk2.bridge.api.DaggerBridge;
import com.leacox.dagger.hk2.bridge.api.DaggerIntoHk2Bridge;
import com.leacox.dagger.servlet.DaggerServletContextListener;
import com.leacox.dagger.servlet.internal.ModuleClasses;
import dagger.Module;
import dagger.ObjectGraph;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
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

        //DaggerBridge.getDaggetBridge().initializeDaggerBridge(locator);
        //DaggerIntoHk2Bridge daggerBridge = locator.getService(DaggerIntoHk2Bridge.class);

        objectGraph = (ObjectGraph) servletContext.getAttribute(DaggerServletContextListener.OBJECT_GRAPH_NAME);
        //objectGraph.get(Jersey2Module.ResourceConfigProvider.class).set(config);

        //objectGraph.get(DaggerInjectionRegisterer.class).registerInjections();

        //daggerBridge.bridgeDaggerObjectGraph(objectGraph);

        // ServiceLocatorUtilities.addOneConstant(locator, new DaggerInjectResolver(objectGraph));
    }

    @Override
    public boolean bind(Class<?> component, Set<Class<?>> providerContracts) {
        if (objectGraph == null) {
            return false;
        }

        try {
            objectGraph.get(component);

            DynamicConfiguration dynamicConfig = Injections.getConfiguration(locator);

            ServiceBindingBuilder bindingBuilder = Injections.newFactoryBinder(
                    new DaggerComponentProvider.DaggerManagedBeanFactory(objectGraph, locator, component));
            bindingBuilder.to(component);
            Injections.addBinding(bindingBuilder, dynamicConfig);
            dynamicConfig.commit();

            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void done() {
    }

    private static class DaggerManagedBeanFactory implements Factory {
        private final ObjectGraph objectGraph;
        private final ServiceLocator locator;
        private final Class clazz;

        DaggerManagedBeanFactory(ObjectGraph objectGraph, ServiceLocator locator, Class clazz) {
            this.objectGraph = objectGraph;
            this.locator = locator;
            this.clazz = clazz;
        }

        @Override
        public Object provide() {
            Object object = objectGraph.get(clazz);
            //locator.inject(object);
            return object;
        }

        @Override
        public void dispose(Object instance) {
        }
    }
}
