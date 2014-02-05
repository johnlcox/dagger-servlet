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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import javax.inject.Singleton;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.google.common.collect.Maps;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import dagger.ScopingObjectGraph;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author crazybob@google.com (Bob Lee)
 * @author John Leacox
 */
public class ServletTest {
    private static final String SCOPED_OBJECT_GRAPH_KEY = ScopingObjectGraph.class.getName();
    private static final String IN_REQUEST_KEY = "DaggerKey[type=" + InRequest.class + "]";
    private static final String IN_SESSION_KEY = "DaggerKey[type=" + InSession.class + "]";
    //private static final Class<InRequest> IN_REQUEST_KEY = InRequest.class;
    //private static final Class<InRequest> IN_REQUEST_NULL_KEY = Key.get(InRequest.class, Null.class);
    //private static final Class<InSession> IN_SESSION_KEY = InSession.class;
    //private static final Class<InSession> IN_SESSION_NULL_KEY = Key.get(InSession.class, Null.class);

    @BeforeMethod
    public void setUp() {
        //we need to clear the reference to the pipeline every test =(
        DaggerFilter.reset();
    }

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
                    InRequest.class
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
    }

    @Module(
            injects = {
                    InSession.class
            },
            includes = {
            }
    )
    static class TestSessionModule {
        @Provides
        @Singleton
        InSession provideInSession() {
            return new InSession();
        }
    }

    @Test
    public void testNewRequestObject()
            throws IOException, ServletException {
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

//        final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
//        String inRequestKey = IN_REQUEST_KEY.toString();
//        expect(request.getAttribute(inRequestKey)).andReturn(null);
//        request.setAttribute(eq(inRequestKey), isA(InRequest.class));
//
//        String inRequestNullKey = IN_REQUEST_NULL_KEY.toString();
//        expect(request.getAttribute(inRequestNullKey)).andReturn(null);
//        request.setAttribute(eq(inRequestNullKey), eq(NullObject.INSTANCE));
//
//        final boolean[] invoked = new boolean[1];
//        FilterChain filterChain = new FilterChain() {
//            public void doFilter(ServletRequest servletRequest,
//                                 ServletResponse servletResponse) {
//                invoked[0] = true;
////        assertSame(request, servletRequest);
//                assertNotNull(injector.getInstance(InRequest.class));
//                assertNull(injector.getInstance(IN_REQUEST_NULL_KEY));
//            }
//        };
//
//        replay(request);
//
//        filter.doFilter(request, null, filterChain);
//
//        verify(request);
//        assertTrue(invoked[0]);
        final HttpServletRequest request = createNiceMock(HttpServletRequest.class);

        //String inRequestKey = //IN_REQUEST_KEY.toString();
        expect(request.getAttribute(IN_REQUEST_KEY)).andReturn(null);
        request.setAttribute(eq(IN_REQUEST_KEY), isA(InRequest.class));

//        String inRequestNullKey = IN_REQUEST_NULL_KEY.toString();
//        expect(request.getAttribute(inRequestNullKey)).andReturn(null);
//        request.setAttribute(eq(inRequestNullKey), eq(NullObject.INSTANCE));

        final boolean[] invoked = new boolean[1];
        FilterChain filterChain = new FilterChain() {
            public void doFilter(ServletRequest servletRequest,
                                 ServletResponse servletResponse) {
                invoked[0] = true;
//        assertSame(request, servletRequest);
                assertNotNull(objectGraph.get(InRequest.class));
                //assertNull(injector.getInstance(IN_REQUEST_NULL_KEY));
            }
        };

        replay(request);

        filter.doFilter(request, null, filterChain);

        verify(request);
        assertTrue(invoked[0]);
    }

    @Test
    public void testExistingRequestObject()
            throws IOException, ServletException {
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

//        final HttpServletRequest request = createMock(HttpServletRequest.class);
//        HttpSession session = createNiceMock(HttpSession.class);
//
//        expect(request.getSession()).andReturn(session).anyTimes();
//
//        ObjectGraph requestObjectGraph = ObjectGraph.create(TestAppModule.class, TestSessionModule.class,
//                TestRequestModule.class);
//
//        final InRequest inRequest = requestObjectGraph.get(InRequest.class);
//
//        expect(request.getAttribute(SCOPED_OBJECT_GRAPH_KEY)).andReturn(requestObjectGraph).times(2);
//
//
//        try {
//            objectGraph.get(InRequest.class);
//            fail("InRequest should only be bound inside of a request");
//        } catch (IllegalArgumentException e) { }
//
//        final boolean[] invoked = new boolean[1];
//        FilterChain filterChain = new FilterChain() {
//            @Override
//            public void doFilter(ServletRequest servletRequest,
//                                 ServletResponse servletResponse) {
//                invoked[0] = true;
//
//                assertSame(inRequest, objectGraph.get(InRequest.class));
//                assertSame(inRequest, objectGraph.get(InRequest.class));
//            }
//        };
//
//        try {
//            objectGraph.get(InRequest.class);
//            fail("InRequest should only be bound inside of a request");
//        } catch (IllegalArgumentException e) { }
//
//        replay(request, session);
//
//        filter.doFilter(request, null, filterChain);
//
//        verify(request);
//        assertTrue(invoked[0]);

        final HttpServletRequest request = createMock(HttpServletRequest.class);

        final InRequest inRequest = new InRequest();
        //String inRequestKey = IN_REQUEST_KEY.toString();
        expect(request.getAttribute(IN_REQUEST_KEY)).andReturn(inRequest).times(2);

//        String inRequestNullKey = IN_REQUEST_NULL_KEY.toString();
//        expect(request.getAttribute(inRequestNullKey)).andReturn(NullObject.INSTANCE).times(2);

        final boolean[] invoked = new boolean[1];
        FilterChain filterChain = new FilterChain() {
            public void doFilter(ServletRequest servletRequest,
                                 ServletResponse servletResponse) {
                invoked[0] = true;

                assertSame(inRequest, objectGraph.get(InRequest.class));
                assertSame(inRequest, objectGraph.get(InRequest.class));

//                assertNull(injector.getInstance(IN_REQUEST_NULL_KEY));
//                assertNull(injector.getInstance(IN_REQUEST_NULL_KEY));
            }
        };

        replay(request);

        filter.doFilter(request, null, filterChain);

        verify(request);
        assertTrue(invoked[0]);
    }

    @Test
    public void testNewSessionObject()
            throws IOException, ServletException {
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

//        final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
//        final HttpSession session = createMock(HttpSession.class);
//
//        expect(request.getSession()).andReturn(session).times(1);
//        expect(session.getAttribute(SCOPED_OBJECT_GRAPH_KEY)).andReturn(null);
//        session.setAttribute(eq(SCOPED_OBJECT_GRAPH_KEY), isA(ObjectGraph.class));
//
//        final boolean[] invoked = new boolean[1];
//        FilterChain filterChain = new FilterChain() {
//            @Override
//            public void doFilter(ServletRequest servletRequest,
//                                 ServletResponse servletResponse) {
//                invoked[0] = true;
////        assertSame(request, servletRequest);
//                assertNotNull(objectGraph.get(InSession.class));
//            }
//        };
//
//        replay(request, session);
//
//        filter.doFilter(request, null, filterChain);
//
//        verify(request, session);
//        assertTrue(invoked[0]);

        final HttpServletRequest request = createMock(HttpServletRequest.class);
        final HttpSession session = createNiceMock(HttpSession.class);

//        String inSessionKey = IN_SESSION_KEY.toString();
//        String inSessionNullKey = IN_SESSION_NULL_KEY.toString();

        expect(request.getSession()).andReturn(session).times(1);
        expect(session.getAttribute(IN_SESSION_KEY)).andReturn(null);
        session.setAttribute(eq(IN_SESSION_KEY), isA(InSession.class));

//        expect(session.getAttribute(inSessionNullKey)).andReturn(null);
//        session.setAttribute(eq(inSessionNullKey), eq(NullObject.INSTANCE));

        final boolean[] invoked = new boolean[1];
        FilterChain filterChain = new FilterChain() {
            public void doFilter(ServletRequest servletRequest,
                                 ServletResponse servletResponse) {
                invoked[0] = true;
//        assertSame(request, servletRequest);
                assertNotNull(objectGraph.get(InSession.class));
                //assertNull(injector.getInstance(IN_SESSION_NULL_KEY));
            }
        };

        replay(request, session);

        filter.doFilter(request, null, filterChain);

        verify(request, session);
        assertTrue(invoked[0]);
    }

    @Test
    public void testExistingSessionObject()
            throws IOException, ServletException {
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

//        final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
//        final HttpSession session = createMock(HttpSession.class);
//
//        ObjectGraph sessionObjectGraph = ObjectGraph.create(TestAppModule.class, TestSessionModule.class);
//
//        final InSession inSession = sessionObjectGraph.get(InSession.class);
//
//        expect(request.getSession()).andReturn(session).times(2);
//        expect(session.getAttribute(SCOPED_OBJECT_GRAPH_KEY)).andReturn(sessionObjectGraph).times(2);
//        //expect(session.getAttribute(inSessionKey)).andReturn(inSession).times(2);
//
//        //expect(session.getAttribute(inSessionNullKey)).andReturn(NullObject.INSTANCE).times(2);
//
//        try {
//            objectGraph.get(InSession.class);
//            fail("InSession should only be bound inside of a session");
//        } catch (IllegalArgumentException e) { }
//
//        final boolean[] invoked = new boolean[1];
//        FilterChain filterChain = new FilterChain() {
//            @Override
//            public void doFilter(ServletRequest servletRequest,
//                                 ServletResponse servletResponse) {
//                invoked[0] = true;
////        assertSame(request, servletRequest);
//
//                assertSame(inSession, objectGraph.get(InSession.class));
//                assertSame(inSession, objectGraph.get(InSession.class));
//
//                //assertNull(injector.getInstance(IN_SESSION_NULL_KEY));
//                //assertNull(injector.getInstance(IN_SESSION_NULL_KEY));
//            }
//        };
//
//        try {
//            objectGraph.get(InSession.class);
//            fail("InSession should only be bound inside of a session");
//        } catch (IllegalArgumentException e) { }
//
//        replay(request, session);
//
//        filter.doFilter(request, null, filterChain);
//
//        verify(request, session);
//        assertTrue(invoked[0]);

        final HttpServletRequest request = createMock(HttpServletRequest.class);
        final HttpSession session = createMock(HttpSession.class);

//        String inSessionKey = IN_SESSION_KEY.toString();
//        String inSessionNullKey = IN_SESSION_NULL_KEY.toString();

        final InSession inSession = new InSession();
        expect(request.getSession()).andReturn(session).times(2);
        expect(session.getAttribute(IN_SESSION_KEY)).andReturn(inSession).times(2);

//        expect(session.getAttribute(inSessionNullKey)).andReturn(NullObject.INSTANCE).times(2);

        final boolean[] invoked = new boolean[1];
        FilterChain filterChain = new FilterChain() {
            public void doFilter(ServletRequest servletRequest,
                                 ServletResponse servletResponse) {
                invoked[0] = true;
//        assertSame(request, servletRequest);

                assertSame(inSession, objectGraph.get(InSession.class));
                assertSame(inSession, objectGraph.get(InSession.class));

//                assertNull(injector.getInstance(IN_SESSION_NULL_KEY));
//                assertNull(injector.getInstance(IN_SESSION_NULL_KEY));
            }
        };

        replay(request, session);

        filter.doFilter(request, null, filterChain);

        verify(request, session);
        assertTrue(invoked[0]);
    }

    @Test(enabled = false) // Scoping requires storing the object graph somewhere with the same lifetime of the scope.
    // That object for session scope is the session object, but the object graph is not serializable, so session
    // scope probably needs to be removed.
    public void testHttpSessionIsSerializable()
            throws IOException, ClassNotFoundException, ServletException {
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

//        final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
//        final HttpSession session = newFakeHttpSession();
//
//        //String inSessionKey = IN_SESSION_KEY.toString();
//        //String inSessionNullKey = IN_SESSION_NULL_KEY.toString();
//
//        expect(request.getSession()).andReturn(session).times(1);
//
//        final boolean[] invoked = new boolean[1];
//        FilterChain filterChain = new FilterChain() {
//            @Override
//            public void doFilter(ServletRequest servletRequest,
//                                 ServletResponse servletResponse) {
//                invoked[0] = true;
//                assertNotNull(objectGraph.get(InSession.class));
//                //assertNull(injector.getInstance(IN_SESSION_NULL_KEY));
//            }
//        };
//
//        replay(request);
//
//        filter.doFilter(request, null, filterChain);
//
//        verify(request);
//        assertTrue(invoked[0]);
//
//        HttpSession deserializedSession = reserialize(session);
//
//        assertTrue(deserializedSession.getAttribute(SCOPED_OBJECT_GRAPH_KEY) instanceof ObjectGraph);
//        //assertEquals(NullObject.INSTANCE, deserializedSession.getAttribute(inSessionNullKey));
        final HttpServletRequest request = createMock(HttpServletRequest.class);
        final HttpSession session = newFakeHttpSession();

        String inSessionKey = "DaggerKey[type=" + InSession.class + "]"; //IN_SESSION_KEY.toString();
        //String inSessionNullKey = IN_SESSION_NULL_KEY.toString();

        expect(request.getSession()).andReturn(session).times(1);

        final boolean[] invoked = new boolean[1];
        FilterChain filterChain = new FilterChain() {
            public void doFilter(ServletRequest servletRequest,
                                 ServletResponse servletResponse) {
                invoked[0] = true;
                assertNotNull(objectGraph.get(InSession.class));
                //assertNull(injector.getInstance(IN_SESSION_NULL_KEY));
            }
        };

        replay(request);

        filter.doFilter(request, null, filterChain);

        verify(request);
        assertTrue(invoked[0]);

        HttpSession deserializedSession = reserialize(session);

        assertTrue(deserializedSession.getAttribute(inSessionKey) instanceof InSession);
        //assertEquals(NullObject.INSTANCE, deserializedSession.getAttribute(inSessionNullKey));
    }


    private static class FakeHttpSessionHandler implements InvocationHandler, Serializable {
        final Map<String, Object> attributes = Maps.newHashMap();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("setAttribute".equals(name)) {
                attributes.put((String) args[0], args[1]);
                return null;
            } else if ("getAttribute".equals(name)) {
                return attributes.get(args[0]);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * Returns a fake, serializable HttpSession which stores attributes in a HashMap.
     */
    private HttpSession newFakeHttpSession() {
        return (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(),
                new Class[]{HttpSession.class}, new FakeHttpSessionHandler());
    }

//    private Injector createInjector() throws CreationException {
//
//        return Guice.createInjector(new AbstractModule() {
//
//            @Override
//            protected void configure() {
//                install(new ServletModule());
//                bind(InSession.class);
//                bind(IN_SESSION_NULL_KEY).toProvider(Providers.<InSession>of(null)).in(SessionScoped.class);
//                bind(InRequest.class);
//                bind(IN_REQUEST_NULL_KEY).toProvider(Providers.<InRequest>of(null)).in(RequestScoped.class);
//            }
//        });
//    }

    //
//    @SessionScoped
    static class InSession implements Serializable {
    }

    //
    //@RequestScoped
    static class InRequest {
    }

    public static <E> E reserialize(E original) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new ObjectOutputStream(out).writeObject(original);
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            @SuppressWarnings("unchecked") // the reserialized type is assignable
                    E reserialized = (E) new ObjectInputStream(in).readObject();
            return reserialized;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

//    @BindingAnnotation
//    @Retention(RUNTIME)
//    @Target({PARAMETER, METHOD, FIELD})
//    @interface Null {
//    }
}
