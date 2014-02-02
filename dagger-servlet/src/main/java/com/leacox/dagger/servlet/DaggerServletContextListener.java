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
 *      Copyright (C) 2006 Google Inc.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.*;
import dagger.ObjectGraph;
import dagger.ScopingObjectGraph;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

/**
 * Configures injection using dagger and configures filters and servlets.
 * <p/>
 * This class should be subclasses to register filters and servlets in the {@link #configureServlets()} method. Your
 * dagger modules should be added to the {@link #getBaseModules()}, {@link #getRequestScopedModules()}, and
 * {@link #getSessionScopedModules()} methods.
 * <p/>
 * Your dagger modules should be split into separate classes by scope like so:
 * <ul>
 * <li>Application Wide - Put application wide modules in {@link #getBaseModules()}</li>
 * <li>Request Scoped - Put request scoped modules in {@link #getRequestScopedModules()}</li>
 * <li>Session Scoped - Put session scoped modules in {@link #getSessionScopedModules()}</li>
 * </ul>
 * <p/>
 * The following modules are included by dagger-servlet to provide some standard bindings:
 * <ul>
 * <li>
 * {@link com.leacox.dagger.servlet.ServletModule} - This module should be included as part of the base modules,
 * either directly in the modules list, or by putting it in the {@code includes} list of your own application module.
 * <p/>
 * The following bindings are provided by this module:
 * <ul>
 * <li>{@link ServletContext}</li>
 * <li>Several internal dagger-servlet bindings</li>
 * </ul>
 * </li>
 * <li>
 * {@link com.leacox.dagger.servlet.ServletRequestModule} - This module should be included as part of the request scoped
 * modules, either directly in the modules list, or by putting it in the {@code includes} list of your own request
 * scoped module.
 * <p/>
 * The following bindings are provided by this module:
 * <ul>
 * <li>{@link javax.servlet.ServletRequest}</li>
 * <li>{@link javax.servlet.ServletResponse}</li>
 * <li>{@link javax.servlet.http.HttpSession}</li>
 * <p/>
 * </ul>
 * </li>
 * </ul>
 *
 * @author John Leacox
 */
public abstract class DaggerServletContextListener implements ServletContextListener {
    public static final String OBJECT_GRAPH_NAME = ObjectGraph.class.getName();

    private ObjectGraph objectGraph;

    private List<FilterDefinition> filterDefinitions = null;
    //private final List<FilterInstanceBindingEntry> filterInstanceEntries = Lists.newArrayList();

    private List<ServletDefinition> servletDefinitions = null;
    //private final List<ServletInstanceBindingEntry> servletInstanceEntries = Lists.newArrayList();

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        checkState(filterDefinitions == null, "Re-entry is not allowed.");
        checkState(servletDefinitions == null, "Re-entry is not allowed.");
        filterDefinitions = Lists.newArrayList();
        servletDefinitions = Lists.newArrayList();
        try {
            ServletContext servletContext = servletContextEvent.getServletContext();

            ObjectGraph unscopedGraph = ObjectGraph.create((Object[]) getBaseModules());
            ObjectGraph scopingObjectGraph = ScopingObjectGraph.create(unscopedGraph)
                    .addScopedModules(RequestScoped.class, (Object[]) getRequestScopedModules())
                    .addScopedModules(SessionScoped.class, (Object[]) getSessionScopedModules());

            scopingObjectGraph.get(ServletContextProvider.class).set(servletContext);
            scopingObjectGraph.get(InternalServletModule.ObjectGraphProvider.class).set(scopingObjectGraph);
            Iterable<Class<?>> fullModules = Iterables.concat(
                    Arrays.asList(getBaseModules()),
                    Arrays.asList(getRequestScopedModules()),
                    Arrays.asList(getSessionScopedModules()));
            scopingObjectGraph.get(InternalServletModule.FullModulesProvider.class).set(Iterables.toArray(fullModules, Class.class));

            configureServlets();

            scopingObjectGraph.get(InternalServletModule.FilterDefinitionsProvider.class)
                    .set(filterDefinitions.toArray(new FilterDefinition[filterDefinitions.size()]));

            // Ensure that servlets are not bound twice to the same pattern.
            Set<String> servletUris = Sets.newHashSet();
            for (ServletDefinition servletDefinition : servletDefinitions) {
                if (servletUris.contains(servletDefinition.getPattern())) {
                    // TODO: Consider finding all servlet configuration errors and throw one exception with all of them.
                    throw new IllegalStateException("More than one servlet was mapped to the same URI pattern: "
                            + servletDefinition.getPattern());
                } else {
                    servletUris.add(servletDefinition.getPattern());
                }
            }
            scopingObjectGraph.get(InternalServletModule.ServletDefinitionsProvider.class)
                    .set(servletDefinitions.toArray(new ServletDefinition[servletDefinitions.size()]));

            // Make sure the dagger filter is injected
            DaggerFilter daggerFilter = scopingObjectGraph.get(DaggerFilter.class);
            scopingObjectGraph.inject(daggerFilter);

            objectGraph = scopingObjectGraph;

            servletContext.setAttribute(OBJECT_GRAPH_NAME, scopingObjectGraph);
        } finally {
            filterDefinitions = null;
            servletDefinitions = null;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        servletContext.removeAttribute(OBJECT_GRAPH_NAME);
    }

    @VisibleForTesting
    ObjectGraph getObjectGraph() {
        return objectGraph;
    }

    // TODO: Would it be better to automatically include ServletModule and ServletRequestModule?

    /**
     * Override this method to return an array of your application level Dagger modules. {@link ServletModule} should
     * be included.
     */
    protected abstract Class<?>[] getBaseModules();

    /**
     * Override this method to return an array of your request scoped Dagger modules. {@link ServletRequestModule}
     * should be included.
     */
    protected abstract Class<?>[] getRequestScopedModules();

    /**
     * Override this method to return an array of your session scoped Dagger modules.
     */
    protected abstract Class<?>[] getSessionScopedModules();

    // Can't do dynamic bindings with dagger, so require the context listener implementation to setup any
    // filters/servlets that should be injected and configure the filter/servlet definitions here for the pipelines.

    // TODO: Make sure documentation correctly matches what is supported. Are instances supported for filter/servlets? And varargs?

    /**
     * <h3>Servlet Mapping EDSL</h3>
     * <p/>
     * <p> Part of the EDSL builder language for configuring servlets
     * and filters with dagger-servlet. Think of this as an in-code replacement for web.xml.
     * Filters and servlets are configured here using simple java method calls. Here is a typical
     * example of registering a filter in your context listener:
     * <p/>
     * <pre>
     *  MyContextListener extends DaggerServletContextListener {
     *      {@literal @}Override
     *      protected Class<?>[] getBaseModules() {
     *          return new Class<?>[]{ MyModule.class, ServletModule.class };
     *      }
     *
     *      {@literal @}Override
     *      protected Class<?>[] getRequestScopedModules() {
     *          return new Class<?>[]{ MyRequestModule.class };
     *      }
     *
     *      {@literal @}Override
     *      protected Class<?>[] getSessionScopedModules() {
     *          return new Class<?>[]{ MySessionModule.class };
     *      }
     *
     *      {@literal @}Override
     *      protected void configureServlets() {
     *          serve("*.html").with(MyServlet.class);
     *      }
     *  }
     * </pre>
     * <p/>
     * This registers a servlet (subclass of {@code HttpServlet}) called {@code MyServlet} to service
     * any web pages ending in {@code .html}. You can also use a path-style syntax to register
     * servlets:
     * <p/>
     * <pre>
     * <b>serve("/my/*").with(MyServlet.class)</b>
     * </pre>
     * <p/>
     * Every servlet (or filter) is required to be a singleton. If you cannot annotate the class
     * directly, you should annotate the provides method your module. Mapping a servlet that is bound
     * under any other scope is an error.
     * <p/>
     * <p/>
     * <h4>Dispatch Order</h4>
     * You are free to register as many servlets and filters as you like this way. They will
     * be compared and dispatched in the order in which the filter methods are called:
     * <p/>
     * <pre>
     *  MyContextListener extends DaggerServletContextListener {
     *      {@literal @}Override
     *      protected Class<?>[] getBaseModules() {
     *          return new Class<?>[]{ MyModule.class, ServletModule.class };
     *      }
     *
     *      {@literal @}Override
     *      protected Class<?>[] getRequestScopedModules() {
     *          return new Class<?>[]{ MyRequestModule.class };
     *      }
     *
     *      {@literal @}Override
     *      protected Class<?>[] getSessionScopedModules() {
     *          return new Class<?>[]{ MySessionModule.class };
     *      }
     *
     *      {@literal @}Override
     *      protected void configureServlets() {
     *          filter("/*").through(MyFilter.class);
     *          filter("*.css").through(MyCssFilter.class);
     *          filter("*.jpg").through(new MyJpgFilter());
     *          // etc..
     *
     *          serve("*.html").with(MyServlet.class);
     *          serve("/my/*").with(MyServlet.class);
     *          serve("*.jpg").with(new MyServlet());
     *          // etc...
     *      }
     *  }
     * </pre>
     * This will traverse down the list of rules in lexical order. For example, a url
     * "{@code /my/file.js}" (after it runs through the matching filters) will first
     * be compared against the servlet mapping:
     * <p/>
     * <pre>
     * serve("*.html").with(MyServlet.class);
     * </pre>
     * And failing that, it will descend to the next servlet mapping:
     * <p/>
     * <pre>
     * serve("/my/*").with(MyServlet.class);
     * </pre>
     * <p/>
     * Since this rule matches, Dagger Servlet will dispatch to {@code MyServlet}. These
     * two mapping rules can also be written in more compact form using varargs syntax:
     * <p/>
     * <pre>
     * serve(<b>"*.html", "/my/*"</b>).with(MyServlet.class);
     * </pre>
     * <p/>
     * This way you can map several URI patterns to the same servlet. A similar syntax is
     * also available for filter mappings.
     * <p/>
     * <p/>
     * <h4>Regular Expressions</h4>
     * You can also map servlets (or filters) to URIs using regular expressions:
     * <pre>
     * <b>serveRegex("(.)*ajax(.)*").with(MyAjaxServlet.class)</b>
     * </pre>
     * <p/>
     * This will map any URI containing the text "ajax" in it to {@code MyAjaxServlet}. Such as:
     * <ul>
     * <li>http://www.example.com/ajax.html</li>
     * <li>http://www.example.com/content/ajax/index</li>
     * <li>http://www.example.com/it/is_totally_ajaxian</li>
     * </ul>
     * <p/>
     * <p/>
     * <h3>Initialization Parameters</h3>
     * <p/>
     * Servlets (and filters) allow you to pass in init params
     * using the {@code <init-param>} tag in web.xml. You can similarly pass in parameters to
     * Servlets and filters registered in dagger-servlet using a {@link java.util.Map} of parameter
     * name/value pairs. For example, to initialize {@code MyServlet} with two parameters
     * ({@code name="John", site="example.com"}) you could write:
     * <p/>
     * <pre>
     * Map&lt;String, String&gt; params = new HashMap&lt;String, String&gt;();
     * params.put("name", "John");
     * params.put("site", "example.com");
     *
     * ...
     * serve("/*").with(MyServlet.class, <b>params</b>)
     * </pre>
     */
    // TODO: guice-servlet supports bindings from multiple modules. Support for something like this would be good
    // so that drop-in plugins can supply servlet functionality.
    protected void configureServlets() {
    }

    /**
     * @param urlPattern Any Servlet-style pattern. examples: /*, /html/*, *.html, etc.
     */
    protected final FilterDefinitionBuilder filter(String urlPattern, String... morePatterns) {
        return new FilterDefinitionBuilderImpl(Lists.asList(urlPattern, morePatterns), UriPatternType.SERVLET);
    }

    /**
     * @param regex Any Java-style regular expression.
     */
    protected final FilterDefinitionBuilder filterRegex(String regex, String... moreRegexes) {
        return new FilterDefinitionBuilderImpl(Lists.asList(regex, moreRegexes), UriPatternType.REGEX);
    }

    /**
     * @param urlPattern Any Servlet-style pattern. examples: /*, /html/*, *.html, etc.
     */
    protected final ServletDefinitionBuilder serve(String urlPattern, String... morePatterns) {
        return new ServletDefinitionBuilderImpl(Lists.asList(urlPattern, morePatterns), UriPatternType.SERVLET);
    }

    /**
     * @param regex Any Java-style regular expression.
     */
    protected final ServletDefinitionBuilder serveRegex(String regex, String... moreRegexes) {
        return new ServletDefinitionBuilderImpl(Lists.asList(regex, moreRegexes), UriPatternType.REGEX);
    }

    /**
     * See the EDSL examples at {@link #configureServlets()}
     */
    public static interface FilterDefinitionBuilder {
        void through(Class<? extends Filter> filterClass);

        // TODO: Implement instance support
        void through(Filter filter);

        void through(Class<? extends Filter> filterClass, Map<String, String> initParams);

        void through(Filter filter, Map<String, String> initParams);
    }

    private class FilterDefinitionBuilderImpl implements FilterDefinitionBuilder {
        private final List<String> uriPatterns;
        private final UriPatternType uriPatternType;

        private FilterDefinitionBuilderImpl(List<String> uriPatterns, UriPatternType uriPatternType) {
            this.uriPatterns = uriPatterns;
            this.uriPatternType = uriPatternType;
        }

        @Override
        public void through(Class<? extends Filter> filterClass) {
            through(filterClass, Maps.<String, String>newHashMap());
        }

        @Override
        public void through(Filter filter) {
            through(filter, Maps.<String, String>newHashMap());
        }

        @Override
        public void through(Class<? extends Filter> filterClass, Map<String, String> initParams) {
            through(filterClass, initParams, null);
        }

        @Override
        public void through(Filter filter, Map<String, String> initParams) {
            through(filter.getClass(), initParams, filter);
        }

        private void through(Class<? extends Filter> filterClass, Map<String, String> initParams,
                             Filter filterInstance) {
            for (String pattern : uriPatterns) {
                filterDefinitions.add(new FilterDefinition(pattern, filterClass,
                        UriPatternType.get(uriPatternType, pattern), initParams, filterInstance));
            }
        }
    }

    /**
     * See the EDSL examples at {@link #configureServlets()}
     */
    public static interface ServletDefinitionBuilder {
        void with(Class<? extends HttpServlet> servletClass);

        // TODO: Implement instance support
        void with(HttpServlet servlet);

        void with(Class<? extends HttpServlet> servletClass, Map<String, String> initParams);

        void with(HttpServlet servlet, Map<String, String> initParams);
    }

    private class ServletDefinitionBuilderImpl implements ServletDefinitionBuilder {
        private final List<String> uriPatterns;
        private final UriPatternType uriPatternType;

        private ServletDefinitionBuilderImpl(List<String> uriPatterns, UriPatternType uriPatternType) {
            this.uriPatterns = uriPatterns;
            this.uriPatternType = uriPatternType;
        }

        @Override
        public void with(Class<? extends HttpServlet> servletClass) {
            with(servletClass, Maps.<String, String>newHashMap());
        }

        @Override
        public void with(HttpServlet servlet) {
            with(servlet, Maps.<String, String>newHashMap());
        }

        @Override
        public void with(Class<? extends HttpServlet> servletClass, Map<String, String> initParams) {
            with(servletClass, initParams, null);
        }

        @Override
        public void with(HttpServlet servlet, Map<String, String> initParams) {
            with(servlet.getClass(), initParams, servlet);
        }

        private void with(Class<? extends HttpServlet> servletClass, Map<String, String> initParams,
                          HttpServlet servletInstance) {
            for (String pattern : uriPatterns) {
                servletDefinitions.add(new ServletDefinition(pattern, servletClass,
                        UriPatternType.get(uriPatternType, pattern), initParams, servletInstance));
            }
        }
    }
}
