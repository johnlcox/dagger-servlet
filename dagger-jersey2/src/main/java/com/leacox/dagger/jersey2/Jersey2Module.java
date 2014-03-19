package com.leacox.dagger.jersey2;

import com.leacox.dagger.servlet.ServletModule;
import dagger.Module;
import dagger.Provides;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * @author John Leacox
 */
@Module(
        injects = {
                DaggerComponentProvider.class,
                DaggerInjectionRegisterer.class,
                Jersey2Module.ResourceConfigProvider.class
        },
        includes = {
                ServletModule.class
        },
        library = true
)
public class Jersey2Module {
    @Provides
    @Singleton
    ResourceConfig provideResourceConfig(ResourceConfigProvider resourceConfigProvider) {
        return resourceConfigProvider.get();
    }

    @Singleton
    static class ResourceConfigProvider implements Provider<ResourceConfig> {
        private ResourceConfig config;

        @Inject
        ResourceConfigProvider() {
        }

        void set(ResourceConfig config) {
            this.config = config;
        }

        @Override
        public ResourceConfig get() {
            return config;
        }
    }
}
