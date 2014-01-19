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

import com.leacox.dagger.servlet.ServletModule;
import com.leacox.dagger.servlet.internal.ModuleClasses;
import com.sun.jersey.api.core.ResourceContext;
import com.sun.jersey.core.util.FeaturesAndProperties;
import com.sun.jersey.spi.MessageBodyWorkers;
import com.sun.jersey.spi.container.ExceptionMapperContext;
import com.sun.jersey.spi.container.WebApplication;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import javax.inject.Singleton;
import javax.ws.rs.ext.Providers;

/**
 * A Dagger module that provides application wide JAX-RS and Jersey related bindings. In addition to the internal
 * bindings the following bindings are provided:
 * <ul>
 * <li>{@link com.sun.jersey.spi.container.WebApplication}</li>
 * <li>{@link javax.ws.rs.ext.Providers}</li>
 * <li>{@link com.sun.jersey.core.util.FeaturesAndProperties}</li>
 * <li>{@link com.sun.jersey.spi.MessageBodyWorkers}</li>
 * <li>{@link com.sun.jersey.spi.container.ExceptionMapperContext}</li>
 * <li>{@link com.sun.jersey.api.core.ResourceContext}</li>
 * </ul>
 *
 * @author John Leacox
 */
@Module(
        injects = {
                DaggerContainer.class
        },
        includes = {
                ServletModule.class
        },
        library = true
)
public class JerseyModule {
    @Provides
    @Singleton
    public DaggerContainer provideDaggerContainer(ObjectGraph objectGraph, @ModuleClasses Object[] modules) {
        return new DaggerContainer(objectGraph, modules);
    }

    @Provides
    public WebApplication webApplication(DaggerContainer daggerContainer) {
        return daggerContainer.getWebApplication();
    }

    @Provides
    public Providers provideProviders(WebApplication webApplication) {
        return webApplication.getProviders();
    }

    @Provides
    public FeaturesAndProperties provideFeaturesAndProperties(WebApplication webApplication) {
        return webApplication.getFeaturesAndProperties();
    }

    @Provides
    public MessageBodyWorkers provideMessageBodyWorkers(WebApplication webApplication) {
        return webApplication.getMessageBodyWorkers();
    }

    @Provides
    public ExceptionMapperContext provideExceptionMapperContext(WebApplication webApplication) {
        return webApplication.getExceptionMapperContext();
    }

    @Provides
    public ResourceContext provideResourceContext(WebApplication webApplication) {
        return webApplication.getResourceContext();
    }
}
