package com.leacox.dagger.servlet;

import dagger.Module;
import dagger.ObjectGraph;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author John Leacox
 */
public class RandomTests {
    @Test
    public void testObjectGraphInjection() {
        ObjectGraph objectGraph = ObjectGraph.create(new ServletModule());
        objectGraph.get(InternalServletModule.ObjectGraphProvider.class).set(objectGraph);

        FilterPipeline filterPipeline = objectGraph.get(FilterPipeline.class);

        assertEquals(DaggerFilterPipeline.class, filterPipeline.getClass());
    }

//    @Test
//    public void testRequestInjection() throws IOException, ServletException {
//        ObjectGraph objectGraph = ObjectGraph.create(new ServletModule());
//        objectGraph.get(InternalServletModule.ObjectGraphProvider.class).set(objectGraph);
//
//        ServletRequest expectedRequest = mock(HttpServletRequest.class);
//        ServletResponse expectedResponse = mock(HttpServletResponse.class);
//        FilterChain filterChain = mock(FilterChain.class);
//
//        // TODO: Why does this throw a StackOverFlowError?
//        DaggerFilter daggerFilter = objectGraph.get(DaggerFilter.class);
//        daggerFilter.doFilter(expectedRequest, expectedResponse, filterChain);
//
//        ObjectGraph requestGraph = objectGraph.plus(new ServletRequestModule());
//
//        ServletRequest request = requestGraph.get(ServletRequest.class);
//        ServletResponse response = requestGraph.get(ServletResponse.class);
//
//        assertEquals(expectedRequest, request);
//        assertEquals(expectedResponse, response);
//    }

    @Test
    public void testRequestAndResponseBindings() throws Exception {
        ObjectGraph objectGraph = ObjectGraph.create(new ServletModule());
        objectGraph.get(InternalServletModule.ObjectGraphProvider.class).set(objectGraph);

        final ServletRequest request = mock(HttpServletRequest.class);
        final ServletResponse response = mock(HttpServletResponse.class);

        final boolean[] invoked = new boolean[1];

        DaggerFilter filter = objectGraph.get(DaggerFilter.class);
        final ObjectGraph requestGraph = objectGraph.plus(new ServletRequestModule());
        FilterChain filterChain = new FilterChain() {
            public void doFilter(ServletRequest servletRequest,
                                 ServletResponse servletResponse) {
                invoked[0] = true;
                assertSame(request, servletRequest);
                assertSame(request, requestGraph.get(ServletRequest.class));

                assertSame(response, servletResponse);
                assertSame(response, requestGraph.get(ServletResponse.class));
            }
        };
        filter.doFilter(request, response, filterChain);

        assertTrue(invoked[0]);
    }
}
