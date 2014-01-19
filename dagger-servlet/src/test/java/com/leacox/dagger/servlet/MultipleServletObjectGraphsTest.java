/**
 * Copyright (C) 2010 Google Inc.
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

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.testng.annotations.Test;

import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpServlet;

import static com.leacox.dagger.servlet.DaggerServletContextListener.OBJECT_GRAPH_NAME;
import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * This gorgeous test asserts that multiple servlet pipelines can
 * run in the SAME JVM. booya.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author John Leacox
 */
public class MultipleServletObjectGraphsTest {
    @Module(
            injects = {
            },
            includes = {
                    ServletModule.class
            },
            library = true
    )
    static class TestModule {
        @Provides
        @Singleton
        DummyServlet provideDummyServlet() {
            return new DummyServlet();
        }

        @Provides
        @Singleton
        DummyFilterImpl provideDummyFilter() {
            return new DummyFilterImpl();
        }
    }

    @Test
    public final void testTwoObjectGraphs() {
        ServletContext fakeContextOne = createMock(ServletContext.class);
        ServletContext fakeContextTwo = createMock(ServletContext.class);

        fakeContextOne.setAttribute(eq(OBJECT_GRAPH_NAME), isA(ObjectGraph.class));
        expectLastCall().once();

        fakeContextTwo.setAttribute(eq(OBJECT_GRAPH_NAME), isA(ObjectGraph.class));
        expectLastCall().once();

        replay(fakeContextOne);

        // Simulate the start of a servlet container.
        DaggerServletContextListener contextListener1 = new DaggerServletContextListener() {
            @Override
            protected Class<?>[] getBaseModules() {
                return new Class<?>[]{TestModule.class};
            }

            @Override
            protected Class<?>[] getRequestScopedModules() {
                return new Class<?>[]{ServletRequestModule.class};
            }

            @Override
            protected void configureServlets() {
                // This creates a ManagedFilterPipeline internally...
                serve("/*").with(DummyServlet.class);
            }
        };

        contextListener1.contextInitialized(new ServletContextEvent(fakeContextOne));
        ObjectGraph objectGraph1 = contextListener1.getObjectGraph();

        ServletContext contextOne = objectGraph1.get(ServletContext.class);
        assertNotNull(contextOne);

        // Now simulate a second injector with a slightly different config.
        replay(fakeContextTwo);
        DaggerServletContextListener contextListener2 = new DaggerServletContextListener() {
            @Override
            protected Class<?>[] getBaseModules() {
                return new Class<?>[]{TestModule.class};
            }

            @Override
            protected Class<?>[] getRequestScopedModules() {
                return new Class<?>[]{ServletRequestModule.class};
            }

            @Override
            protected void configureServlets() {
                // This creates a ManagedFilterPipeline internally...
                filter("/8").through(DummyFilterImpl.class);

                serve("/*").with(HttpServlet.class);
            }
        };

        contextListener2.contextInitialized(new ServletContextEvent(fakeContextTwo));
        ObjectGraph objectGraph2 = contextListener2.getObjectGraph();

        ServletContext contextTwo = objectGraph2.get(ServletContext.class);

        // Make sure they are different.
        assertNotNull(contextTwo);
        assertNotSame(contextOne, contextTwo);

        // Make sure they are as expected
        assertSame(fakeContextOne, contextOne);
        assertSame(fakeContextTwo, contextTwo);

        // Make sure they are consistent.
        assertSame(contextOne, objectGraph1.get(ServletContext.class));
        assertSame(contextTwo, objectGraph2.get(ServletContext.class));

        verify(fakeContextOne, fakeContextTwo);
    }
}
