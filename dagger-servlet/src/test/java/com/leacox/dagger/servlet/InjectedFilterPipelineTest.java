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
import dagger.ObjectGraph;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.fail;

/**
 * Exactly the same as {@linkplain com.leacox.dagger.servlet.FilterPipelineTest} except
 * that we test that the static pipeline is not used.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author John Leacox
 */
public class InjectedFilterPipelineTest {
    private ObjectGraph objectGraph1;
    private ObjectGraph objectGraph2;

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

    @BeforeMethod
    public final void setUp() {
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
                filter("/*").through(TestFilter.class);
                filter("*.html").through(TestFilter.class);
                filter("/*").through(TestFilter.class);
                filter("*.jsp").through(TestFilter.class);

                // These filters should never fire
                filter("/index/*").through(NeverFilter.class);
                filter("/public/login/*").through(NeverFilter.class);
            }
        };

        ServletContext servletContext1 = createMock(ServletContext.class);
        contextListener1.contextInitialized(new ServletContextEvent(servletContext1));
        objectGraph1 = contextListener1.getObjectGraph();

        // Test second object graph with exactly opposite pipeline config
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
                // These filters should never fire
                filter("*.html").through(NeverFilter.class);
                filter("/non-jsp/*").through(NeverFilter.class);

                // only these filters fire.
                filter("/index/*").through(TestFilter.class);
                filter("/public/login/*").through(TestFilter.class);
            }
        };

        ServletContext servletContext2 = createMock(ServletContext.class);
        contextListener2.contextInitialized(new ServletContextEvent(servletContext2));
        objectGraph2 = contextListener2.getObjectGraph();
    }

    @AfterMethod
    public final void tearDown() {
        DaggerFilter.reset();
    }

    @Test
    public final void testDispatchThruInjectedDaggerFilter() throws ServletException, IOException {
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
                .andReturn("/non-jsp/login.html") // use a path that will fail in objectGraph2
                .anyTimes();
        expect(request.getContextPath())
                .andReturn("")
                .anyTimes();

        //at the end, proceed down webapp's normal filter chain
        proceedingFilterChain.doFilter(isA(HttpServletRequest.class), (ServletResponse) isNull());
        expectLastCall().once();

        //run mock script ***
        replay(filterConfig, servletContext, request, proceedingFilterChain);

        DaggerFilter webFilter = objectGraph1.get(DaggerFilter.class);

        webFilter.init(filterConfig);
        webFilter.doFilter(request, null, proceedingFilterChain);
        webFilter.destroy();

        //assert expectations
        verify(filterConfig, servletContext, request, proceedingFilterChain);


        // reset mocks and run them against the other object graph
        reset(filterConfig, servletContext, request, proceedingFilterChain);

        // Create a second proceeding filter chain
        FilterChain proceedingFilterChain2 = createMock(FilterChain.class);

        //begin mock script ***

        expect(filterConfig.getServletContext())
                .andReturn(servletContext)
                .once();
        expect(request.getRequestURI())
                .andReturn("/public/login/login.jsp") // use a path that will fail in objectGraph1
                .anyTimes();
        expect(request.getContextPath())
                .andReturn("")
                .anyTimes();

        //at the end, proceed down webapp's normal filter chain
        proceedingFilterChain2.doFilter(isA(HttpServletRequest.class), (ServletResponse) isNull());
        expectLastCall().once();

        // Never fire on this pipeline
        replay(filterConfig, servletContext, request, proceedingFilterChain2, proceedingFilterChain);

        webFilter = objectGraph2.get(DaggerFilter.class);

        webFilter.init(filterConfig);
        webFilter.doFilter(request, null, proceedingFilterChain2);
        webFilter.destroy();

        // Verify that we have not crossed the streams, Venkman!
        verify(filterConfig, servletContext, request, proceedingFilterChain, proceedingFilterChain2);
    }

    @Singleton
    public static class TestFilter implements Filter {
        @Inject
        TestFilter() {
        }

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                             FilterChain filterChain) throws IOException, ServletException {
            filterChain.doFilter(servletRequest, servletResponse);
        }

        @Override
        public void destroy() {
        }
    }

    @Singleton
    public static class NeverFilter implements Filter {
        @Inject
        NeverFilter() {
        }

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                             FilterChain filterChain) throws IOException, ServletException {
            fail("This filter should never have fired");
        }

        @Override
        public void destroy() {
        }
    }
}
