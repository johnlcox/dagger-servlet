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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import dagger.ObjectGraph;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * An internal representation of a filter definition against a particular URI pattern.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author John Leacox
 */
class FilterDefinition {
    private final String pattern;
    private final Class<? extends Filter> filterClass;
    private final UriPatternMatcher patternMatcher;
    private final Map<String, String> initParams;
    // set only if this was bound to an instance of a Filter.
    private final Filter filterInstance;

    // always set after init is called.
    private final AtomicReference<Filter> filter = new AtomicReference<Filter>();

    public FilterDefinition(String pattern, Class<? extends Filter> filterClass,
                            UriPatternMatcher patternMatcher, Map<String, String> initParams, Filter filterInstance) {
        this.pattern = pattern;
        this.filterClass = filterClass;
        this.patternMatcher = patternMatcher;
        this.initParams = Collections.unmodifiableMap(new HashMap<String, String>(initParams));
        this.filterInstance = filterInstance;
    }

    public FilterDefinition get() {
        return this;
    }

    private boolean shouldFilter(String uri) {
        return patternMatcher.matches(uri);
    }

    public void init(final ServletContext servletContext, ObjectGraph objectGraph,
                     Set<Filter> initializedSoFar) throws ServletException {
        // This absolutely must be a singleton, and so is only initialized once.
        // TODO: There isn't a good way to make sure the class is a singleton. Classes with the @Singleton annotation
        // can be identified, but classes that are singletons via an @Singleton annotated @Provides method won't
        // be identified as singletons. Bad stuff may happen for non-singletons.
//        if (!Scopes.isSingleton(filterClass)) {
//            throw new ServletException("Filters must be bound as singletons. "
//                    + filterClass + " was not bound in singleton scope.");
//        }

        Filter filter;
        if (filterInstance == null) {
            filter = objectGraph.get(filterClass);
        } else {
            filter = filterInstance;
        }
        this.filter.set(filter);

        // Only fire init() if this Singleton filter has not already appeared earlier
        // in the filter chain.
        if (initializedSoFar.contains(filter)) {
            return;
        }

        // Initialize our filter with the configured context params and servlet context.
        filter.init(new FilterConfig() {
            @Override
            public String getFilterName() {
                return filterClass.getCanonicalName();
            }

            @Override
            public ServletContext getServletContext() {
                return servletContext;
            }

            @Override
            public String getInitParameter(String s) {
                return initParams.get(s);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return Iterators.asEnumeration(initParams.keySet().iterator());
            }
        });

        initializedSoFar.add(filter);
    }

    public void destroy(Set<Filter> destroyedSoFar) {
        // filters are always singletons
        Filter reference = filter.get();

        // Do nothing if this Filter was invalid (usually due to not being scoped
        // properly), or was already destroyed. According to Servlet Spec: it is
        // "out of service", and does not need to be destroyed.
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

    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse, FilterChainInvocation filterChainInvocation)
            throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final String path = request.getRequestURI().substring(request.getContextPath().length());

        if (shouldFilter(path)) {
            filter.get()
                    .doFilter(servletRequest, servletResponse, filterChainInvocation);

        } else {
            //otherwise proceed down chain anyway
            filterChainInvocation.doFilter(servletRequest, servletResponse);
        }
    }

    @VisibleForTesting
    Filter getFilter() {
        return filter.get();
    }
}
