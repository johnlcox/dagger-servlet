/**
 * Copyright (C) 2014 John Leacox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leacox.dagger.jersey;

import com.google.common.collect.Sets;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProvider;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import com.sun.jersey.core.spi.component.ioc.IoCInstantiatedComponentProvider;
import com.sun.jersey.core.spi.component.ioc.IoCProxiedComponentProvider;
import dagger.Module;
import dagger.ObjectGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Set;

/**
 * A Dagger-based {@link IoCComponentProviderFactory}.
 *
 * @author John Leacox
 */
class DaggerComponentProviderFactory implements IoCComponentProviderFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DaggerComponentProviderFactory.class);

    private final ObjectGraph objectGraph;
    private final Set<Class<?>> daggerInjectableClasses = Sets.newHashSet();

    public DaggerComponentProviderFactory(ResourceConfig config, ObjectGraph objectGraph, Object[] modules) {
        this.objectGraph = objectGraph;

        for (Object module : modules) {
            Module annotation;
            if (module instanceof Class<?>) {
                annotation = ((Class<?>) module).getAnnotation(Module.class);
            } else {
                annotation = module.getClass().getAnnotation(Module.class);
            }
            register(config, annotation);
        }
    }

    @Override
    public IoCComponentProvider getComponentProvider(Class<?> clazz) {
        return getComponentProvider(null, clazz);
    }

    @Override
    public IoCComponentProvider getComponentProvider(ComponentContext componentContext, Class<?> clazz) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("getComponentProvider({0}", clazz.getName());
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
                LOGGER.info("Registering {0} as a provider class", clazz.getName());
                config.getClasses().add(clazz);
            } else if (ResourceConfig.isRootResourceClass(clazz)) {
                LOGGER.info("Registering {0} as a root resource class", clazz.getName());
                config.getClasses().add(clazz);
            }

            daggerInjectableClasses.add(clazz);
        }

        for (Class<?> clazz : annotation.includes()) {
            register(config, clazz.getAnnotation(Module.class));
        }
    }

    private boolean isDaggerConstructorInjected(Class<?> clazz) {
        if (daggerInjectableClasses.contains(clazz)) {
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                if (isInjectable(constructor)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isDaggerFieldInjected(Class<?> clazz) {
        if (daggerInjectableClasses.contains(clazz)) {
            for (Field field : clazz.getDeclaredFields()) {
                if (isInjectable(field)) {
                    return true;
                }
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
