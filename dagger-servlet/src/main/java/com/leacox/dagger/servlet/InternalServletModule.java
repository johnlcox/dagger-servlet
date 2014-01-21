package com.leacox.dagger.servlet;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.ServletContext;

/**
 * @author John Leacox
 */
@Module(
        injects = {
                InternalServletModule.ServletContextProvider.class,
                InternalServletModule.ObjectGraphProvider.class,
                FilterPipeline.class,
                DaggerFilterPipeline.class,
                //ServletPipeline.class,
                DaggerServletPipeline.class,
                DaggerFilter.class
        }
        //library = true
)
class InternalServletModule {
    @Provides
    FilterPipeline providFilterPipeline(DaggerFilterPipeline filterPipeline) {
        return filterPipeline;
    }

//    @Provides
//    ServletPipeline provideServletPipeline(DaggerServletPipeline servletPipeline) {
//        return servletPipeline;
//    }

    @Provides
    DaggerFilter provideDaggerFilter(FilterPipeline filterPipeline) {
        return new DaggerFilter(filterPipeline);
    }

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
    static public class ObjectGraphProvider implements Provider<ObjectGraph> {
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
}
