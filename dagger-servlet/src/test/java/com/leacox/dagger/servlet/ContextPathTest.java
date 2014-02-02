/**
 * Copyright (C) 2011 Google Inc.
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
import org.easymock.IMocksControl;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * Tests to make sure that servlets with a context path are handled right.
 *
 * @author John Leacox
 */
public class ContextPathTest {

    @Inject
    FooTestServlet fooServlet;

    @Inject
    BarTestServlet barServlet;

    private IMocksControl globalControl;
    private ObjectGraph objectGraph;
    private ServletContext servletContext;
    private FilterConfig filterConfig;
    private DaggerFilter daggerFilter;

    @Module(
            injects = {
                    FooTestServlet.class,
                    BarTestServlet.class,
                    ContextPathTest.class
            },
            includes = {
                    ServletModule.class
            }
    )
    static class TestModule {
        @Provides
        @Singleton
        FooTestServlet provideFooTestServlet() {
            return new FooTestServlet();
        }

        @Provides
        @Singleton
        BarTestServlet provideBarTestServlet() {
            return new BarTestServlet();
        }
    }

    @BeforeMethod
    public final void setUp() throws Exception {
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
                serve("/foo/*").with(FooTestServlet.class);
                serve("/bar/*").with(BarTestServlet.class);
            }
        };

        servletContext = createMock("blah", ServletContext.class);

        contextListener.contextInitialized(new ServletContextEvent(servletContext));
        objectGraph = contextListener.getObjectGraph();
        objectGraph.inject(this);

        globalControl = createControl();

        assertNotNull(fooServlet);
        assertNotNull(barServlet);
        assertNotSame(fooServlet, barServlet);

        servletContext = globalControl.createMock(ServletContext.class);
        filterConfig = globalControl.createMock(FilterConfig.class);

        expect(filterConfig.getServletContext()).andReturn(servletContext).anyTimes();

        globalControl.replay();

        daggerFilter = new DaggerFilter();
        daggerFilter.init(filterConfig);
    }

    @AfterMethod
    public final void tearDown() {
        assertNotNull(fooServlet);
        assertNotNull(barServlet);

        fooServlet = null;
        barServlet = null;

        daggerFilter.destroy();
        daggerFilter = null;

        DaggerFilter.reset();

        objectGraph = null;

        filterConfig = null;
        servletContext = null;

        globalControl.verify();
    }

    @Test
    public void testSimple() throws Exception {
        IMocksControl testControl = createControl();
        TestFilterChain testFilterChain = new TestFilterChain();
        HttpServletRequest req = testControl.createMock(HttpServletRequest.class);
        HttpServletResponse res = testControl.createMock(HttpServletResponse.class);

        expect(req.getMethod()).andReturn("GET").anyTimes();
        expect(req.getRequestURI()).andReturn("/bar/foo").anyTimes();
        expect(req.getServletPath()).andReturn("/bar/foo").anyTimes();
        expect(req.getContextPath()).andReturn("").anyTimes();

        testControl.replay();

        daggerFilter.doFilter(req, res, testFilterChain);

        assertFalse(testFilterChain.isTriggered());
        assertFalse(fooServlet.isTriggered());
        assertTrue(barServlet.isTriggered());

        testControl.verify();
    }

    //
    // each of the following "runTest" calls takes three path parameters:
    //
    // The value of "getRequestURI()"
    // The value of "getServletPath()"
    // The value of "getContextPath()"
    //
    // these values have been captured using a filter in Apache Tomcat 6.0.32
    // and are used for real-world values that a servlet container would send into
    // the DaggerFilter.
    //
    // the remaining three booleans are:
    //
    // True if the request gets passed down the filter chain
    // True if the request hits the "foo" servlet
    // True if the request hits the "bar" sevlet
    //
    // After adjusting the request URI for the web app deployment location, all
    // calls
    // should always produce the same result.
    //

    // ROOT Web app, using Tomcat default servlet
    @Test
    public void testRootDefault() throws Exception {
        // fetching /. Should go up the filter chain (only mappings on /foo/* and /bar/*).
        runTest("/", "/", "", true, false, false);
        // fetching /bar/. Should hit the bar servlet
        runTest("/bar/", "/bar/", "", false, false, true);
        // fetching /foo/xxx. Should hit the foo servlet
        runTest("/foo/xxx", "/foo/xxx", "", false, true, false);
        // fetching /xxx. Should go up the chain
        runTest("/xxx", "/xxx", "", true, false, false);
    }

    // ROOT Web app, using explicit backing servlet mounted at /*
    @Test
    public void testRootExplicit() throws Exception {
        // fetching /. Should go up the filter chain (only mappings on /foo/* and /bar/*).
        runTest("/", "", "", true, false, false);
        // fetching /bar/. Should hit the bar servlet
        runTest("/bar/", "", "", false, false, true);
        // fetching /foo/xxx. Should hit the foo servlet
        runTest("/foo/xxx", "", "", false, true, false);
        // fetching /xxx. Should go up the chain
        runTest("/xxx", "", "", true, false, false);
    }

    // ROOT Web app, using two backing servlets, mounted at /bar/* and /foo/*
    @Test
    public void testRootSpecific() throws Exception {
        // fetching /. Should go up the filter chain (only mappings on /foo/* and /bar/*).
        runTest("/", "/", "", true, false, false);
        // fetching /bar/. Should hit the bar servlet
        runTest("/bar/", "/bar", "", false, false, true);
        // fetching /foo/xxx. Should hit the foo servlet
        runTest("/foo/xxx", "/foo", "", false, true, false);
        // fetching /xxx. Should go up the chain
        runTest("/xxx", "/xxx", "", true, false, false);
    }

    // Web app located at /webtest, using Tomcat default servlet
    @Test
    public void testWebtestDefault() throws Exception {
        // fetching /. Should go up the filter chain (only mappings on /foo/* and /bar/*).
        runTest("/webtest/", "/", "/webtest", true, false, false);
        // fetching /bar/. Should hit the bar servlet
        runTest("/webtest/bar/", "/bar/", "/webtest", false, false, true);
        // fetching /foo/xxx. Should hit the foo servlet
        runTest("/webtest/foo/xxx", "/foo/xxx", "/webtest", false, true, false);
        // fetching /xxx. Should go up the chain
        runTest("/webtest/xxx", "/xxx", "/webtest", true, false, false);
    }

    // Web app located at /webtest, using explicit backing servlet mounted at /*
    @Test
    public void testWebtestExplicit() throws Exception {
        // fetching /. Should go up the filter chain (only mappings on /foo/* and /bar/*).
        runTest("/webtest/", "", "/webtest", true, false, false);
        // fetching /bar/. Should hit the bar servlet
        runTest("/webtest/bar/", "", "/webtest", false, false, true);
        // fetching /foo/xxx. Should hit the foo servlet
        runTest("/webtest/foo/xxx", "", "/webtest", false, true, false);
        // fetching /xxx. Should go up the chain
        runTest("/webtest/xxx", "", "/webtest", true, false, false);
    }

    // Web app located at /webtest, using two backing servlets, mounted at /bar/*
    // and /foo/*
    @Test
    public void testWebtestSpecific() throws Exception {
        // fetching /. Should go up the filter chain (only mappings on /foo/* and
        // /bar/*).
        runTest("/webtest/", "/", "/webtest", true, false, false);
        // fetching /bar/. Should hit the bar servlet
        runTest("/webtest/bar/", "/bar", "/webtest", false, false, true);
        // fetching /foo/xxx. Should hit the foo servlet
        runTest("/webtest/foo/xxx", "/foo", "/webtest", false, true, false);
        // fetching /xxx. Should go up the chain
        runTest("/webtest/xxx", "/xxx", "/webtest", true, false, false);
    }

    private void runTest(final String requestURI, final String servletPath, final String contextPath,
                         final boolean filterResult, final boolean fooResult, final boolean barResult)
            throws Exception {
        IMocksControl testControl = createControl();

        barServlet.clear();
        fooServlet.clear();

        TestFilterChain testFilterChain = new TestFilterChain();
        HttpServletRequest req = testControl.createMock(HttpServletRequest.class);
        HttpServletResponse res = testControl.createMock(HttpServletResponse.class);

        expect(req.getMethod()).andReturn("GET").anyTimes();
        expect(req.getRequestURI()).andReturn(requestURI).anyTimes();
        expect(req.getServletPath()).andReturn(servletPath).anyTimes();
        expect(req.getContextPath()).andReturn(contextPath).anyTimes();

        testControl.replay();

        daggerFilter.doFilter(req, res, testFilterChain);

        assertEquals(filterResult, testFilterChain.isTriggered());
        assertEquals(fooResult, fooServlet.isTriggered());
        assertEquals(barResult, barServlet.isTriggered());

        testControl.verify();
    }

    public static class FooTestServlet extends TestServlet {
        @Inject
        public FooTestServlet() {}
    }

    public static class BarTestServlet extends TestServlet {
        @Inject
        public BarTestServlet() {}
    }

    public static abstract class TestServlet extends HttpServlet {
        private boolean triggered = false;

        @Override
        public void doGet(HttpServletRequest req, HttpServletResponse resp) {
            triggered = true;
        }

        public boolean isTriggered() {
            return triggered;
        }

        public void clear() {
            triggered = false;
        }
    }

    public static class TestFilterChain implements FilterChain {
        private boolean triggered = false;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException,
                ServletException {
            triggered = true;
        }

        public boolean isTriggered() {
            return triggered;
        }

        public void clear() {
            triggered = false;
        }
    }
}
