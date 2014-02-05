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

package com.leacox.dagger.servlet;

import com.leacox.dagger.servlet.internal.ModuleClasses;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * @author John Leacox
 */
@Module(
        injects = {
                ServletContextProvider.class,
                InternalServletModule.ObjectGraphProvider.class,
                InternalServletModule.FullModulesProvider.class,
                InternalServletModule.FilterDefinitionsProvider.class,
                InternalServletModule.ServletDefinitionsProvider.class,
                FilterPipeline.class,
                ManagedFilterPipeline.class,
                ManagedServletPipeline.class,
                DaggerFilter.class
        },
        library = true,
        complete = false
)
class InternalServletModule {
    @Provides
    FilterPipeline providFilterPipeline(ManagedFilterPipeline filterPipeline) {
        return filterPipeline;
    }

    @Provides
    @Singleton
    ManagedServletPipeline provideServletPipeline(ServletDefinition[] servletDefinitions) {
        return new ManagedServletPipeline(servletDefinitions);
    }

    @Provides
    @Singleton
    ObjectGraph provideObjectGraph(ObjectGraphProvider objectGraphProvider) {
        return objectGraphProvider.get();
    }

    @Provides
    @Singleton
    @ModuleClasses
    Object[] provideFullModules(FullModulesProvider fullModulesProvider) {
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
    static class FullModulesProvider implements Provider<Object[]> {
        private Object[] modules;

        @Inject
        FullModulesProvider() {
        }

        void set(Object[] modules) {
            this.modules = modules;
        }

        @Override
        public Object[] get() {
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
        private ServletDefinition[] servletDefinitions = new ServletDefinition[0];

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
