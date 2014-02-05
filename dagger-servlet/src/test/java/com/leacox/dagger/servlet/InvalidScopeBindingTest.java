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
 *
 * This file incorporates code covered by the following terms:
 *
 *      Copyright (C) 2009 Google Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package com.leacox.dagger.servlet;

import dagger.Module;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import static org.easymock.EasyMock.createMock;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * Ensures that an error is thrown if a Servlet or Filter is bound
 * under any scope other than singleton, explicitly.
 *
 * @author dhanji@gmail.com
 * @author John Leacox
 */
public class InvalidScopeBindingTest {
    @Module(
            injects = {
                    MySingletonFilter.class,
                    MyNonSingletonFilter.class,
                    MyNonSingletonServlet.class
            },
            includes = {
                    ServletModule.class
            }
    )
    static class TestModule {
    }

    @AfterMethod
    protected void tearDown() throws Exception {
        DaggerFilter.reset();
    }

    @Test(enabled = false)
    // Disabled until there is a good way to make sure a servlet is a singleton. There isn't a good way to make sure
    // the class is a singleton. Classes with the @Singleton annotation can be identified, but classes that are
    // singletons via an @Singleton annotated @Provides method won't be identified as singletons. Bad stuff may happen
    // for non-singletons.
    public final void testServletInNonSingletonScopeThrowsServletException() {
        DaggerFilter daggerFilter = new DaggerFilter();

        DaggerServletContextListener contextListener = new DaggerServletContextListener() {
            @Override
            protected Class<?>[] getBaseModules() {
                return new Class<?>[]{TestModule.class};
            }

            @Override
            protected Class<?>[] getRequestScopedModules() {
                return new Class<?>[]{};
            }

            @Override
            protected void configureServlets() {
                serve("/*").with(MyNonSingletonServlet.class);
            }
        };

        ServletContext servletContext = createMock("blah", ServletContext.class);
        contextListener.contextInitialized(new ServletContextEvent(servletContext));

        ServletException se = null;
        try {
            daggerFilter.init(createMock(FilterConfig.class));
        } catch (ServletException e) {
            se = e;
        } finally {
            assertNotNull(se, "Servlet exception was not thrown with wrong scope binding");
        }
    }

    @Test(enabled = false)
    // Disabled until there is a good way to make sure a filter is a singleton. There isn't a good way to make sure
    // the class is a singleton. Classes with the @Singleton annotation can be identified, but classes that are
    // singletons via an @Singleton annotated @Provides method won't be identified as singletons. Bad stuff may happen
    // for non-singletons.
    public final void testFilterInNonSingletonScopeThrowsServletException() {
        DaggerFilter daggerFilter = new DaggerFilter();

        DaggerServletContextListener contextListener = new DaggerServletContextListener() {
            @Override
            protected Class<?>[] getBaseModules() {
                return new Class<?>[]{TestModule.class};
            }

            @Override
            protected Class<?>[] getRequestScopedModules() {
                return new Class<?>[]{};
            }

            @Override
            protected void configureServlets() {
                filter("/*").through(MyNonSingletonFilter.class);
            }
        };

        ServletContext servletContext = createMock("blah", ServletContext.class);
        contextListener.contextInitialized(new ServletContextEvent(servletContext));

        ServletException se = null;
        try {
            daggerFilter.init(createMock(FilterConfig.class));
        } catch (ServletException e) {
            se = e;
        } finally {
            assertNotNull(se, "Servlet exception was not thrown with wrong scope binding");
        }
    }

    @Test
    public final void testHappyCaseFilter() {
        DaggerFilter daggerFilter = new DaggerFilter();

        DaggerServletContextListener contextListener = new DaggerServletContextListener() {
            @Override
            protected Class<?>[] getBaseModules() {
                return new Class<?>[]{TestModule.class};
            }

            @Override
            protected Class<?>[] getRequestScopedModules() {
                return new Class<?>[]{};
            }

            @Override
            protected void configureServlets() {
                // Annotated scoping variant.
                filter("/*").through(MySingletonFilter.class);

                // Explicit scoping to an instance
                filter("/*").through(new DummyFilterImpl());
            }
        };

        ServletContext servletContext = createMock("blah", ServletContext.class);
        contextListener.contextInitialized(new ServletContextEvent(servletContext));

        ServletException se = null;
        try {
            daggerFilter.init(createMock(FilterConfig.class));
        } catch (ServletException e) {
            se = e;
        } finally {
            assertNull(se, "Servlet exception was thrown with correct scope binding");
        }
    }

    @RequestScoped
    public static class MyNonSingletonServlet extends HttpServlet {
        @Inject
        MyNonSingletonServlet() {}
    }

    @SessionScoped
    public static class MyNonSingletonFilter extends DummyFilterImpl {
        @Inject
        MyNonSingletonFilter() {}
    }

    @Singleton
    public static class MySingletonFilter extends DummyFilterImpl {
        @Inject
        MySingletonFilter() {}
    }
}