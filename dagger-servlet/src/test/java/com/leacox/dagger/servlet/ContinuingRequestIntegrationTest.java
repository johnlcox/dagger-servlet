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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Tests continuation of requests
 */
public class ContinuingRequestIntegrationTest {
    private static final String PARAM_VALUE = "there";
    private static final String PARAM_NAME = "hi";

    private final AtomicBoolean failed = new AtomicBoolean(false);
    private final AbstractExecutorService sameThreadExecutor = new AbstractExecutorService() {
        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return ImmutableList.of();
        }

        @Override
        public boolean isShutdown() {
            return true;
        }

        @Override
        public boolean isTerminated() {
            return true;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            try {
                task.call();
                fail();
            } catch (Exception e) {
                // Expected.
                assertTrue(e instanceof IllegalStateException);
                failed.set(true);
            }

            return null;
        }
    };

    private ExecutorService executor;
    private ObjectGraph objectGraph;
    private static OffRequestCallable staticCallable;

    @Module(
            injects = {
                    ExecutorService.class,
                    ContinuingServlet.class
            },
            includes = {
                    ServletModule.class
            },
            library = true
    )
    class TestAppModule {
        public TestAppModule() {}

        @Provides
        ExecutorService provideExecutorService() {
            return executor;
        }

        @Provides
        ContinuingServlet provideContinuingServlet(ExecutorService executorService, ObjectGraph objectGraph) {
            return new ContinuingServlet(executorService, objectGraph);
        }
    }

    @Module(
            injects = {
                    OffRequestCallable.class
            },
            includes = {
                    ServletRequestModule.class
            },
            addsTo = TestAppModule.class,
            library = true
    )
    class TestRequestModule {
        public TestRequestModule() {}

        @Provides
        @Singleton
        SomeObject provideSomeObject() {
            return new SomeObject();
        }

        @Provides
        OffRequestCallable provideOffRequestCallable() {
            return new OffRequestCallable();
        }
    }

    @AfterMethod
    protected void tearDown() throws Exception {
        objectGraph.get(DaggerFilter.class).destroy();
        staticCallable = null;
    }

    @Test
    public final void testRequestContinuesInOtherThread()
            throws ServletException, IOException, InterruptedException {
        executor = Executors.newSingleThreadExecutor();

        DaggerServletContextListener contextListener = new DaggerServletContextListener() {
            @Override
            protected Object[] getBaseModules() {
                return new Object[]{new TestAppModule()};
            }

            @Override
            protected Object[] getRequestScopedModules() {
                return new Object[]{new TestRequestModule()};
            }

            @Override
            protected void configureServlets() {
                serve("/*").with(ContinuingServlet.class);
            }
        };

        ServletContext servletContext = createNiceMock(ServletContext.class);
        contextListener.contextInitialized(new ServletContextEvent(servletContext));
        objectGraph = contextListener.getObjectGraph();

        FilterConfig filterConfig = createMock(FilterConfig.class);
        expect(filterConfig.getServletContext()).andReturn(createMock(ServletContext.class));

        DaggerFilter daggerFilter = objectGraph.get(DaggerFilter.class);

        HttpServletRequest request = createNiceMock(HttpServletRequest.class);

        expect(request.getRequestURI()).andReturn("/");
        expect(request.getContextPath())
                .andReturn("")
                .anyTimes();
        expect(request.getMethod()).andReturn("GET");

        FilterChain filterChain = createMock(FilterChain.class);
        expect(request.getParameter(PARAM_NAME)).andReturn(PARAM_VALUE);

        replay(request, filterConfig, filterChain);

        daggerFilter.init(filterConfig);
        daggerFilter.doFilter(request, createMock(HttpServletResponse.class), filterChain);

        // join.
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(PARAM_VALUE, staticCallable.value);
        verify(request, filterConfig, filterChain);
    }

    @Test
    public final void testRequestContinuationDiesInHttpRequestThread()
            throws ServletException, IOException, InterruptedException {
        executor = sameThreadExecutor;

        DaggerServletContextListener contextListener = new DaggerServletContextListener() {
            @Override
            protected Object[] getBaseModules() {
                return new Object[]{new TestAppModule()};
            }

            @Override
            protected Object[] getRequestScopedModules() {
                return new Object[]{new TestRequestModule()};
            }

            @Override
            protected void configureServlets() {
                serve("/*").with(ContinuingServlet.class);
            }
        };

        ServletContext servletContext = createNiceMock(ServletContext.class);
        contextListener.contextInitialized(new ServletContextEvent(servletContext));
        objectGraph = contextListener.getObjectGraph();

        FilterConfig filterConfig = createMock(FilterConfig.class);
        expect(filterConfig.getServletContext()).andReturn(servletContext); //createMock(ServletContext.class));

        DaggerFilter daggerFilter = objectGraph.get(DaggerFilter.class);

        HttpServletRequest request = createNiceMock(HttpServletRequest.class);

        expect(request.getRequestURI()).andReturn("/");
        expect(request.getContextPath())
                .andReturn("")
                .anyTimes();

        expect(request.getMethod()).andReturn("GET");
        FilterChain filterChain = createMock(FilterChain.class);

        replay(request, filterConfig, filterChain);

        daggerFilter.init(filterConfig);
        daggerFilter.doFilter(request, createMock(HttpServletResponse.class), filterChain);

        // join.
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertTrue(failed.get());
        assertFalse(PARAM_VALUE.equals(staticCallable.value));

        verify(request, filterConfig, filterChain);
    }

    @Singleton
    public static class SomeObject {
    }

    @Singleton
    public static class ContinuingServlet extends HttpServlet {
        ExecutorService executorService;
        ObjectGraph objectGraph;
        OffRequestCallable callable;

        @Inject
        ContinuingServlet(ExecutorService executorService, ObjectGraph objectGraph) {
            this.executorService = executorService;
            this.objectGraph = objectGraph;
            this.callable = new OffRequestCallable();
        }

        private SomeObject someObject;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            assertNull(someObject);

            objectGraph.inject(callable);
            // Store in the static callable so we can verify after the request is gone
            staticCallable = callable;
            // Seed with someobject.
            someObject = new SomeObject();
            Callable<String> task = ServletScopes.continueRequest(callable,
                    ImmutableMap.<Class<?>, Object>of(SomeObject.class, someObject));

            executorService.submit(task);
        }
    }

    @Singleton
    public static class OffRequestCallable implements Callable<String> {
        @Inject
        Provider<HttpServletRequest> request;
        @Inject
        Provider<HttpServletResponse> response;
        @Inject
        Provider<SomeObject> someObject;

        public OffRequestCallable() {}

        public String value;

        @Override
        public String call() throws Exception {
            assertNull(response.get());

            // Inside this request, we should always get the same instance.
            assertSame(someObject.get(), someObject.get());

            return value = request.get().getParameter(PARAM_NAME);
        }
    }
}
