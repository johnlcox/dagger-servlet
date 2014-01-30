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

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An internal representation of a servlet definition mapped to a particular URI pattern. Also
 * performs the request dispatch to that servlet. How nice and OO =)
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author John Leacox
 * @see <a href="https://code.google.com/p/google-guice/source/browse/extensions/servlet/src/com/google/inject/servlet/ServletDefinition.java?name=3.0">guice-servlet-3.0 ServletDefinition</a>
 */
public class ServletDefinition {
    private final String pattern;
    private final Class<? extends HttpServlet> servletClass;
    private final UriPatternMatcher patternMatcher;
    private final Map<String, String> initParams;

    private final AtomicReference<HttpServlet> httpServlet = new AtomicReference<HttpServlet>();

    private boolean isInitialized = false;

    ServletDefinition(String pattern, Class<? extends HttpServlet> servletClass, UriPatternMatcher patternMatcher,
                      Map<String, String> initParams) {
        this.pattern = pattern;
        this.servletClass = servletClass;
        this.patternMatcher = patternMatcher;
        this.initParams = Collections.unmodifiableMap(Maps.newHashMap(initParams));
    }

    public void init(final ServletContext servletContext, ObjectGraph objectGraph) throws ServletException {
        if (isInitialized) {
            return;
        }

        if (!Scopes.isSingleton(servletClass)) {
            throw new ServletException("Servlets must be bound with singleton scope. " + servletClass +
                    " was not bound with singleton scope.");
        }

        HttpServlet httpServlet = objectGraph.get(servletClass);
        this.httpServlet.set(httpServlet);

        // TODO: Is the initializedSoFar Set needed?

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

        isInitialized = true;
    }

    public void destroy() {
        HttpServlet httpServlet = this.httpServlet.get();

        // TODO: Is the destroyedSoFar Set needed?

        if (httpServlet == null) {
            return;
        }

        httpServlet.destroy();
    }

    boolean shouldServe(String uri) {
        return patternMatcher.matches(uri);
    }

    public boolean service(ServletRequest servletRequest,
                           ServletResponse servletResponse) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String path = request.getRequestURI().substring(request.getContextPath().length());

        boolean serve = shouldServe(path);

        if (serve) {
            doService(servletRequest, servletResponse);
        }

        return serve;
    }

    void doService(final ServletRequest servletRequest, ServletResponse servletResponse) {
        HttpServletRequest request = new HttpServletRequestWrapper((HttpServletRequest) servletRequest) {
            private String path;
            private boolean pathComputed = false;
            private String pathInfo;
            private boolean pathInfoComputed = false;

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

            @Override
            public String getPathTranslated() {
                final String info = getPathInfo();

                return (null == info) ? null : getRealPath(info);
            }

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
        }
    }
}
