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

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import static com.google.common.base.Preconditions.checkState;

/**
 * A wrapping dispatcher for servlets, in much the same way as {@link ManagedFilterPipeline} is for
 * filters.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author John Leacox
 * @see <a href="https://code.google.com/p/google-guice/source/browse/extensions/servlet/src/com/google/inject/servlet/ManagedServletPipeline.java?name=3.0">guice-servlet-3.0 ManagedFilterPipeline</a>
 */
@Singleton
class ManagedServletPipeline {
    private final ServletDefinition[] servletDefinitions;

    @Inject
    public ManagedServletPipeline(ServletDefinition[] servletDefinitions) {
        this.servletDefinitions = servletDefinitions;
    }

    boolean hasServletsMapped() {
        return servletDefinitions.length > 0;
    }

    public void init(ServletContext servletContext, ObjectGraph objectGraph) throws ServletException {
        Set<HttpServlet> initializedSoFar
                = Sets.newSetFromMap(Maps.<HttpServlet, Boolean>newIdentityHashMap());

        for (ServletDefinition servletDefinition : servletDefinitions) {
            servletDefinition.init(servletContext, objectGraph, initializedSoFar);
        }
    }

    public boolean service(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {

        // Stop at the first matching servlet and service.
        for (ServletDefinition servletDefinition : servletDefinitions) {
            if (servletDefinition.service(request, response)) {
                return true;
            }
        }

        // There was no match...
        return false;
    }

    public void destroy() {
        Set<HttpServlet> destroyedSoFar
                = Sets.newSetFromMap(Maps.<HttpServlet, Boolean>newIdentityHashMap());
        for (ServletDefinition servletDefinition : servletDefinitions) {
            servletDefinition.destroy(destroyedSoFar);
        }
    }

    /**
     * @return Returns a request dispatcher wrapped with a servlet mapped to
     * the given path or null if no mapping was found.
     */
    RequestDispatcher getRequestDispatcher(String path) {
        final String newRequestUri = path;

        // TODO(dhanji): check servlet spec to see if the following is legal or not.
        // Need to strip query string if requested...

        for (final ServletDefinition servletDefinition : servletDefinitions) {
            if (servletDefinition.shouldServe(path)) {
                return new RequestDispatcher() {
                    public void forward(ServletRequest servletRequest, ServletResponse servletResponse)
                            throws ServletException, IOException {
                        checkState(!servletResponse.isCommitted(),
                                "Response has been committed--you can only call forward before"
                                        + " committing the response (hint: don't flush buffers)");

                        // clear buffer before forwarding
                        servletResponse.resetBuffer();

                        ServletRequest requestToProcess;
                        if (servletRequest instanceof HttpServletRequest) {
                            requestToProcess = new RequestDispatcherRequestWrapper(servletRequest, newRequestUri);
                        } else {
                            // This should never happen, but instead of throwing an exception
                            // we will allow a happy case pass thru for maximum tolerance to
                            // legacy (and internal) code.
                            requestToProcess = servletRequest;
                        }

                        servletRequest.setAttribute(REQUEST_DISPATCHER_REQUEST, Boolean.TRUE);

                        // now dispatch to the servlet
                        try {
                            servletDefinition.doService(requestToProcess, servletResponse);
                        } finally {
                            servletRequest.removeAttribute(REQUEST_DISPATCHER_REQUEST);
                        }
                    }

                    public void include(ServletRequest servletRequest, ServletResponse servletResponse)
                            throws ServletException, IOException {
                        servletRequest.setAttribute(REQUEST_DISPATCHER_REQUEST, Boolean.TRUE);

                        // route to the target servlet
                        try {
                            servletDefinition.doService(servletRequest, servletResponse);
                        } finally {
                            servletRequest.removeAttribute(REQUEST_DISPATCHER_REQUEST);
                        }
                    }
                };
            }
        }

        //otherwise, can't process
        return null;
    }

    /**
     * A Marker constant attribute that when present in the request indicates to Guice servlet that
     * this request has been generated by a request dispatcher rather than the servlet pipeline.
     * In accordance with section 8.4.2 of the Servlet 2.4 specification.
     */
    public static final String REQUEST_DISPATCHER_REQUEST = "javax.servlet.forward.servlet_path";

    private static class RequestDispatcherRequestWrapper extends HttpServletRequestWrapper {
        private final String newRequestUri;

        public RequestDispatcherRequestWrapper(ServletRequest servletRequest, String newRequestUri) {
            super((HttpServletRequest) servletRequest);
            this.newRequestUri = newRequestUri;
        }

        @Override
        public String getRequestURI() {
            return newRequestUri;
        }
    }
}
