package com.leacox.dagger.servlet;

import com.leacox.dagger.servlet.DaggerFilter;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import java.util.logging.Filter;

/**
 * @author John Leacox
 */
@Module(
        injects = {
                InternalServletModule.ServletContextProvider.class,
                InternalServletModule.ObjectGraphProvider.class,
                InternalServletModule.FullModulesProvider.class,
                InternalServletModule.FilterDefinitionsProvider.class,
                InternalServletModule.ServletDefinitionsProvider.class,
                FilterPipeline.class,
                ManagedFilterPipeline.class,
                ManagedServletPipeline.class,
                DaggerFilter.class
        },
        library = true
)
class InternalServletModule {
    @Provides
    FilterPipeline providFilterPipeline(DaggerFilterPipeline filterPipeline) {
        return filterPipeline;
    }

    @Provides
    ServletPipeline provideServletPipeline(DaggerServletPipeline servletPipeline) {
        return servletPipeline;
    }

//    @Provides
//    DaggerFilter provideDaggerFilter(FilterPipeline injectedPipeline) {
//        return new DaggerFilter(injectedPipeline);
//    }

    @Provides
    @Singleton
    ServletContext provideServletContext(ServletContextProvider servletContextProvider) {
        return servletContextProvider.get();
    }

    @Provides
    @Singleton
    ObjectGraph provideObjectGraph(ObjectGraphProvider objectGraphProvider) {
        return objectGraphProvider.get();
    }

    @Provides
    @Singleton
    @ModuleClasses
    Class<?>[] provideFullModules(FullModulesProvider fullModulesProvider) {
        return fullModulesProvider.get();
    }

    @Provides
    @Singleton
    FilterDefinition[] provideFilterDefinitions(FilterDefinitionsProvider filterDefinitionsProvider) {
        return filterDefinitionsProvider.get();
    }

    @Provides
    @Singleton
    ServletDefinition[] provideServletDefinitions(ServletDefinitionsProvider servletDefinitionsProvider) {
        return servletDefinitionsProvider.get();
    }

    @Singleton
    static class ServletContextProvider implements Provider<ServletContext> {
        private ServletContext servletContext;

        @Inject
        ServletContextProvider() {
        }

        void set(ServletContext servletContext) {
            this.servletContext = servletContext;
        }

        @Override
        public ServletContext get() {
            return servletContext;
        }
    }

    @Singleton
    static class ObjectGraphProvider implements Provider<ObjectGraph> {
        private ObjectGraph objectGraph;

        @Inject
        ObjectGraphProvider() {
        }

        void set(ObjectGraph objectGraph) {
            this.objectGraph = objectGraph;
        }

        @Override
        public ObjectGraph get() {
            return objectGraph;
        }
    }

    @Singleton
    static class FullModulesProvider implements Provider<Class<?>[]> {
        private Class<?>[] modules;

        @Inject
        FullModulesProvider() {
        }

        void set(Class<?>[] modules) {
            this.modules = modules;
        }

        @Override
        public Class<?>[] get() {
            return modules;
        }
    }

    @Singleton
    static class FilterDefinitionsProvider implements Provider<FilterDefinition[]> {
        private FilterDefinition[] filterDefinitions;

        @Inject
        FilterDefinitionsProvider() {
        }

        void set(FilterDefinition[] filterDefinitions) {
            this.filterDefinitions = filterDefinitions;
        }

        @Override
        public FilterDefinition[] get() {
            return filterDefinitions;
        }
    }

    @Singleton
    static class ServletDefinitionsProvider implements Provider<ServletDefinition[]> {
        private ServletDefinition[] servletDefinitions;

        @Inject
        ServletDefinitionsProvider() {
        }

        void set(ServletDefinition[] servletDefinitions) {
            this.servletDefinitions = servletDefinitions;
        }

        @Override
        public ServletDefinition[] get() {
            return servletDefinitions;
        }
    }
}
