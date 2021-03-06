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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Basic unit test for lifecycle of a ServletDefinition (wrapper).
 *
 * @author Dhanji R. Prasanna (dhanji@gmail com)
 * @author John Leacox
 */
public class ServletDefinitionTest {
    @Test
    public final void testServletInitAndConfig() throws ServletException {
        ObjectGraph objectGraph = createMock(ObjectGraph.class);

        final HttpServlet mockServlet = new HttpServlet() {
        };
        expect(objectGraph.get(HttpServlet.class))
                .andReturn(mockServlet)
                .anyTimes();

        replay(objectGraph);

        //some init params
        //noinspection SSBasedInspection
        final Map<String, String> initParams = new HashMap<String, String>() {
            {
                put("ahsd", "asdas24dok");
                put("ahssd", "asdasd124ok");
                put("ahfsasd", "asda124sdok");
                put("ahsasgd", "a124sdasdok");
                put("ahsd124124", "as124124124dasdok");
            }
        };

        String pattern = "/*";
        final ServletDefinition servletDefinition = new ServletDefinition(pattern,
                HttpServlet.class, UriPatternType.get(UriPatternType.SERVLET, pattern), initParams, null);

        ServletContext servletContext = createMock(ServletContext.class);
        final String contextName = "thing__!@@44__SRV" + getClass();
        expect(servletContext.getServletContextName())
                .andReturn(contextName);

        replay(servletContext);

        servletDefinition.init(servletContext, objectGraph,
                Sets.newSetFromMap(Maps.<HttpServlet, Boolean>newIdentityHashMap()));

        assertNotNull(mockServlet.getServletContext());
        assertEquals(contextName, mockServlet.getServletContext().getServletContextName());
        assertEquals(HttpServlet.class.getName(), mockServlet.getServletName());

        final ServletConfig servletConfig = mockServlet.getServletConfig();
        final Enumeration names = servletConfig.getInitParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();

            assertTrue(initParams.containsKey(name));
            assertEquals(initParams.get(name), servletConfig.getInitParameter(name));
        }

        verify(objectGraph, servletContext);
    }
}
