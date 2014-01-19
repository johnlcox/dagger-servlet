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
 *      Copyright (C) 2006 Google Inc.
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.fail;

/**
 * This is a basic whitebox test that verifies the glue between
 * DaggerFilter and ManagedFilterPipeline is working.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author John Leacox
 */
public class FilterPipelineTest {

    @BeforeMethod
    public final void setUp() {
        DaggerFilter.reset();

        ServletContextListener contextListener = new DaggerServletContextListener() {
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
                filter("/*").through(TestFilter.class);
                filter("*.html").through(TestFilter.class);
                filter("/*").through(TestFilter.class);
                filter("*.jsp").through(TestFilter.class);

                // These filters should never fire
                filter("/index/*").through(NeverFilter.class);
                filter("/public/login/*").through(NeverFilter.class);
            }
        };

        ServletContext servletContext = createMock(ServletContext.class);
        contextListener.contextInitialized(new ServletContextEvent(servletContext));
    }

    @AfterMethod
    public final void tearDown() {
        DaggerFilter.reset();
    }

    @Test
    public final void testDispatchThruDaggerFilter() throws ServletException, IOException {

        //create mocks
        FilterConfig filterConfig = createMock(FilterConfig.class);
        ServletContext servletContext = createMock(ServletContext.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);
        FilterChain proceedingFilterChain = createMock(FilterChain.class);

        //begin mock script ***

        expect(filterConfig.getServletContext())
                .andReturn(servletContext)
                .once();

        expect(request.getRequestURI())
                .andReturn("/public/login.jsp")
                .anyTimes();
        expect(request.getContextPath())
                .andReturn("")
                .anyTimes();

        //at the end, proceed down webapp's normal filter chain
        proceedingFilterChain.doFilter(isA(HttpServletRequest.class), (ServletResponse) isNull());
        expectLastCall().once();

        //run mock script ***
        replay(filterConfig, servletContext, request, proceedingFilterChain);

        final DaggerFilter webFilter = new DaggerFilter();

        webFilter.init(filterConfig);
        webFilter.doFilter(request, null, proceedingFilterChain);
        webFilter.destroy();

        //assert expectations
        verify(filterConfig, servletContext, request, proceedingFilterChain);
    }

    @Module(
            injects = {
                    TestFilter.class,
                    NeverFilter.class
            },
            includes = {
                    ServletModule.class
            }
    )
    static class TestModule {

    }

    @Singleton
    public static class TestFilter implements Filter {
        @Inject
        TestFilter() {}

        @Override
        public void init(FilterConfig filterConfig) throws ServletException { }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                             FilterChain filterChain) throws IOException, ServletException {
            filterChain.doFilter(servletRequest, servletResponse);
        }

        @Override
        public void destroy() { }
    }

    @Singleton
    public static class NeverFilter implements Filter {
        @Inject
        NeverFilter() {}

        @Override
        public void init(FilterConfig filterConfig) throws ServletException { }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                             FilterChain filterChain) throws IOException, ServletException {
            fail("This filter should never have fired");
        }

        @Override
        public void destroy() { }
    }
}
