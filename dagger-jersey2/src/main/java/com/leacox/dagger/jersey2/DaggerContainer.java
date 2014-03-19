package com.leacox.dagger.jersey2;

import com.google.common.collect.Sets;
import com.leacox.dagger.servlet.internal.ModuleClasses;
import dagger.Module;
import dagger.ObjectGraph;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.inject.Inject;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author John Leacox
 */
public class DaggerContainer extends ServletContainer {
    private static final Logger LOGGER = Logger.getLogger(DaggerContainer.class.getName());

    private final ObjectGraph objectGraph;
    private final Object[] modules;
    private final Set<Class<?>> daggerInjectableClass = Sets.newHashSet();

    @Inject
    public DaggerContainer(ObjectGraph objectGraph, @ModuleClasses Object[] modules,
                           ResourceConfig resourceConfig) {
        super(resourceConfig);

        this.objectGraph = objectGraph;
        this.modules = modules;

        initialize(resourceConfig);

        resourceConfig.register(DaggerComponentProvider.class);
    }

    private void initialize(ResourceConfig config) {
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

    private void register(ResourceConfig config, Module annotation) {
        if (annotation == null) {
            throw new IllegalStateException("All dagger modules must be annotated with @Module");
        }

        for (Class<?> clazz : annotation.injects()) {
            config.register(clazz);
//            if (ResourceConfig.isProviderClass(clazz)) {
//                LOGGER.log(Level.INFO, "Registering {0} as a provider class", clazz.getName());
//                config.getClasses().add(clazz);
//            } else if (ResourceConfig.isRootResourceClass(clazz)) {
//                LOGGER.log(Level.INFO, "Registering {0} as a root resource class", clazz.getName());
//                config.getClasses().add(clazz);
//            }

            daggerInjectableClass.add(clazz);
        }

        for (Class<?> clazz : annotation.includes()) {
            register(config, clazz.getAnnotation(Module.class));
        }
    }
}
