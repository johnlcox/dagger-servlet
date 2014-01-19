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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

import static com.leacox.dagger.servlet.ManagedServletPipeline.REQUEST_DISPATCHER_REQUEST;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Ensures servlet spec compliance for CGI-style variables and general
 * path/pattern matching.
 *
 * @author Dhanji R. Prasanna (dhanji@gmail com)
 * @author John Leacox
 */
public class ServletDefinitionPathsTest {

    // Data-driven test.
    @Test
    public final void testServletPathMatching() throws IOException, ServletException {
        servletPath("/index.html", "*.html", "/index.html");
        servletPath("/somewhere/index.html", "*.html", "/somewhere/index.html");
        servletPath("/somewhere/index.html", "/*", "");
        servletPath("/index.html", "/*", "");
        servletPath("/", "/*", "");
        servletPath("//", "/*", "");
        servletPath("/////", "/*", "");
        servletPath("", "/*", "");
        servletPath("/thing/index.html", "/thing/*", "/thing");
        servletPath("/thing/wing/index.html", "/thing/*", "/thing");
    }

    private void servletPath(final String requestPath, String mapping,
                             final String expectedServletPath) throws IOException, ServletException {
        ObjectGraph objectGraph = createMock(ObjectGraph.class);

        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);

        final boolean[] run = new boolean[1];
        //get an instance of this servlet
        expect(objectGraph.get(HttpServlet.class))
                .andReturn(new HttpServlet() {

                    @Override
                    protected void service(HttpServletRequest servletRequest,
                                           HttpServletResponse httpServletResponse) throws ServletException, IOException {

                        final String path = servletRequest.getServletPath();
                        assertEquals(path, expectedServletPath,
                                String.format("expected [%s] but was [%s]", expectedServletPath, path));
                        run[0] = true;
                    }
                });

        expect(request.getServletPath())
                .andReturn(requestPath);

        replay(objectGraph, request);

        ServletDefinition servletDefinition = new ServletDefinition(mapping, HttpServlet.class,
                UriPatternType.get(UriPatternType.SERVLET, mapping), new HashMap<String, String>(), null);

        servletDefinition.init(null, objectGraph,
                Sets.newSetFromMap(Maps.<HttpServlet, Boolean>newIdentityHashMap()));
        servletDefinition.doService(request, response);

        assertTrue(run[0], "Servlet did not run!");

        verify(objectGraph, request);

    }

    // Data-driven test.
    @Test
    public final void testPathInfoWithServletStyleMatching() throws IOException, ServletException {
        pathInfoWithServletStyleMatching("/path/index.html", "/path", "/*", "/index.html", "");
        pathInfoWithServletStyleMatching("/path//hulaboo///index.html", "/path", "/*",
                "/hulaboo/index.html", "");
        pathInfoWithServletStyleMatching("/path/", "/path", "/*", "/", "");
        pathInfoWithServletStyleMatching("/path////////", "/path", "/*", "/", "");

        // a servlet mapping of /thing/*
        pathInfoWithServletStyleMatching("/path/thing////////", "/path", "/thing/*", "/", "/thing");
        pathInfoWithServletStyleMatching("/path/thing/stuff", "/path", "/thing/*", "/stuff", "/thing");
        pathInfoWithServletStyleMatching("/path/thing/stuff.html", "/path", "/thing/*", "/stuff.html",
                "/thing");
        pathInfoWithServletStyleMatching("/path/thing", "/path", "/thing/*", null, "/thing");

        // *.xx style mapping
        pathInfoWithServletStyleMatching("/path/thing.thing", "/path", "*.thing", null, "/thing.thing");
        pathInfoWithServletStyleMatching("/path///h.thing", "/path", "*.thing", null, "/h.thing");
        pathInfoWithServletStyleMatching("/path///...//h.thing", "/path", "*.thing", null,
                "/.../h.thing");
        pathInfoWithServletStyleMatching("/path/my/h.thing", "/path", "*.thing", null, "/my/h.thing");

    }

    private void pathInfoWithServletStyleMatching(final String requestUri, final String contextPath,
                                                  String mapping, final String expectedPathInfo, final String servletPath)
            throws IOException, ServletException {

        ObjectGraph objectGraph = createMock(ObjectGraph.class);

        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);

        final boolean[] run = new boolean[1];
        //get an instance of this servlet
        expect(objectGraph.get(HttpServlet.class))
                .andReturn(new HttpServlet() {

                    @Override
                    protected void service(HttpServletRequest servletRequest,
                                           HttpServletResponse httpServletResponse) throws ServletException, IOException {

                        final String path = servletRequest.getPathInfo();

                        if (null == expectedPathInfo) {
                            assertNull(path, String.format("expected [%s] but was [%s]", expectedPathInfo, path));
                        } else {
                            assertEquals(path, expectedPathInfo,
                                    String.format("expected [%s] but was [%s]", expectedPathInfo, path));
                        }

                        //assert memoizer
                        //noinspection StringEquality
                        assertSame(servletRequest.getPathInfo(), path, "memo field did not work");

                        run[0] = true;
                    }
                });

        expect(request.getRequestURI())
                .andReturn(requestUri);

        expect(request.getServletPath())
                .andReturn(servletPath)
                .anyTimes();

        expect(request.getContextPath())
                .andReturn(contextPath);

        expect(request.getAttribute(REQUEST_DISPATCHER_REQUEST)).andReturn(null);

        replay(objectGraph, request);

        ServletDefinition servletDefinition = new ServletDefinition(mapping, HttpServlet.class,
                UriPatternType.get(UriPatternType.SERVLET, mapping), new HashMap<String, String>(), null);

        servletDefinition.init(null, objectGraph,
                Sets.newSetFromMap(Maps.<HttpServlet, Boolean>newIdentityHashMap()));
        servletDefinition.doService(request, response);

        assertTrue(run[0], "Servlet did not run!");

        verify(objectGraph, request);
    }

    // Data-driven test.
    @Test
    public final void testPathInfoWithRegexMatching() throws IOException, ServletException {
        // first a mapping of /*
        pathInfoWithRegexMatching("/path/index.html", "/path", "/(.)*", "/index.html", "");
        pathInfoWithRegexMatching("/path//hulaboo///index.html", "/path", "/(.)*",
                "/hulaboo/index.html", "");
        pathInfoWithRegexMatching("/path/", "/path", "/(.)*", "/", "");
        pathInfoWithRegexMatching("/path////////", "/path", "/(.)*", "/", "");

        // a servlet mapping of /thing/*
        pathInfoWithRegexMatching("/path/thing////////", "/path", "/thing/(.)*", "/", "/thing");
        pathInfoWithRegexMatching("/path/thing/stuff", "/path", "/thing/(.)*", "/stuff", "/thing");
        pathInfoWithRegexMatching("/path/thing/stuff.html", "/path", "/thing/(.)*", "/stuff.html",
                "/thing");
        pathInfoWithRegexMatching("/path/thing", "/path", "/thing/(.)*", null, "/thing");

        // *.xx style mapping
        pathInfoWithRegexMatching("/path/thing.thing", "/path", ".*\\.thing", null, "/thing.thing");
        pathInfoWithRegexMatching("/path///h.thing", "/path", ".*\\.thing", null, "/h.thing");
        pathInfoWithRegexMatching("/path///...//h.thing", "/path", ".*\\.thing", null,
                "/.../h.thing");
        pathInfoWithRegexMatching("/path/my/h.thing", "/path", ".*\\.thing", null, "/my/h.thing");

        // path
        pathInfoWithRegexMatching("/path/test.com/com.test.MyServletModule", "", "/path/[^/]+/(.*)",
                "com.test.MyServletModule", "/path/test.com/com.test.MyServletModule");
    }

    public final void pathInfoWithRegexMatching(final String requestUri, final String contextPath,
                                                String mapping, final String expectedPathInfo, final String servletPath)
            throws IOException, ServletException {
        ObjectGraph objectGraph = createMock(ObjectGraph.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);

        final boolean[] run = new boolean[1];
        //get an instance of this servlet
        expect(objectGraph.get(HttpServlet.class))
                .andReturn(new HttpServlet() {

                    @Override
                    protected void service(HttpServletRequest servletRequest,
                                           HttpServletResponse httpServletResponse) throws ServletException, IOException {

                        final String path = servletRequest.getPathInfo();

                        if (null == expectedPathInfo) {
                            assertNull(path, String.format("expected [%s] but was [%s]", expectedPathInfo, path));
                        } else {
                            assertEquals(path, expectedPathInfo,
                                    String.format("expected [%s] but was [%s]", expectedPathInfo, path));
                        }

                        //assert memoizer
                        //noinspection StringEquality
                        assertSame(servletRequest.getPathInfo(), path, "memo field did not work");

                        run[0] = true;
                    }
                });

        expect(request.getRequestURI())
                .andReturn(requestUri);

        expect(request.getServletPath())
                .andReturn(servletPath)
                .anyTimes();

        expect(request.getContextPath())
                .andReturn(contextPath);

        expect(request.getAttribute(REQUEST_DISPATCHER_REQUEST)).andReturn(null);

        replay(objectGraph, request);

        ServletDefinition servletDefinition = new ServletDefinition(mapping, HttpServlet.class,
                UriPatternType.get(UriPatternType.REGEX, mapping), new HashMap<String, String>(), null);

        servletDefinition.init(null, objectGraph,
                Sets.newSetFromMap(Maps.<HttpServlet, Boolean>newIdentityHashMap()));
        servletDefinition.doService(request, response);

        assertTrue(run[0], "Servlet did not run!");

        verify(objectGraph, request);
    }
}
