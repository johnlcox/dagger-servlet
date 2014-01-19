/**
 * Copyright (C) 2008 Google Inc.
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dagger.ObjectGraph;
import org.testng.annotations.Test;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.leacox.dagger.servlet.ManagedServletPipeline.REQUEST_DISPATCHER_REQUEST;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests forwarding and inclusion (RequestDispatcher actions from the
 * servlet spec).
 *
 * @author Dhanji R. Prasanna (dhanji@gmail com)
 * @author John Leacox
 */
public class ServletPipelineRequestDispatcherTest {
    private static final Class<?> HTTP_SERLVET_CLASS = HttpServlet.class;
    private static final String A_KEY = "thinglyDEgintly" + new Date() + UUID.randomUUID();
    private static final String A_VALUE = ServletPipelineRequestDispatcherTest.class.toString()
            + new Date() + UUID.randomUUID();

    @Test
    public final void testIncludeManagedServlet() throws IOException, ServletException {
        String pattern = "blah.html";
        final ServletDefinition servletDefinition = new ServletDefinition(pattern,
                HttpServlet.class, UriPatternType.get(UriPatternType.SERVLET, pattern),
                new HashMap<String, String>(), null);

        ObjectGraph objectGraph = createMock(ObjectGraph.class);
        final HttpServletRequest requestMock = createMock(HttpServletRequest.class);

        expect(requestMock.getAttribute(A_KEY))
                .andReturn(A_VALUE);


        requestMock.setAttribute(REQUEST_DISPATCHER_REQUEST, true);
        requestMock.removeAttribute(REQUEST_DISPATCHER_REQUEST);

        final boolean[] run = new boolean[1];
        final HttpServlet mockServlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse httpServletResponse)
                    throws ServletException, IOException {
                run[0] = true;

                final Object o = request.getAttribute(A_KEY);
                assertEquals(o, A_VALUE, "Wrong attrib returned - " + o);
            }
        };

        expect(objectGraph.get(HTTP_SERLVET_CLASS))
                .andReturn(mockServlet);

        replay(objectGraph, requestMock);

        // Have to init the Servlet before we can dispatch to it.
        servletDefinition.init(null, objectGraph,
                Sets.newSetFromMap(Maps.<HttpServlet, Boolean>newIdentityHashMap()));

        final RequestDispatcher dispatcher = new ManagedServletPipeline(new ServletDefinition[]{servletDefinition})
                .getRequestDispatcher(pattern);

        assertNotNull(dispatcher);
        dispatcher.include(requestMock, createMock(HttpServletResponse.class));

        assertTrue(run[0], "Include did not dispatch to our servlet!");

        verify(objectGraph, requestMock);
    }

    @Test
    public final void testForwardToManagedServlet() throws IOException, ServletException {
        String pattern = "blah.html";
        final ServletDefinition servletDefinition = new ServletDefinition(pattern,
                HttpServlet.class, UriPatternType.get(UriPatternType.SERVLET, pattern),
                new HashMap<String, String>(), null);

        ObjectGraph objectGraph = createMock(ObjectGraph.class);
        final HttpServletRequest requestMock = createMock(HttpServletRequest.class);
        final HttpServletResponse mockResponse = createMock(HttpServletResponse.class);

        expect(requestMock.getAttribute(A_KEY))
                .andReturn(A_VALUE);

        requestMock.setAttribute(REQUEST_DISPATCHER_REQUEST, true);
        requestMock.removeAttribute(REQUEST_DISPATCHER_REQUEST);

        expect(mockResponse.isCommitted())
                .andReturn(false);

        mockResponse.resetBuffer();
        expectLastCall().once();

        final List<String> paths = new ArrayList<String>();
        final HttpServlet mockServlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse httpServletResponse)
                    throws ServletException, IOException {
                paths.add(request.getRequestURI());

                final Object o = request.getAttribute(A_KEY);
                assertEquals(o, A_VALUE, "Wrong attrib returned - " + o);
            }
        };

        expect(objectGraph.get(HTTP_SERLVET_CLASS))
                .andReturn(mockServlet);

        replay(objectGraph, requestMock, mockResponse);

        // Have to init the Servlet before we can dispatch to it.
        servletDefinition.init(null, objectGraph,
                Sets.newSetFromMap(Maps.<HttpServlet, Boolean>newIdentityHashMap()));

        final RequestDispatcher dispatcher = new ManagedServletPipeline(new ServletDefinition[]{servletDefinition})
                .getRequestDispatcher(pattern);

        assertNotNull(dispatcher);
        dispatcher.forward(requestMock, mockResponse);

        assertTrue(paths.contains(pattern), "Include did not dispatch to our servlet!");

        verify(objectGraph, requestMock, mockResponse);
    }

    @Test
    public final void testForwardToManagedServletFailureOnCommittedBuffer()
            throws IOException, ServletException {
        IllegalStateException expected = null;
        try {
            forwardToManagedServletFailureOnCommittedBuffer();
        } catch (IllegalStateException ise) {
            expected = ise;
        } finally {
            assertNotNull(expected, "Expected IllegalStateException was not thrown");
        }
    }

    public final void forwardToManagedServletFailureOnCommittedBuffer()
            throws IOException, ServletException {
        String pattern = "blah.html";
        final ServletDefinition servletDefinition = new ServletDefinition(pattern,
                HttpServlet.class, UriPatternType.get(UriPatternType.SERVLET, pattern),
                new HashMap<String, String>(), null);

        ObjectGraph objectGraph = createMock(ObjectGraph.class);
        final HttpServletRequest mockRequest = createMock(HttpServletRequest.class);
        final HttpServletResponse mockResponse = createMock(HttpServletResponse.class);

        expect(mockResponse.isCommitted())
                .andReturn(true);

        final HttpServlet mockServlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse httpServletResponse)
                    throws ServletException, IOException {

                final Object o = request.getAttribute(A_KEY);
                assertEquals(o, A_VALUE, "Wrong attrib returned - " + o);
            }
        };

        expect(objectGraph.get(HttpServlet.class))
                .andReturn(mockServlet);

        replay(objectGraph, mockRequest, mockResponse);

        // Have to init the Servlet before we can dispatch to it.
        servletDefinition.init(null, objectGraph,
                Sets.newSetFromMap(Maps.<HttpServlet, Boolean>newIdentityHashMap()));

        final RequestDispatcher dispatcher = new ManagedServletPipeline(new ServletDefinition[]{servletDefinition})
                .getRequestDispatcher(pattern);

        assertNotNull(dispatcher);

        try {
            dispatcher.forward(mockRequest, mockResponse);
        } finally {
            verify(objectGraph, mockRequest, mockResponse);
        }

    }
}
