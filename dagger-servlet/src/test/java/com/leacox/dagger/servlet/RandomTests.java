package com.leacox.dagger.servlet;

import dagger.Module;
import dagger.Provides;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author John Leacox
 */
public class RandomTests {
//    @Before
//    public void beforeTest() {
//        DaggerFilter.reset();
//    }

//    @Test
//    public void testObjectGraphInjection() {
//        ObjectGraph objectGraph = ObjectGraph.create(new ServletModule());
//        objectGraph.get(InternalServletModule.ObjectGraphProvider.class).set(objectGraph);
//
//        FilterPipeline filterPipeline = objectGraph.get(FilterPipeline.class);
//
//        assertEquals(ManagedFilterPipeline.class, filterPipeline.getClass());
//    }

//    @Test
//    public void testRequestAndResponseBindings() throws Exception {
//        ObjectGraph objectGraph = ObjectGraph.create(new ServletModule());
//        objectGraph.get(InternalServletModule.ObjectGraphProvider.class).set(objectGraph);
//        objectGraph.get(InternalServletModule.FilterDefinitionsProvider.class).set(new FilterDefinition[0]);
//
//        final ServletRequest request = mock(HttpServletRequest.class);
//        final ServletResponse response = mock(HttpServletResponse.class);
//
//        final boolean[] invoked = new boolean[1];
//
//        DaggerFilter filter = objectGraph.get(DaggerFilter.class);
//        final ObjectGraph requestGraph = objectGraph.plus(new ServletRequestModule());
//        FilterChain filterChain = new FilterChain() {
//            public void doFilter(ServletRequest servletRequest,
//                                 ServletResponse servletResponse) {
//                invoked[0] = true;
//                assertSame(request, servletRequest);
//                assertSame(request, requestGraph.get(ServletRequest.class));
//
//                assertSame(response, servletResponse);
//                assertSame(response, requestGraph.get(ServletResponse.class));
//            }
//        };
//        filter.doFilter(request, response, filterChain);
//
//        assertTrue(invoked[0]);
//    }

//    @Test
//    public void testRequestScopedInjectionSame() throws IOException, ServletException {
//        final ScopingObjectGraph objectGraph = ScopingObjectGraph.create(ObjectGraph.create(new ServletModule()))
//                .addScopedModules(RequestScoped.class, new ServletRequestModule(), new RequestScopedModule());
//        objectGraph.get(InternalServletModule.ObjectGraphProvider.class).set(objectGraph);
//        objectGraph.get(InternalServletModule.FilterDefinitionsProvider.class).set(new FilterDefinition[0]);
//
//        final ServletRequest request = ServletTestUtils.newFakeHttpServletRequest();
//        final ServletResponse response = ServletTestUtils.newFakeHttpServletResponse();
//
//        final boolean[] invoked = new boolean[1];
//
//        DaggerFilter filter = objectGraph.get(DaggerFilter.class);
//
//        FilterChain filterChain = new FilterChain() {
//            public void doFilter(ServletRequest servletRequest,
//                                 ServletResponse servletResponse) {
//                invoked[0] = true;
//
//                RequestFoo fooOne = objectGraph.get(RequestFoo.class);
//                RequestFoo fooTwo = objectGraph.get(RequestFoo.class);
//
//                assertSame(fooOne, fooTwo);
//            }
//        };
//        filter.doFilter(request, response, filterChain);
//
//        assertTrue(invoked[0]);
//    }

//    @Test
//    public void testRequestScopedInjectionSeparateRequests() throws IOException, ServletException {
//        final ScopingObjectGraph objectGraph = ScopingObjectGraph.create(ObjectGraph.create(new ServletModule()))
//                .addScopedModules(RequestScoped.class, new ServletRequestModule(), new RequestScopedModule());
//        objectGraph.get(InternalServletModule.ObjectGraphProvider.class).set(objectGraph);
//        objectGraph.get(InternalServletModule.FilterDefinitionsProvider.class).set(new FilterDefinition[0]);
//
//        final RequestFoo[] foos = new RequestFoo[2];
//
//        DaggerFilter filter = objectGraph.get(DaggerFilter.class);
//
//        final ServletRequest requestOne = ServletTestUtils.newFakeHttpServletRequest();
//        final ServletResponse responseOne = ServletTestUtils.newFakeHttpServletResponse();
//        FilterChain filterChainOne = new FilterChain() {
//            public void doFilter(ServletRequest servletRequest,
//                                 ServletResponse servletResponse) {
//                foos[0] = objectGraph.get(RequestFoo.class);
//            }
//        };
//        filter.doFilter(requestOne, responseOne, filterChainOne);
//
//        final ServletRequest requestTwo = ServletTestUtils.newFakeHttpServletRequest();
//        final ServletResponse responseTwo = ServletTestUtils.newFakeHttpServletResponse();
//        FilterChain filterChainTwo = new FilterChain() {
//            public void doFilter(ServletRequest servletRequest,
//                                 ServletResponse servletResponse) {
//                foos[1] = objectGraph.get(RequestFoo.class);
//            }
//        };
//        filter.doFilter(requestTwo, responseTwo, filterChainTwo);
//
//        assertNotSame(foos[0], foos[1]);
//    }

    @Singleton
    static class RequestFoo {
        private final String foo;

        @Inject
        RequestFoo(String foo) {
            this.foo = foo;
        }

        public String getFoo() {
            return foo;
        }
    }

    @Module(
            injects = {
                    RequestFoo.class
            }
//            },
//            addsTo = ServletModule.class,
//            includes = {
//                    InternalServletRequestModule.class
//            }
    )
    static class RequestScopedModule {
        @Provides
        public String provideFoo() {
            return "foo";
        }

    }
}
