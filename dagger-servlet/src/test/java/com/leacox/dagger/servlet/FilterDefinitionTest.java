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
 *      Copyright (C) 2008 Google Inc.
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dagger.ObjectGraph;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests the lifecycle of the encapsulated {@link FilterDefinition} class.
 *
 * @author Dhanji R. Prasanna (dhanji@gmail com)
 * @author John Leacox
 */
public class FilterDefinitionTest {
    @Test
    public final void testFilterInitAndConfig() throws ServletException {
        ObjectGraph objectGraph = createMock(ObjectGraph.class);

        final MockFilter mockFilter = new MockFilter();

        expect(objectGraph.get(Filter.class))
                .andReturn(mockFilter)
                .anyTimes();

        replay(objectGraph);

        //some init params
        //noinspection SSBasedInspection
        final Map<String, String> initParams = new HashMap<String, String>() {{
            put("ahsd", "asdas24dok");
            put("ahssd", "asdasd124ok");
            put("ahfsasd", "asda124sdok");
            put("ahsasgd", "a124sdasdok");
            put("ahsd124124", "as124124124dasdok");
        }};


        ServletContext servletContext = createMock(ServletContext.class);
        final String contextName = "thing__!@@44";
        expect(servletContext.getServletContextName()).andReturn(contextName);

        replay(servletContext);

        String pattern = "/*";
        final FilterDefinition filterDef = new FilterDefinition(pattern, Filter.class,
                UriPatternType.get(UriPatternType.SERVLET, pattern), initParams, null);
        filterDef.init(servletContext, objectGraph,
                Sets.newSetFromMap(Maps.<Filter, Boolean>newIdentityHashMap()));

        assertTrue(filterDef.getFilter() instanceof MockFilter);
        final FilterConfig filterConfig = mockFilter.getConfig();
        assertNotNull(filterConfig);
        assertEquals(filterConfig.getServletContext().getServletContextName(), contextName);
        assertEquals(filterConfig.getFilterName(), Filter.class.getCanonicalName());

        final Enumeration names = filterConfig.getInitParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();

            assertTrue(initParams.containsKey(name));
            assertTrue(initParams.get(name).equals(filterConfig.getInitParameter(name)));
        }

        verify(objectGraph, servletContext);
    }

    @Test
    public final void testFilterInitFilterInstance() throws ServletException {
        ObjectGraph objectGraph = createStrictMock(ObjectGraph.class);

        final MockFilter mockFilter = new MockFilter();

        replay(objectGraph);

        //some init params
        //noinspection SSBasedInspection
        final Map<String, String> initParams = new HashMap<String, String>() {{
            put("ahsd", "asdas24dok");
            put("ahssd", "asdasd124ok");
            put("ahfsasd", "asda124sdok");
            put("ahsasgd", "a124sdasdok");
            put("ahsd124124", "as124124124dasdok");
        }};


        ServletContext servletContext = createMock(ServletContext.class);
        final String contextName = "thing__!@@44";
        expect(servletContext.getServletContextName()).andReturn(contextName);

        replay(servletContext);

        String pattern = "/*";
        final FilterDefinition filterDef = new FilterDefinition(pattern, Filter.class,
                UriPatternType.get(UriPatternType.SERVLET, pattern), initParams, mockFilter);
        filterDef.init(servletContext, objectGraph,
                Sets.newSetFromMap(Maps.<Filter, Boolean>newIdentityHashMap()));

        assertTrue(filterDef.getFilter() instanceof MockFilter);
        assertEquals(filterDef.getFilter(), mockFilter);
        final FilterConfig filterConfig = mockFilter.getConfig();
        assertNotNull(filterConfig);
        assertEquals(filterConfig.getServletContext().getServletContextName(), contextName);
        assertEquals(filterConfig.getFilterName(), Filter.class.getCanonicalName());

        final Enumeration names = filterConfig.getInitParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();

            assertTrue(initParams.containsKey(name));
            assertTrue(initParams.get(name).equals(filterConfig.getInitParameter(name)));
        }

        verify(objectGraph, servletContext);
    }

    @Test
    public final void testFilterCreateDispatchDestroy() throws ServletException, IOException {
        ObjectGraph objectGraph = createMock(ObjectGraph.class);

        HttpServletRequest request = createMock(HttpServletRequest.class);

        final MockFilter mockFilter = new MockFilter();

        expect(objectGraph.get(Filter.class))
                .andReturn(mockFilter)
                .anyTimes();

        expect(request.getRequestURI()).andReturn("/index.html");
        expect(request.getContextPath())
                .andReturn("")
                .anyTimes();

        replay(objectGraph, request);

        String pattern = "/*";
        final FilterDefinition filterDef = new FilterDefinition(pattern, Filter.class,
                UriPatternType.get(UriPatternType.SERVLET, pattern), new HashMap<String, String>(), null);
        //should fire on mockfilter now
        filterDef.init(createMock(ServletContext.class), objectGraph,
                Sets.newSetFromMap(Maps.<Filter, Boolean>newIdentityHashMap()));
        assertTrue(filterDef.getFilter() instanceof MockFilter);

        assertTrue(mockFilter.isInit(), "Init did not fire");

        final boolean proceed[] = new boolean[1];
        filterDef.doFilter(request, null, new FilterChainInvocation(null, null, null) {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
                    throws IOException, ServletException {

                proceed[0] = true;
            }
        });

        assertTrue(proceed[0], "Filter did not proceed down chain");

        filterDef.destroy(Sets.newSetFromMap(Maps.<Filter, Boolean>newIdentityHashMap()));
        assertTrue(mockFilter.isDestroy(), "Destroy did not fire");

        verify(objectGraph, request);

    }

    @Test
    public final void testFilterCreateDispatchDestroySupressChain()
            throws ServletException, IOException {
        ObjectGraph objectGraph = createMock(ObjectGraph.class);

        HttpServletRequest request = createMock(HttpServletRequest.class);

        final MockFilter mockFilter = new MockFilter() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                                 FilterChain filterChain) throws IOException, ServletException {
                setRun(true);

                //suppress rest of chain...
            }
        };

        expect(objectGraph.get(Filter.class))
                .andReturn(mockFilter)
                .anyTimes();

        expect(request.getRequestURI()).andReturn("/index.html");
        expect(request.getContextPath())
                .andReturn("")
                .anyTimes();

        replay(objectGraph, request);

        String pattern = "/*";
        final FilterDefinition filterDef = new FilterDefinition(pattern, Filter.class,
                UriPatternType.get(UriPatternType.SERVLET, pattern), new HashMap<String, String>(), null);
        //should fire on mockfilter now
        filterDef.init(createMock(ServletContext.class), objectGraph,
                Sets.newSetFromMap(Maps.<Filter, Boolean>newIdentityHashMap()));
        assertTrue(filterDef.getFilter() instanceof MockFilter);


        assertTrue(mockFilter.isInit(), "init did not fire");

        final boolean proceed[] = new boolean[1];
        filterDef.doFilter(request, null, new FilterChainInvocation(null, null, null) {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
                    throws IOException, ServletException {
                proceed[0] = true;
            }
        });

        assertTrue(!proceed[0], "filter did not suppress chain");

        filterDef.destroy(Sets.newSetFromMap(Maps.<Filter, Boolean>newIdentityHashMap()));
        assertTrue(mockFilter.isDestroy(), "destroy did not fire");

        verify(objectGraph, request);

    }

    private static class MockFilter implements Filter {
        private boolean init;
        private boolean destroy;
        private boolean run;
        private FilterConfig config;

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            init = true;

            this.config = filterConfig;
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                             FilterChain filterChain) throws IOException, ServletException {
            run = true;

            //proceed
            filterChain.doFilter(servletRequest, servletResponse);
        }

        protected void setRun(boolean run) {
            this.run = run;
        }

        @Override
        public void destroy() {
            destroy = true;
        }

        public boolean isInit() {
            return init;
        }

        public boolean isDestroy() {
            return destroy;
        }

        public boolean isRun() {
            return run;
        }

        public FilterConfig getConfig() {
            return config;
        }
    }
}
