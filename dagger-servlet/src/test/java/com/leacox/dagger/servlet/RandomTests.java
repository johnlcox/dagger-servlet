package com.leacox.dagger.servlet;

import dagger.Module;
import dagger.ObjectGraph;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void testRequestInjection() throws IOException, ServletException {
        ObjectGraph objectGraph = ObjectGraph.create(new ServletModule());
        objectGraph.get(InternalServletModule.ObjectGraphProvider.class).set(objectGraph);

        ServletRequest expectedRequest = mock(ServletRequest.class);
        ServletResponse expectedResponse = mock(ServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        // TODO: Why does this throw a StackOverFlowError?
        DaggerFilter daggerFilter = objectGraph.get(DaggerFilter.class);
        daggerFilter.doFilter(expectedRequest, expectedResponse, filterChain);

        ObjectGraph requestGraph = objectGraph.plus(new ServletRequestModule());

        ServletRequest request = requestGraph.get(ServletRequest.class);
        ServletResponse response = requestGraph.get(ServletResponse.class);

        assertEquals(expectedRequest, request);
        assertEquals(expectedResponse, response);
    }
}
