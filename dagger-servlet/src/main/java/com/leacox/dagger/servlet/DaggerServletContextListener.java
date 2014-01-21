package com.leacox.dagger.servlet;

import dagger.ObjectGraph;

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

        ObjectGraph objectGraph = getObjectGraph();
        objectGraph.get(InternalServletModule.ServletContextProvider.class).set(servletContext);
        objectGraph.get(InternalServletModule.ObjectGraphProvider.class).set(objectGraph);
        servletContext.setAttribute(OBJECT_GRAPH_NAME, objectGraph);
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        servletContext.removeAttribute(OBJECT_GRAPH_NAME);
    }

    protected abstract ObjectGraph getObjectGraph();
}
