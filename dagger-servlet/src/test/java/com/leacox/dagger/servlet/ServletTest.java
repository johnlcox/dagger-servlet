/**
 * Copyright (C) 2006 Google Inc.
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Singleton;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static com.leacox.dagger.servlet.ServletScopes.NullObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * @author crazybob@google.com (Bob Lee)
 * @author John Leacox
 */
public class ServletTest {
    private static final ServletScopes.DaggerKey OBJECT_GRAPH_KEY = ServletScopes.DaggerKey.get(ObjectGraph.class);
    private static final ServletScopes.DaggerKey IN_REQUEST_KEY = ServletScopes.DaggerKey.get(InRequest.class);
    private static final ServletScopes.DaggerKey IN_REQUEST_NULLABLE_KEY =
            ServletScopes.DaggerKey.get(InRequestNullable.class);

    @Module(
            injects = {
            },
            includes = {
                    ServletModule.class
            }
    )
    static class TestAppModule {
    }

    @Module(
            injects = {
                    InRequest.class,
                    InRequestNullable.class
            },
            includes = {
                    ServletRequestModule.class
            },
            addsTo = TestAppModule.class,
            library = true
    )
    static class TestRequestModule {
        @Provides
        @Singleton
        InRequest provideInRequest() {
            return new InRequest();
        }

        @Provides
        @Singleton
        InRequestNullable provideInRequestNullable() {
            return null;
        }
    }

    @BeforeMethod
    public void setUp() {
        //we need to clear the reference to the pipeline every test =(
        DaggerFilter.reset();
    }

    @Test
    public void testNewRequestObject() throws IOException, ServletException {
        DaggerServletContextListener contextListener = new DaggerServletContextListener() {
            @Override
            protected Class<?>[] getBaseModules() {
                return new Class<?>[]{TestAppModule.class};
            }

            @Override
            protected Class<?>[] getRequestScopedModules() {
                return new Class<?>[]{TestRequestModule.class};
            }

            @Override
            protected void configureServlets() {
            }
        };

        ServletContext servletContext = createMock(ServletContext.class);
        contextListener.contextInitialized(new ServletContextEvent(servletContext));
        final ObjectGraph objectGraph = contextListener.getObjectGraph();

        DaggerFilter filter = new DaggerFilter();

        final HttpServletRequest request = createMock(HttpServletRequest.class);

        String inRequestKey = IN_REQUEST_KEY.toString();
        expect(request.getAttribute(inRequestKey)).andReturn(null);
        request.setAttribute(eq(inRequestKey), isA(InRequest.class));

        String objectGraphKey = OBJECT_GRAPH_KEY.toString();
        expect(request.getAttribute(objectGraphKey)).andReturn(null);
        request.setAttribute(eq(objectGraphKey), isA(ObjectGraph.class));
        expectLastCall();

        String inRequestNullKey = IN_REQUEST_NULLABLE_KEY.toString();
        expect(request.getAttribute(inRequestNullKey)).andReturn(null);
        request.setAttribute(eq(inRequestNullKey), eq(NullObject.INSTANCE));

        expect(request.getAttribute(objectGraphKey)).andReturn(null);
        request.setAttribute(eq(objectGraphKey), isA(ObjectGraph.class));
        expectLastCall();

        final boolean[] invoked = new boolean[1];
        FilterChain filterChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest,
                                 ServletResponse servletResponse) {
                invoked[0] = true;
                assertSame(request, servletRequest);
                assertNotNull(objectGraph.get(InRequest.class));
                assertNull(objectGraph.get(InRequestNullable.class));
            }
        };

        replay(request);

        filter.doFilter(request, null, filterChain);

        verify(request);
        assertTrue(invoked[0]);
    }

    public void testExistingRequestObject() throws IOException, ServletException {
        DaggerServletContextListener contextListener = new DaggerServletContextListener() {
            @Override
            protected Class<?>[] getBaseModules() {
                return new Class<?>[]{TestAppModule.class};
            }

            @Override
            protected Class<?>[] getRequestScopedModules() {
                return new Class<?>[]{TestRequestModule.class};
            }

            @Override
            protected void configureServlets() {
            }
        };

        ServletContext servletContext = createNiceMock(ServletContext.class);
        contextListener.contextInitialized(new ServletContextEvent(servletContext));
        final ObjectGraph objectGraph = contextListener.getObjectGraph();

        DaggerFilter filter = new DaggerFilter();

        final HttpServletRequest request = createMock(HttpServletRequest.class);

        final InRequest inRequest = new InRequest();
        String inRequestKey = IN_REQUEST_KEY.toString();
        expect(request.getAttribute(inRequestKey)).andReturn(inRequest).times(2);

        String inRequestNullKey = IN_REQUEST_NULLABLE_KEY.toString();
        expect(request.getAttribute(inRequestNullKey)).andReturn(NullObject.INSTANCE).times(2);

        final boolean[] invoked = new boolean[1];
        FilterChain filterChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest,
                                 ServletResponse servletResponse) {
                invoked[0] = true;

                assertSame(inRequest, objectGraph.get(InRequest.class));
                assertSame(inRequest, objectGraph.get(InRequest.class));

                assertNull(objectGraph.get(InRequestNullable.class));
                assertNull(objectGraph.get(InRequestNullable.class));
            }
        };

        replay(request);

        filter.doFilter(request, null, filterChain);

        verify(request);
        assertTrue(invoked[0]);
    }

    static class InRequest {
    }

    static class InRequestNullable {
    }
}
