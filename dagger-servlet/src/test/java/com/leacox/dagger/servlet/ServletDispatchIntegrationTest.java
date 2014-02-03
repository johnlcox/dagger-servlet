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

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.leacox.dagger.servlet.ManagedServletPipeline.REQUEST_DISPATCHER_REQUEST;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests the FilterPipeline that dispatches to dagger-managed servlets,
 * is a full integration test, with a real object graph.
 *
 * @author Dhanji R. Prasanna (dhanji gmail com)
 * @author John Leacox
 */
public class ServletDispatchIntegrationTest {
    private static int inits, services, destroys, doFilters;

    @BeforeMethod
    public void setUp() {
        inits = 0;
        services = 0;
        destroys = 0;
        doFilters = 0;

        DaggerFilter.reset();
    }

    @Module(
            injects = {
                    TestServlet.class,
                    NeverServlet.class,
                    TestFilter.class,
                    ForwardingServlet.class,
                    ForwardedServlet.class
            },
            includes = {
                    ServletModule.class
            }
    )
    static class TestModule {

    }

    @Test
    public final void testDispatchRequestToManagedPipelineServlets()
            throws ServletException, IOException {
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
                serve("/*").with(TestServlet.class);

                // These servets should never fire... (ordering test)
                serve("*.html").with(NeverServlet.class);
                serve("/test/*").with(NeverServlet.class);
                serve("/index/*").with(NeverServlet.class);
                serve("*.jsp").with(NeverServlet.class);
            }
        };

        ServletContext servletContext = createMock("blah", ServletContext.class);
        contextListener.contextInitialized(new ServletContextEvent(servletContext));
        ObjectGraph objectGraph = contextListener.getObjectGraph();

        final FilterPipeline pipeline = objectGraph.get(FilterPipeline.class);

        pipeline.initPipeline(null);

        //create ourselves a mock request with test URI
        HttpServletRequest requestMock = createMock(HttpServletRequest.class);

        expect(requestMock.getRequestURI())
                .andReturn("/index.html")
                .times(1);
        expect(requestMock.getContextPath())
                .andReturn("")
                .anyTimes();

        //dispatch request
        replay(requestMock);

        pipeline.dispatch(requestMock, null, createMock(FilterChain.class));

        pipeline.destroyPipeline();

        verify(requestMock);

        assertTrue(inits == 2 && services == 1 && destroys == 2,
                "lifecycle states did not fire correct number of times-- inits: " + inits + "; dos: "
                        + services + "; destroys: " + destroys);
    }

    @Test
    public final void testDispatchRequestToManagedPipelineWithFilter()
            throws ServletException, IOException {
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
                filter("/*").through(TestFilter.class);

                serve("/*").with(TestServlet.class);

                // These servets should never fire...
                serve("*.html").with(NeverServlet.class);
                serve("/test/*").with(NeverServlet.class);
                serve("/index/*").with(NeverServlet.class);
                serve("*.jsp").with(NeverServlet.class);
            }
        };

        ServletContext servletContext = createMock("blah", ServletContext.class);
        contextListener.contextInitialized(new ServletContextEvent(servletContext));
        ObjectGraph objectGraph = contextListener.getObjectGraph();

        final FilterPipeline pipeline = objectGraph.get(FilterPipeline.class);

        pipeline.initPipeline(null);

        //create ourselves a mock request with test URI
        HttpServletRequest requestMock = createMock(HttpServletRequest.class);

        expect(requestMock.getRequestURI())
                .andReturn("/index.html")
                .times(2);
        expect(requestMock.getContextPath())
                .andReturn("")
                .anyTimes();

        //dispatch request
        replay(requestMock);

        pipeline.dispatch(requestMock, null, createMock(FilterChain.class));

        pipeline.destroyPipeline();

        verify(requestMock);

        assertTrue(inits == 3 && services == 1 && destroys == 3 && doFilters == 1,
                "lifecycle states did not fire correct number of times-- inits: " + inits + "; dos: "
                        + services + "; destroys: " + destroys + "; doFilters: " + doFilters);
    }

    @Singleton
    public static class TestServlet extends HttpServlet {
        @Inject
        TestServlet() {}

        @Override
        public void init(ServletConfig filterConfig) throws ServletException {
            inits++;
        }

        @Override
        public void service(ServletRequest servletRequest, ServletResponse servletResponse)
                throws IOException, ServletException {
            services++;
        }

        @Override
        public void destroy() {
            destroys++;
        }
    }

    @Singleton
    public static class NeverServlet extends HttpServlet {
        @Inject
        NeverServlet() {}

        @Override
        public void init(ServletConfig filterConfig) throws ServletException {
            inits++;
        }

        @Override
        public void service(ServletRequest servletRequest, ServletResponse servletResponse)
                throws IOException, ServletException {
            assertTrue(false, "NeverServlet was fired, when it should not have been.");
        }

        @Override
        public void destroy() {
            destroys++;
        }
    }

    @Singleton
    public static class TestFilter implements Filter {
        @Inject
        TestFilter() {}

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            inits++;
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                             FilterChain filterChain) throws IOException, ServletException {
            doFilters++;
            filterChain.doFilter(servletRequest, servletResponse);
        }

        @Override
        public void destroy() {
            destroys++;
        }
    }


    @Singleton
    public static class ForwardingServlet extends HttpServlet {
        @Inject
        ForwardingServlet() {}

        @Override
        public void service(ServletRequest servletRequest, ServletResponse servletResponse)
                throws IOException, ServletException {
            final HttpServletRequest request = (HttpServletRequest) servletRequest;

            request.getRequestDispatcher("/blah.jsp")
                    .forward(servletRequest, servletResponse);
        }
    }

    @Singleton
    public static class ForwardedServlet extends HttpServlet {
        static int forwardedTo = 0;

        // Reset for test.
        @Inject
        public ForwardedServlet() {
            forwardedTo = 0;
        }

        @Override
        public void service(ServletRequest servletRequest, ServletResponse servletResponse)
                throws IOException, ServletException {
            final HttpServletRequest request = (HttpServletRequest) servletRequest;

            assertTrue((Boolean) request.getAttribute(REQUEST_DISPATCHER_REQUEST));
            forwardedTo++;
        }
    }

    @Test
    public void testForwardUsingRequestDispatcher() throws IOException, ServletException {
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
                serve("/").with(ForwardingServlet.class);
                serve("/blah.jsp").with(ForwardedServlet.class);
            }
        };

        ServletContext servletContext = createMock("blah", ServletContext.class);
        contextListener.contextInitialized(new ServletContextEvent(servletContext));
        //ObjectGraph objectGraph = contextListener.getObjectGraph();

        final HttpServletRequest requestMock = createNiceMock(HttpServletRequest.class);
        HttpServletResponse responseMock = createMock(HttpServletResponse.class);
        HttpSession sessionMock = createNiceMock(HttpSession.class);
        expect(requestMock.getRequestURI())
                .andReturn("/")
                .anyTimes();
        expect(requestMock.getContextPath())
                .andReturn("")
                .anyTimes();
        expect(requestMock.getSession()).andReturn(sessionMock).anyTimes();


        requestMock.setAttribute(REQUEST_DISPATCHER_REQUEST, true);
        expect(requestMock.getAttribute(REQUEST_DISPATCHER_REQUEST)).andReturn(true);
        requestMock.removeAttribute(REQUEST_DISPATCHER_REQUEST);

        expect(responseMock.isCommitted()).andReturn(false);
        responseMock.resetBuffer();

        replay(requestMock, responseMock, sessionMock);

        new DaggerFilter()
                .doFilter(requestMock, responseMock,
                        createMock(FilterChain.class));

        assertEquals(ForwardedServlet.forwardedTo, 1, "Incorrect number of forwards");
        verify(requestMock, responseMock);
    }
}
