package com.leacox.dagger.jersey;

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
        servletContext.setAttribute(OBJECT_GRAPH_NAME, objectGraph);
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        servletContext.removeAttribute(OBJECT_GRAPH_NAME);
    }

    protected abstract ObjectGraph getObjectGraph();
}
