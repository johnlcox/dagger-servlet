package com.leacox.dagger.servlet;

import dagger.ObjectGraph;
import dagger.ScopingObjectGraph;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;
import java.util.Map;

/**
 * @author John Leacox
 */
public abstract class DaggerServletContextListener implements ServletContextListener {
    private static final String OBJECT_GRAPH_NAME = ObjectGraph.class.getName();

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();

        ObjectGraph scopingObjectGraph = ScopingObjectGraph.create(getObjectGraph())
                .addScopedModules(RequestScoped.class, (Object[]) getRequestScopedModules())
                .addScopedModules(SessionScoped.class, (Object[]) getSessionScopedModules());

        scopingObjectGraph.get(InternalServletModule.ServletContextProvider.class).set(servletContext);
        scopingObjectGraph.get(InternalServletModule.ObjectGraphProvider.class).set(scopingObjectGraph);
        servletContext.setAttribute(OBJECT_GRAPH_NAME, scopingObjectGraph);
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        servletContext.removeAttribute(OBJECT_GRAPH_NAME);
    }

    protected abstract ObjectGraph getObjectGraph();

    protected abstract Class<?>[] getRequestScopedModules();

    protected abstract Class<?>[] getSessionScopedModules();

    // TODO: Filter and Servlet bindings? This config probably needs to go in DaggerServletContextListener
    // Can't do dynamic bindings with dagger, so require the context listener implementation to setup any
    // filters/servlets that should be injected and configure the filter/servlet definitions here for the pipelines.
    protected void configureServlets() {
    }

    protected final FilterDefinitionBuilder filter(String urlPattern, String... morePatterns) {
        return null;
    }

    protected final FilterDefinitionBuilder filterRegex(String regex, String moreRegexes) {
        return null;
    }

    protected final ServletDefinitionBuilder serve(String urlPattern, String... morePatterns) {
        return null;
    }

    protected final ServletDefinitionBuilder serveRegex(String regex, String... moreRegexes) {
        return null;
    }

    public static interface FilterDefinitionBuilder {
        void through(Class<? extends Filter> filterClass);

        void through(Filter filter);

        void through(Class<? extends Filter> filterClass, Map<String, String> initParams);

        void through(Filter filter, Map<String, String> initParams);
    }

    private static class FilterDefinitionBuilderImpl implements FilterDefinitionBuilder {

        @Override
        public void through(Class<? extends Filter> filterClass) {

        }

        @Override
        public void through(Filter filter) {

        }

        @Override
        public void through(Class<? extends Filter> filterClass, Map<String, String> initParams) {

        }

        @Override
        public void through(Filter filter, Map<String, String> initParams) {

        }
    }

    public static interface ServletDefinitionBuilder {
        void with(Class<? extends HttpServlet> servletClass);

        void with(HttpServlet servlet);

        void with(Class<? extends HttpServlet> servletClass, Map<String, String> initParams);

        void with(HttpServlet servlet, Map<String, String> initParams);
    }
}
