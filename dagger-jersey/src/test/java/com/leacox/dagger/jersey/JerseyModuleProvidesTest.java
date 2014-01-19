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

import com.leacox.dagger.servlet.DaggerServletContextListener;
import com.sun.jersey.api.core.ResourceContext;
import com.sun.jersey.core.util.FeaturesAndProperties;
import com.sun.jersey.spi.MessageBodyWorkers;
import com.sun.jersey.spi.container.ExceptionMapperContext;
import com.sun.jersey.spi.container.WebApplication;
import dagger.Module;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Providers;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author John Leacox
 */
public class JerseyModuleProvidesTest {
    private final GrizzlyTestServer testServer = new GrizzlyTestServer();

    @Path("test/provides")
    public static class ProvidesResource {
        @Inject
        DaggerContainer daggerContainer;
        @Inject
        WebApplication webApplication;
        @Inject
        Providers providers;
        @Inject
        FeaturesAndProperties featuresAndProperties;
        @Inject
        MessageBodyWorkers messageBodyWorkers;
        @Inject
        ExceptionMapperContext exceptionMapperContext;
        @Inject
        ResourceContext resourceContext;

        @Inject
        public ProvidesResource(DaggerContainer daggerContainer, WebApplication webApplication, Providers providers,
                                FeaturesAndProperties featuresAndProperties, MessageBodyWorkers messageBodyWorkers,
                                ExceptionMapperContext exceptionMapperContext, ResourceContext resourceContext) {
            assertNotNull(daggerContainer);
            assertNotNull(webApplication);
            assertNotNull(providers);
            assertNotNull(featuresAndProperties);
            assertNotNull(messageBodyWorkers);
            assertNotNull(exceptionMapperContext);
            assertNotNull(resourceContext);
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            assertNotNull(daggerContainer);
            assertNotNull(webApplication);
            assertNotNull(providers);
            assertNotNull(featuresAndProperties);
            assertNotNull(messageBodyWorkers);
            assertNotNull(exceptionMapperContext);
            assertNotNull(resourceContext);

            return "Injected";
        }
    }

    @Module(
            injects = ProvidesResource.class,
            includes = JerseyModule.class
    )
    static class AppModule {
    }

    @BeforeMethod
    public void setUp() {
        testServer.startServer(DaggerTestContextListener.class);
    }

    @AfterMethod
    public void tearDown() {
        testServer.stopServer();
    }

    public static class DaggerTestContextListener extends DaggerServletContextListener {
        @Override
        protected Object[] getBaseModules() {
            return new Object[]{new AppModule()};
        }

        @Override
        protected Object[] getRequestScopedModules() {
            return new Object[]{};
        }

        @Override
        protected void configureServlets() {
            serve("*").with(DaggerContainer.class);
        }
    }

    @Test
    public void testJerseyModuleProvides() {
        String value = testServer.getRootResource().path("test/provides").get(String.class);
        assertEquals("Injected", value);
    }
}
