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
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author John Leacox
 */
public class ScopeTest {
    private final GrizzlyTestServer testServer = new GrizzlyTestServer();

    @Path("test/dagger-instantiated")
    public static class DaggerInstantiatedResource {
        private final SomeRequestObject requestObject;
        private final SomeSingletonObject singletonObject;

        @QueryParam("param")
        String param;

        @Inject
        public DaggerInstantiatedResource(SomeRequestObject requestObject, SomeSingletonObject singletonObject) {
            this.requestObject = requestObject;
            this.singletonObject = singletonObject;
        }

        @GET
        @Path("request")
        @Produces(MediaType.TEXT_PLAIN)
        public String getRequestInstance() {
            assertEquals("daggerparam", param);
            assertNotNull(requestObject);
            return requestObject.toString();
        }

        @GET
        @Path("singleton")
        @Produces(MediaType.TEXT_PLAIN)
        public String getSingletonInstance() {
            assertEquals("daggerparam", param);
            assertNotNull(singletonObject);
            return singletonObject.toString();
        }
    }

    @Path("test/jersey-instantiated")
    public static class DaggerInjectedResource {
        @Inject
        SomeRequestObject requestObject;
        @Inject
        SomeSingletonObject singletonObject;

        private final String param;

        public DaggerInjectedResource(@QueryParam("param") String param) {
            this.param = param;
        }

        @GET
        @Path("request")
        @Produces(MediaType.TEXT_PLAIN)
        public String getRequestInstance() {
            assertEquals("jerseyparam", param);
            assertNotNull(requestObject);
            return requestObject.toString();
        }

        @GET
        @Path("singleton")
        @Produces(MediaType.TEXT_PLAIN)
        public String getSingletonInstance() {
            assertEquals("jerseyparam", param);
            assertNotNull(singletonObject);
            return singletonObject.toString();
        }
    }

    public static class SomeRequestObject {
    }

    public static class SomeSingletonObject {
    }

    @Module(
            injects = SomeSingletonObject.class,
            includes = JerseyModule.class
    )
    static class AppModule {
        @Provides
        @Singleton
        SomeSingletonObject getSomeSingletonObject() {
            return new SomeSingletonObject();
        }
    }

    @Module(
            injects = {
                    SomeRequestObject.class,
                    DaggerInstantiatedResource.class,
                    DaggerInjectedResource.class
            },
            addsTo = AppModule.class,
            includes = JerseyRequestModule.class
    )
    static class RequestModule {
        @Provides
        @Singleton
        SomeRequestObject getSomeRequestObject() {
            return new SomeRequestObject();
        }
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
    public void testRequestScopedInjection() {
        String daggerObjectOne = testServer.getRootResource().path("test/dagger-instantiated/request")
                .queryParam("param", "daggerparam").get(String.class);
        String daggerObjectTwo = testServer.getRootResource().path("test/dagger-instantiated/request")
                .queryParam("param", "daggerparam").get(String.class);

        assertNotEquals(daggerObjectOne, daggerObjectTwo);

        String jerseyObjectOne = testServer.getRootResource().path("test/jersey-instantiated/request")
                .queryParam("param", "jerseyparam").get(String.class);
        String jerseyObjectTwo = testServer.getRootResource().path("test/jersey-instantiated/request")
                .queryParam("param", "jerseyparam").get(String.class);

        assertNotEquals(jerseyObjectOne, jerseyObjectTwo);
    }

    @Test
    public void testSingletonInjection() {
        String daggerObjectOne = testServer.getRootResource().path("test/dagger-instantiated/singleton")
                .queryParam("param", "daggerparam").get(String.class);
        String daggerObjectTwo = testServer.getRootResource().path("test/dagger-instantiated/singleton")
                .queryParam("param", "daggerparam").get(String.class);

        assertEquals(daggerObjectOne, daggerObjectTwo);

        String jerseyObjectOne = testServer.getRootResource().path("test/jersey-instantiated/singleton")
                .queryParam("param", "jerseyparam").get(String.class);
        String jerseyObjectTwo = testServer.getRootResource().path("test/jersey-instantiated/singleton")
                .queryParam("param", "jerseyparam").get(String.class);

        assertEquals(jerseyObjectOne, jerseyObjectTwo);
    }
}
