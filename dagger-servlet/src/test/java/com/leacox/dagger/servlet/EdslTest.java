/**
 * Copyright (C) 2008 Google Inc.
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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.util.HashMap;

import static org.easymock.EasyMock.createMock;
import static org.testng.Assert.assertEquals;

/**
 * Sanity checks the EDSL and resultant definitions.
 *
 * @author Dhanji R. Prasanna (dhanji gmail com)
 * @author John Leacox
 */
public class EdslTest {

    @Inject
    FilterDefinition[] filterDefinitions;

    @Inject
    ServletDefinition[] servletDefinitions;

    @Module(
            injects = {
                    EdslTest.class
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
    public final void testConfigureServlets() {
        DaggerServletContextListener contextListener = new DaggerServletContextListener() {
            @Override
            protected Class<?>[] getBaseModules() {
                return new Class<?>[]{TestModule.class};
            }

            @Override
            protected Class<?>[] getRequestScopedModules() {
                return new Class<?>[]{ServletRequestModule.class};
            }

            @Override
            protected Class<?>[] getSessionScopedModules() {
                return new Class<?>[0];
            }

            @Override
            protected void configureServlets() {
                filter("/*").through(DummyFilterImpl.class);
                filter("*.html").through(DummyFilterImpl.class);
//                filter("/*").through(Key.get(DummyFilterImpl.class));
                filter("/*").through(new DummyFilterImpl());

                filter("*.html").through(DummyFilterImpl.class,
                        new HashMap<String, String>());

                filterRegex("/person/[0-9]*").through(DummyFilterImpl.class);
                filterRegex("/person/[0-9]*").through(DummyFilterImpl.class,
                        new HashMap<String, String>());

//                filterRegex("/person/[0-9]*").through(Key.get(DummyFilterImpl.class));
//                filterRegex("/person/[0-9]*").through(Key.get(DummyFilterImpl.class),
//                        new HashMap<String, String>());

                filterRegex("/person/[0-9]*").through(new DummyFilterImpl());
                filterRegex("/person/[0-9]*").through(new DummyFilterImpl(),
                        new HashMap<String, String>());


                serve("/1/*").with(DummyServlet.class);
//                serve("/2/*").with(Key.get(DummyServlet.class));
                serve("/3/*").with(new DummyServlet());
                serve("/4/*").with(DummyServlet.class, new HashMap<String, String>());

//                serve("*.htm").with(Key.get(DummyServlet.class));
//                serve("*.html").with(Key.get(DummyServlet.class),
//                        new HashMap<String, String>());

                serveRegex("/person/[0-8]*").with(DummyServlet.class);
                serveRegex("/person/[0-9]*").with(DummyServlet.class,
                        new HashMap<String, String>());

//                serveRegex("/person/[0-6]*").with(Key.get(DummyServlet.class));
//                serveRegex("/person/[0-9]/2/*").with(Key.get(DummyServlet.class),
//                        new HashMap<String, String>());

                serveRegex("/person/[0-5]*").with(new DummyServlet());
                serveRegex("/person/[0-9]/3/*").with(new DummyServlet(),
                        new HashMap<String, String>());
            }
        };

        ServletContext servletContext = createMock(ServletContext.class);

        // Verify that it doesn't blow up and the definition counts match.
        contextListener.contextInitialized(new ServletContextEvent(servletContext));
        ObjectGraph objectGraph = contextListener.getObjectGraph();
        objectGraph.inject(this);

        // TODO: Add tests making sure the definitions are correct.

        assertEquals(8, filterDefinitions.length);
        assertEquals(7, servletDefinitions.length);
    }
}
