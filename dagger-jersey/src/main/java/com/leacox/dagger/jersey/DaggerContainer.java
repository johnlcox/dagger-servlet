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

import com.leacox.dagger.servlet.internal.ModuleClasses;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.container.servlet.WebConfig;
import dagger.ObjectGraph;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import java.util.Map;

/**
 * A {@code servlet} for deploying root resource classes with Dagger injection integration.
 * <p/>
 * This class must be registered in {@link com.leacox.dagger.servlet.DaggerServletContextListener#configureServlets()}.
 * This class extends {@link ServletContainer} and initiates the {@link WebApplication} with a Dagger-based
 * {@link com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory}, so that instances of resource and provider
 * classes managed by Dagger can be injected.
 * <p/>
 * Dagger-managed classes that are root resource classes or provider classes will be automatically registered with,
 * so that they do not need to be configured in the web.xml.
 *
 * @author John Leacox
 */
@Singleton
public class DaggerContainer extends ServletContainer {
    private final ObjectGraph objectGraph;
    private final Object[] modules;

    private WebApplication webApplication;

    @Inject
    public DaggerContainer(ObjectGraph objectGraph, @ModuleClasses Object[] modules) {
        this.objectGraph = objectGraph;
        this.modules = modules;
    }

    @Override
    protected ResourceConfig getDefaultResourceConfig(Map<String, Object> properties, WebConfig webConfig)
            throws ServletException {
        return new DefaultResourceConfig();
    }

    @Override
    protected void initiate(ResourceConfig config, WebApplication webApplication) {
        this.webApplication = webApplication;
        webApplication.initiate(config, new DaggerComponentProviderFactory(config, objectGraph, modules));
    }

    public WebApplication getWebApplication() {
        return webApplication;
    }
}
