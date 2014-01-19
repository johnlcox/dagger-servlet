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

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import dagger.ObjectGraph;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static com.leacox.dagger.servlet.ManagedServletPipeline.REQUEST_DISPATCHER_REQUEST;

/**
 * An internal representation of a servlet definition mapped to a particular URI pattern. Also
 * performs the request dispatch to that servlet. How nice and OO =)
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author John Leacox
 */
public class ServletDefinition {
    private final String pattern;
    private final Class<? extends HttpServlet> servletClass;
    private final UriPatternMatcher patternMatcher;
    private final Map<String, String> initParams;
    // set only if this was bound using a servlet instance.
    private final HttpServlet servletInstance;

    // Always set in init, our servlet is always presumed to be a singleton.
    private final AtomicReference<HttpServlet> httpServlet = new AtomicReference<HttpServlet>();

    ServletDefinition(String pattern, Class<? extends HttpServlet> servletClass, UriPatternMatcher patternMatcher,
                      Map<String, String> initParams, HttpServlet servletInstance) {
        this.pattern = pattern;
        this.servletClass = servletClass;
        this.patternMatcher = patternMatcher;
        this.initParams = Collections.unmodifiableMap(Maps.newHashMap(initParams));
        this.servletInstance = servletInstance;
    }

    public ServletDefinition get() {
        return this;
    }

    boolean shouldServe(String uri) {
        return patternMatcher.matches(uri);
    }

    public void init(final ServletContext servletContext, ObjectGraph objectGraph,
                     Set<HttpServlet> initializedSoFar) throws ServletException {
        // This absolutely must be a singleton, and so is only initialized once.
        // TODO: There isn't a good way to make sure the class is a singleton. Classes with the @Singleton annotation
        // can be identified, but classes that are singletons via an @Singleton annotated @Provides method won't
        // be identified as singletons. Bad stuff may happen for non-singletons.
//        if (!Scopes.isSingleton(servletClass)) {
//            throw new ServletException("Servlets must be bound as singletons. "
//                    + servletClass + " was not bound in singleton scope.");
//        }

        HttpServlet httpServlet;
        if (servletInstance == null) {
            httpServlet = objectGraph.get(servletClass);
        } else {
            httpServlet = servletInstance;
        }
        this.httpServlet.set(httpServlet);

        if (initializedSoFar.contains(httpServlet)) {
            return;
        }

        // Initialize our servlet with the configured context params and servlet context.
        httpServlet.init(new ServletConfig() {
            @Override
            public String getServletName() {
                return servletClass.getCanonicalName();
            }

            @Override
            public ServletContext getServletContext() {
                return servletContext;
            }

            @Override
            public String getInitParameter(String name) {
                return initParams.get(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return Iterators.asEnumeration(initParams.keySet().iterator());
            }
        });

        // Mark as initialized.
        initializedSoFar.add(httpServlet);
    }

    public void destroy(Set<HttpServlet> destroyedSoFar) {
        HttpServlet reference = httpServlet.get();

        // Do nothing if this Servlet was invalid (usually due to not being scoped
        // properly). According to Servlet Spec: it is "out of service", and does not
        // need to be destroyed.
        // Also prevent duplicate destroys to the same singleton that may appear
        // more than once on the filter chain.
        if (null == reference || destroyedSoFar.contains(reference)) {
            return;
        }

        try {
            reference.destroy();
        } finally {
            destroyedSoFar.add(reference);
        }
    }

    /**
     * Wrapper around the service chain to ensure a servlet is servicing what it must and provides it
     * with a wrapped request.
     *
     * @return Returns true if this servlet triggered for the given request. Or false if
     * dagger-servlet should continue dispatching down the servlet pipeline.
     * @throws IOException      If thrown by underlying servlet
     * @throws ServletException If thrown by underlying servlet
     */
    public boolean service(ServletRequest servletRequest,
                           ServletResponse servletResponse) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String path = request.getRequestURI().substring(request.getContextPath().length());

        boolean serve = shouldServe(path);

        // Invocations of the chain end at the first matched servlet.
        if (serve) {
            doService(servletRequest, servletResponse);
        }

        // Return false if no servlet matched (so we can proceed down to the web.xml servlets).
        return serve;
    }

    /**
     * Utility that delegates to the actual service method of the servlet wrapped with a contextual
     * request (i.e. with correctly computed path info).
     * <p/>
     * We need to suppress deprecation coz we use HttpServletRequestWrapper, which implements
     * deprecated API for backwards compatibility.
     */
    void doService(final ServletRequest servletRequest, ServletResponse servletResponse)
            throws ServletException, IOException {

        HttpServletRequest request = new HttpServletRequestWrapper(
                (HttpServletRequest) servletRequest) {
            private String path;
            private boolean pathComputed = false;
            //must use a boolean on the memo field, because null is a legal value (TODO no, it's not)

            private boolean pathInfoComputed = false;
            private String pathInfo;

            @Override
            public String getPathInfo() {
                if (!isPathInfoComputed()) {
                    int servletPathLength = getServletPath().length();
                    pathInfo = getRequestURI().substring(getContextPath().length()).replaceAll("[/]{2,}", "/");
                    pathInfo = pathInfo.length() > servletPathLength ? pathInfo.substring(servletPathLength) : null;

                    // Corner case: when servlet path and request path match exactly (without trailing '/'),
                    // then pathinfo is null
                    if ("".equals(pathInfo) && servletPathLength != 0) {
                        pathInfo = null;
                    }

                    pathInfoComputed = true;
                }

                return pathInfo;
            }

            // NOTE(dhanji): These two are a bit of a hack to help ensure that request dipatcher-sent
            // requests don't use the same path info that was memoized for the original request.
            private boolean isPathInfoComputed() {
                return pathInfoComputed
                        && !(null != servletRequest.getAttribute(REQUEST_DISPATCHER_REQUEST));
            }

            private boolean isPathComputed() {
                return pathComputed
                        && !(null != servletRequest.getAttribute(REQUEST_DISPATCHER_REQUEST));
            }

            @Override
            public String getServletPath() {
                return computePath();
            }

            @SuppressWarnings("deprecation")
            @Override
            public String getPathTranslated() {
                final String info = getPathInfo();

                return (null == info) ? null : getRealPath(info);
            }

            // Memoizer pattern.
            private String computePath() {
                if (!isPathComputed()) {
                    String servletPath = super.getServletPath();
                    path = patternMatcher.extractPath(servletPath);
                    pathComputed = true;

                    if (null == path) {
                        path = servletPath;
                    }
                }

                return path;
            }
        };

        httpServlet.get().service(request, servletResponse);
    }

    String getServletClass() {
        return servletClass.getCanonicalName();
    }

    String getPattern() {
        return pattern;
    }
}
