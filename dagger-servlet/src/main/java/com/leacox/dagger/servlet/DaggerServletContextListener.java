package com.leacox.dagger.servlet;

import dagger.ObjectGraph;
import dagger.ScopingObjectGraph;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

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
}
