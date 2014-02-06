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
import com.sun.jersey.api.core.ExtendedUriInfo;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.api.core.HttpResponseContext;
import dagger.Module;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author John Leacox
 */
public class JerseyRequestModuleProvidesTest {
    private final GrizzlyTestServer testServer = new GrizzlyTestServer();

    @Path("test/request-provides")
    public static class RequestProvidesResource {
        @Inject
        HttpContext httpContext;
        @Inject
        UriInfo uriInfo;
        @Inject
        ExtendedUriInfo extendedUriInfo;
        @Inject
        HttpRequestContext httpRequestContext;
        @Inject
        HttpHeaders httpHeaders;
        @Inject
        Request request;
        @Inject
        SecurityContext securityContext;
        @Inject
        HttpResponseContext httpResponseContext;

        @Inject
        public RequestProvidesResource(HttpContext httpContext, UriInfo uriInfo, ExtendedUriInfo extendedUriInfo,
                                       HttpRequestContext httpRequestContext, HttpHeaders httpHeaders,
                                       Request request, SecurityContext securityContext,
                                       HttpResponseContext httpResponseContext) {
            assertNotNull(httpContext);
            assertNotNull(uriInfo);
            assertNotNull(extendedUriInfo);
            assertNotNull(httpRequestContext);
            assertNotNull(httpHeaders);
            assertNotNull(request);
            assertNotNull(securityContext);
            assertNotNull(httpResponseContext);
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            assertNotNull(httpContext);
            assertNotNull(uriInfo);
            assertNotNull(extendedUriInfo);
            assertNotNull(httpRequestContext);
            assertNotNull(httpHeaders);
            assertNotNull(request);
            assertNotNull(securityContext);
            assertNotNull(httpResponseContext);

            return "Injected";
        }
    }

    @Module(includes = JerseyModule.class)
    static class AppModule {
    }

    @Module(
            injects = RequestProvidesResource.class,
            includes = JerseyRequestModule.class
    )
    static class RequestModule {
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
            return new Object[]{AppModule.class};
        }

        @Override
        protected Object[] getRequestScopedModules() {
            return new Object[]{new RequestModule()};
        }

        @Override
        protected void configureServlets() {
            serve("*").with(DaggerContainer.class);
        }
    }

    @Test
    public void testJerseyRequestModuleProvides() {
        String value = testServer.getRootResource().path("test/request-provides").get(String.class);
        assertEquals("Injected", value);
    }
}
