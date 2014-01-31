package com.leacox.dagger.servlet;

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
 * @author John Leacox
 */
public abstract class DaggerServletContextListener implements ServletContextListener {
    public static final String OBJECT_GRAPH_NAME = ObjectGraph.class.getName();

    private List<FilterDefinition> filterDefinitions = null;
    //private final List<FilterInstanceBindingEntry> filterInstanceEntries = Lists.newArrayList();

    private List<ServletDefinition> servletDefinitions = null;
    //private final List<ServletInstanceBindingEntry> servletInstanceEntries = Lists.newArrayList();

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        checkState(filterDefinitions == null, "Re-entry is not allowed.");
        checkState(servletDefinitions == null, "Re-entry is not allowed.");
        filterDefinitions = Lists.newArrayList();
        servletDefinitions = Lists.newArrayList();
        try {
            ServletContext servletContext = servletContextEvent.getServletContext();

            ObjectGraph objectGraph = ObjectGraph.create((Object[]) getBaseModules());

            ObjectGraph scopingObjectGraph = ScopingObjectGraph.create(objectGraph)
                    .addScopedModules(RequestScoped.class, (Object[]) getRequestScopedModules())
                    .addScopedModules(SessionScoped.class, (Object[]) getSessionScopedModules());

            scopingObjectGraph.get(InternalServletModule.ServletContextProvider.class).set(servletContext);
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

            servletContext.setAttribute(OBJECT_GRAPH_NAME, scopingObjectGraph);
        } finally {
            filterDefinitions = null;
            servletDefinitions = null;
        }
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        servletContext.removeAttribute(OBJECT_GRAPH_NAME);
    }

    protected abstract Class<?>[] getBaseModules();

    protected abstract Class<?>[] getRequestScopedModules();

    protected abstract Class<?>[] getSessionScopedModules();

    // TODO: Filter and Servlet bindings? This config probably needs to go in DaggerServletContextListener
    // Can't do dynamic bindings with dagger, so require the context listener implementation to setup any
    // filters/servlets that should be injected and configure the filter/servlet definitions here for the pipelines.
    protected void configureServlets() {
    }

    protected final FilterDefinitionBuilder filter(String urlPattern, String... morePatterns) {
        return new FilterDefinitionBuilderImpl(Lists.asList(urlPattern, morePatterns), UriPatternType.SERVLET);
    }

    protected final FilterDefinitionBuilder filterRegex(String regex, String... moreRegexes) {
        return new FilterDefinitionBuilderImpl(Lists.asList(regex, moreRegexes), UriPatternType.REGEX);
    }

    protected final ServletDefinitionBuilder serve(String urlPattern, String... morePatterns) {
        return new ServletDefinitionBuilderImpl(Lists.asList(urlPattern, morePatterns), UriPatternType.SERVLET);
    }

    protected final ServletDefinitionBuilder serveRegex(String regex, String... moreRegexes) {
        return new ServletDefinitionBuilderImpl(Lists.asList(regex, moreRegexes), UriPatternType.REGEX);
    }

    public static interface FilterDefinitionBuilder {
        void through(Class<? extends Filter> filterClass);

        // void through(Filter filter);

        void through(Class<? extends Filter> filterClass, Map<String, String> initParams);

        // void through(Filter filter, Map<String, String> initParams);
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

//      @Override
//      public void through(Filter filter) {
//          through(filter, Maps.<String, String>newHashMap());
//      }

        @Override
        public void through(Class<? extends Filter> filterClass, Map<String, String> initParams) {
            through(filterClass, initParams, null);
        }

//      @Override
//      public void through(Filter filter, Map<String, String> initParams) {
//          through(filter.getClass(), initParams, filter);
//      }

        private void through(Class<? extends Filter> filterClass, Map<String, String> initParams,
                             Filter filterInstance) {
            for (String pattern : uriPatterns) {
                filterDefinitions.add(new FilterDefinition(pattern, filterClass,
                        UriPatternType.get(uriPatternType, pattern), initParams, filterInstance));
            }
        }
    }

    public static interface ServletDefinitionBuilder {
        void with(Class<? extends HttpServlet> servletClass);

        // void with(HttpServlet servlet);

        void with(Class<? extends HttpServlet> servletClass, Map<String, String> initParams);

        // void with(HttpServlet servlet, Map<String, String> initParams);
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

//      @Override
//      public void with(HttpServlet servlet) {
//          with(servlet, Maps.<String, String>newHashMap());
//      }

        @Override
        public void with(Class<? extends HttpServlet> servletClass, Map<String, String> initParams) {
            with(servletClass, initParams, null);
        }

//      @Override
//      public void with(HttpServlet servlet, Map<String, String> initParams) {
//          // TODO: Handle instances
//      }

        private void with(Class<? extends HttpServlet> servletClass, Map<String, String> initParams,
                          HttpServlet servletInstance) {
            for (String pattern : uriPatterns) {
                servletDefinitions.add(new ServletDefinition(pattern, servletClass,
                        UriPatternType.get(uriPatternType, pattern), initParams, servletInstance));
            }
        }
    }
}
