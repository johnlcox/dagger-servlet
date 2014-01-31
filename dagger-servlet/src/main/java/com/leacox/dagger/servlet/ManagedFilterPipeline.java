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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Central routing/dispatch class handles lifecycle of managed filters, and delegates to the servlet
 * pipeline.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author John Leacox
 * @see <a href="https://code.google.com/p/google-guice/source/browse/extensions/servlet/src/com/google/inject/servlet/ManagedFilterPipeline.java?name=3.0">guice-servlet-3.0 ManagedFilterPipeline</a>
 */
@Singleton
class ManagedFilterPipeline implements FilterPipeline {
    private final FilterDefinition[] filterDefinitions;
    private final ManagedServletPipeline servletPipeline;
    private final ServletContext servletContext;

    // Unfortunately, we need the object graph itself in order to create filters + servlets
    private final ObjectGraph objectGraph;

    // Guards a DCL, so needs to be volatile
    private volatile boolean initialized = false;

    @Inject
    public ManagedFilterPipeline(ObjectGraph objectGraph, ManagedServletPipeline servletPipeline,
                                 ServletContext servletContext, FilterDefinition[] filterDefinitions) {
        this.objectGraph = objectGraph;
        this.servletPipeline = servletPipeline;
        this.servletContext = servletContext;

        this.filterDefinitions = filterDefinitions;
    }

    public synchronized void initPipeline(ServletContext servletContext)
            throws ServletException {

        //double-checked lock, prevents duplicate initialization
        if (initialized)
            return;

        // Used to prevent duplicate initialization.
        Set<Filter> initializedSoFar = Sets.newSetFromMap(Maps.<Filter, Boolean>newIdentityHashMap());

        for (FilterDefinition filterDefinition : filterDefinitions) {
            filterDefinition.init(servletContext, objectGraph, initializedSoFar);
        }

        //next, initialize servlets...
        servletPipeline.init(servletContext, objectGraph);

        //everything was ok...
        initialized = true;
    }

    public void dispatch(ServletRequest request, ServletResponse response,
                         FilterChain proceedingFilterChain) throws IOException, ServletException {
        if (!initialized) {
            initPipeline(servletContext);
        }

        //obtain the servlet pipeline to dispatch against
        new FilterChainInvocation(filterDefinitions, servletPipeline, proceedingFilterChain)
                .doFilter(withDispatcher(request, servletPipeline), response);

    }

    /**
     * Used to create an proxy that dispatches either to the guice-servlet pipeline or the regular
     * pipeline based on uri-path match. This proxy also provides minimal forwarding support.
     * <p/>
     * We cannot forward from a web.xml Servlet/JSP to a guice-servlet (because the filter pipeline
     * is not called again). However, we can wrap requests with our own dispatcher to forward the
     * *other* way. web.xml Servlets/JSPs can forward to themselves as per normal.
     * <p/>
     * This is not a problem cuz we intend for people to migrate from web.xml to guice-servlet,
     * incrementally, but not the other way around (which, we should actively discourage).
     */
    @SuppressWarnings({"JavaDoc", "deprecation"})
    private ServletRequest withDispatcher(ServletRequest servletRequest,
                                          final ManagedServletPipeline servletPipeline) {

        HttpServletRequest request = (HttpServletRequest) servletRequest;

        // don't wrap the request if there are no servlets mapped. This prevents us from inserting our
        // wrapper unless it's actually going to be used. This is necessary for compatibility for apps
        // that downcast their HttpServletRequests to a concrete implementation.
        if (!servletPipeline.hasServletsMapped()) {
            return servletRequest;
        }

        //noinspection OverlyComplexAnonymousInnerClass
        return new HttpServletRequestWrapper(request) {

            @Override
            public RequestDispatcher getRequestDispatcher(String path) {
                final RequestDispatcher dispatcher = servletPipeline.getRequestDispatcher(path);

                return (null != dispatcher) ? dispatcher : super.getRequestDispatcher(path);
            }
        };
    }

    public void destroyPipeline() {
        //destroy servlets first
        servletPipeline.destroy();

        //go down chain and destroy all our filters
        Set<Filter> destroyedSoFar = Sets.newSetFromMap(Maps.<Filter, Boolean>newIdentityHashMap());
        for (FilterDefinition filterDefinition : filterDefinitions) {
            filterDefinition.destroy(destroyedSoFar);
        }
    }
}

