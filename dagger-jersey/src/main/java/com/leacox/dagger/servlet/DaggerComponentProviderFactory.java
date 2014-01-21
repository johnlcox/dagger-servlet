package com.leacox.dagger.servlet;

import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProvider;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import com.sun.jersey.core.spi.component.ioc.IoCInstantiatedComponentProvider;
import com.sun.jersey.core.spi.component.ioc.IoCProxiedComponentProvider;
import dagger.Module;
import dagger.ObjectGraph;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author John Leacox
 */
public class DaggerComponentProviderFactory implements IoCComponentProviderFactory {
    private static final Logger LOGGER = Logger.getLogger(DaggerComponentProviderFactory.class.getName());

    private final ObjectGraph objectGraph;

    public DaggerComponentProviderFactory(ResourceConfig config, ObjectGraph objectGraph, JerseyModule jerseyModule) {
        this.objectGraph = objectGraph;

        Module annotation = jerseyModule.getClass().getAnnotation(Module.class);
        register(config, annotation);
    }

    @Override
    public IoCComponentProvider getComponentProvider(Class<?> clazz) {
        return getComponentProvider(null, clazz);
    }

    @Override
    public IoCComponentProvider getComponentProvider(ComponentContext componentContext, Class<?> clazz) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "getComponentProvider({0}", clazz.getName());
        }

        if (isDaggerConstructorInjected(clazz)) {
            return new DaggerInstantiatedComponentProvider(objectGraph, clazz);
        } else if (isDaggerFieldInjected(clazz)) {
            return new DaggerInjectedComponentProvider(objectGraph);
        } else {
            return null;
        }
    }

    private void register(ResourceConfig config, Module annotation) {
        if (annotation == null) {
            throw new IllegalStateException("All dagger modules must be annotated with @Module");
        }

        for (Class<?> clazz : annotation.injects()) {
            if (ResourceConfig.isProviderClass(clazz)) {
                LOGGER.log(Level.INFO, "Registering {0} as a provider class", clazz.getName());
                config.getClasses().add(clazz);
            } else if (ResourceConfig.isRootResourceClass(clazz)) {
                LOGGER.log(Level.INFO, "Registering {0} as a root resource class", clazz.getName());
                config.getClasses().add(clazz);
            }
        }

        for (Class<?> clazz : annotation.includes()) {
            register(config, clazz.getAnnotation(Module.class));
        }
    }

    private boolean isDaggerConstructorInjected(Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (isInjectable(constructor)) {
                return true;
            }
        }

        return false;
    }

    private boolean isDaggerFieldInjected(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (isInjectable(field)) {
                return true;
            }
        }

        return !clazz.equals(Object.class) && isDaggerFieldInjected(clazz.getSuperclass());
    }

    private static boolean isInjectable(AnnotatedElement element) {
        return element.isAnnotationPresent(javax.inject.Inject.class);
    }

    private static class DaggerInstantiatedComponentProvider implements IoCInstantiatedComponentProvider {
        private final ObjectGraph objectGraph;
        private final Class<?> clazz;

        public DaggerInstantiatedComponentProvider(ObjectGraph objectGraph, Class<?> clazz) {
            this.objectGraph = objectGraph;
            this.clazz = clazz;
        }

        @Override
        public Object getInjectableInstance(Object o) {
            return o;
        }

        @Override
        public Object getInstance() {
            return objectGraph.get(clazz);
        }
    }

    private static class DaggerInjectedComponentProvider implements IoCProxiedComponentProvider {
        private final ObjectGraph objectGraph;

        public DaggerInjectedComponentProvider(ObjectGraph objectGraph) {
            this.objectGraph = objectGraph;
        }


        @Override
        public Object getInstance() {
            throw new IllegalStateException();
        }

        @Override
        public Object proxy(Object o) {
            return objectGraph.inject(o);
        }
    }
}
