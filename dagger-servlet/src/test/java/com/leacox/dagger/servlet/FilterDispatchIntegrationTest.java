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
 *      Copyright (C) 2008 Google Inc.
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
import dagger.ObjectGraph;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.leacox.dagger.servlet.ManagedServletPipeline.REQUEST_DISPATCHER_REQUEST;
import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * This tests that filter stage of the pipeline dispatches
 * correctly to dagger-managed filters.
 * <p/>
 * WARNING(dhanji): Non-parallelizable test =(
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author John Leacox
 */
public class FilterDispatchIntegrationTest {
    private static int inits, doFilters, destroys;

    private IMocksControl control;

    @BeforeMethod
    public final void setUp() {
        inits = 0;
        doFilters = 0;
        destroys = 0;
        control = EasyMock.createControl();
        DaggerFilter.reset();
    }

    @Module(
            injects = {
                    TestFilter.class,
                    TestServlet.class
            },
            includes = {
                    ServletModule.class
            }
    )
    static class TestModule {

    }

    @Test
    public final void testDispatchRequestToManagedPipeline() throws ServletException, IOException {
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
                filter("*.html").through(TestFilter.class);
                filter("/*").through(TestFilter.class);

                // These filters should never fire
                filter("/index/*").through(TestFilter.class);
                filter("*.jsp").through(TestFilter.class);

                // Bind a servlet
                serve("*.html").with(TestServlet.class);
            }
        };

        ServletContext servletContext = createMock("blah", ServletContext.class);
        contextListener.contextInitialized(new ServletContextEvent(servletContext));
        final ObjectGraph objectGraph = contextListener.getObjectGraph();

        final FilterPipeline pipeline = objectGraph.get(FilterPipeline.class);
        pipeline.initPipeline(null);

        // create ourselves a mock request with test URI
        HttpServletRequest requestMock = control.createMock(HttpServletRequest.class);

        expect(requestMock.getRequestURI())
                .andReturn("/index.html")
                .anyTimes();
        expect(requestMock.getContextPath())
                .andReturn("")
                .anyTimes();

        requestMock.setAttribute(REQUEST_DISPATCHER_REQUEST, true);
        requestMock.removeAttribute(REQUEST_DISPATCHER_REQUEST);

        HttpServletResponse responseMock = control.createMock(HttpServletResponse.class);
        expect(responseMock.isCommitted())
                .andReturn(false)
                .anyTimes();
        responseMock.resetBuffer();
        expectLastCall().anyTimes();

        FilterChain filterChain = control.createMock(FilterChain.class);

        //dispatch request
        control.replay();
        pipeline.dispatch(requestMock, responseMock, filterChain);
        pipeline.destroyPipeline();
        control.verify();

        TestServlet servlet = objectGraph.get(TestServlet.class);
        assertEquals(2, servlet.processedUris.size());
        assertTrue(servlet.processedUris.contains("/index.html"));
        assertTrue(servlet.processedUris.contains(TestServlet.FORWARD_TO));

        assertTrue(inits == 1 && doFilters == 3 && destroys == 1, "lifecycle states did not"
                + " fire correct number of times-- inits: " + inits + "; dos: " + doFilters
                + "; destroys: " + destroys);
    }

    @Test
    public final void testDispatchRequestToManagedPipelineWithInstance() throws ServletException, IOException {
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
                filter("*.html").through(TestFilter.class);
                filter("/*").through(new TestFilter());

                // These filters should never fire
                filter("/index/*").through(TestFilter.class);
                filter("*.jsp").through(new TestFilter());

                // Bind a servlet
                serve("*.html").with(TestServlet.class);
            }
        };

        ServletContext servletContext = createMock("blah", ServletContext.class);
        contextListener.contextInitialized(new ServletContextEvent(servletContext));
        final ObjectGraph objectGraph = contextListener.getObjectGraph();

        final FilterPipeline pipeline = objectGraph.get(FilterPipeline.class);
        pipeline.initPipeline(null);

        // create ourselves a mock request with test URI
        HttpServletRequest requestMock = control.createMock(HttpServletRequest.class);

        expect(requestMock.getRequestURI())
                .andReturn("/index.html")
                .anyTimes();
        expect(requestMock.getContextPath())
                .andReturn("")
                .anyTimes();

        requestMock.setAttribute(REQUEST_DISPATCHER_REQUEST, true);
        requestMock.removeAttribute(REQUEST_DISPATCHER_REQUEST);

        HttpServletResponse responseMock = control.createMock(HttpServletResponse.class);
        expect(responseMock.isCommitted())
                .andReturn(false)
                .anyTimes();
        responseMock.resetBuffer();
        expectLastCall().anyTimes();

        FilterChain filterChain = control.createMock(FilterChain.class);

        //dispatch request
        control.replay();
        pipeline.dispatch(requestMock, responseMock, filterChain);
        pipeline.destroyPipeline();
        control.verify();

        TestServlet servlet = objectGraph.get(TestServlet.class);
        assertEquals(2, servlet.processedUris.size());
        assertTrue(servlet.processedUris.contains("/index.html"));
        assertTrue(servlet.processedUris.contains(TestServlet.FORWARD_TO));

        // 1 init/destory for the class binding and 2 inits/destroys for the 2 instance bindings
        assertTrue(inits == 3 && doFilters == 3 && destroys == 3, "lifecycle states did not"
                + " fire correct number of times-- inits: " + inits + "; dos: " + doFilters
                + "; destroys: " + destroys);
    }

    @Test
    public final void testDispatchThatNoFiltersFire() throws ServletException, IOException {
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
                filter("/public/*").through(TestFilter.class);
                filter("*.html").through(TestFilter.class);
                filter("*.xml").through(TestFilter.class);

                // These filters should never fire
                filter("/index/*").through(TestFilter.class);
                filter("*.jsp").through(TestFilter.class);
            }
        };

        ServletContext servletContext = createMock("blah", ServletContext.class);
        contextListener.contextInitialized(new ServletContextEvent(servletContext));
        final ObjectGraph objectGraph = contextListener.getObjectGraph();

        final FilterPipeline pipeline = objectGraph.get(FilterPipeline.class);
        pipeline.initPipeline(null);

        //create ourselves a mock request with test URI
        HttpServletRequest requestMock = control.createMock(HttpServletRequest.class);

        expect(requestMock.getRequestURI())
                .andReturn("/index.xhtml")
                .anyTimes();
        expect(requestMock.getContextPath())
                .andReturn("")
                .anyTimes();

        //dispatch request
        FilterChain filterChain = control.createMock(FilterChain.class);
        filterChain.doFilter(requestMock, null);
        control.replay();
        pipeline.dispatch(requestMock, null, filterChain);
        pipeline.destroyPipeline();
        control.verify();

        assertTrue(inits == 1 && doFilters == 0 && destroys == 1, "lifecycle states did not "
                + "fire correct number of times-- inits: " + inits + "; dos: " + doFilters
                + "; destroys: " + destroys);
    }

    @Test
    public final void testDispatchFilterPipelineWithRegexMatching() throws ServletException,
            IOException {
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
                filterRegex("/[A-Za-z]*").through(TestFilter.class);
                filterRegex("/index").through(TestFilter.class);
                //these filters should never fire
                filterRegex("\\w").through(TestFilter.class);
            }
        };

        ServletContext servletContext = createMock("blah", ServletContext.class);
        contextListener.contextInitialized(new ServletContextEvent(servletContext));
        final ObjectGraph objectGraph = contextListener.getObjectGraph();

        final FilterPipeline pipeline = objectGraph.get(FilterPipeline.class);
        pipeline.initPipeline(null);

        //create ourselves a mock request with test URI
        HttpServletRequest requestMock = control.createMock(HttpServletRequest.class);

        expect(requestMock.getRequestURI())
                .andReturn("/index")
                .anyTimes();
        expect(requestMock.getContextPath())
                .andReturn("")
                .anyTimes();

        // dispatch request
        FilterChain filterChain = control.createMock(FilterChain.class);
        filterChain.doFilter(requestMock, null);
        control.replay();
        pipeline.dispatch(requestMock, null, filterChain);
        pipeline.destroyPipeline();
        control.verify();

        assertTrue(inits == 1 && doFilters == 2 && destroys == 1, "lifecycle states did not fire "
                + "correct number of times-- inits: " + inits + "; dos: " + doFilters
                + "; destroys: " + destroys);
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
    public static class TestServlet extends HttpServlet {
        public static final String FORWARD_FROM = "/index.html";
        public static final String FORWARD_TO = "/forwarded.html";
        public List<String> processedUris = new ArrayList<String>();

        @Inject
        TestServlet() {}

        @Override
        protected void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
                throws ServletException, IOException {
            String requestUri = httpServletRequest.getRequestURI();
            processedUris.add(requestUri);

            // If the client is requesting /index.html then we forward to /forwarded.html
            if (FORWARD_FROM.equals(requestUri)) {
                httpServletRequest.getRequestDispatcher(FORWARD_TO)
                        .forward(httpServletRequest, httpServletResponse);
            }
        }

        @Override
        public void service(ServletRequest servletRequest, ServletResponse servletResponse)
                throws ServletException, IOException {
            service((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);
        }
    }
}
