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
import dagger.Module;
import dagger.Provides;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

/**
 * @author John Leacox
 */
public class JerseyInstantiatedTest {
    private final GrizzlyTestServer testServer = new GrizzlyTestServer();

    @Path("test/no-scope")
    public static class NoScopeResource {
        @Inject
        SomeObject someObject;

        public NoScopeResource() { }

        @Context
        UriInfo ui;

        @QueryParam("param")
        String param;

        @GET
        @Produces("text/plain")
        public String getInstance() {
            assertEquals("test/no-scope", ui.getPath());
            assertEquals("noscope", param);

            return someObject.toString();
        }
    }

    public static class SomeObject {
    }

    @Module(
            injects = {
                    SomeObject.class,
                    NoScopeResource.class
            },
            includes = JerseyModule.class
    )
    static class AppModule {
        @Provides
        SomeObject getSomeObject() {
            return new SomeObject();
        }
    }

    @Module(
            injects = {
            },
            addsTo = AppModule.class,
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
            return new Object[]{new AppModule()};
        }

        @Override
        protected Object[] getRequestScopedModules() {
            return new Object[]{RequestModule.class};
        }

        @Override
        protected void configureServlets() {
            serve("*").with(DaggerContainer.class);
        }
    }

    @Test
    public void testNoScopeResource() {
        String objectOne = testServer.getRootResource().path("test/no-scope")
                .queryParam("param", "noscope").get(String.class);
        String objectTwo = testServer.getRootResource().path("test/no-scope")
                .queryParam("param", "noscope").get(String.class);

        assertNotEquals(objectOne, objectTwo);
    }
}
