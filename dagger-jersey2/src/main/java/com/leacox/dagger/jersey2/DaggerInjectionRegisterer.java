package com.leacox.dagger.jersey2;

import com.leacox.dagger.servlet.internal.ModuleClasses;
import dagger.Module;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Inject;

/**
 * @author John Leacox
 */
class DaggerInjectionRegisterer {
    private final Object[] modules;
    private final ResourceConfig config;

    @Inject
    DaggerInjectionRegisterer(@ModuleClasses Object[] modules, ResourceConfig config) {
        this.modules = modules;
        this.config = config;
    }

    public void registerInjections() {
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
        }

        for (Class<?> clazz : annotation.includes()) {
            register(config, clazz.getAnnotation(Module.class));
        }
    }
}
