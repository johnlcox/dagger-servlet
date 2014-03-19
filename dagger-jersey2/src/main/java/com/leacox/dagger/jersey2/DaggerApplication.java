package com.leacox.dagger.jersey2;

import com.leacox.dagger.servlet.DaggerServletContextListener;
import dagger.ObjectGraph;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Inject;
import javax.servlet.ServletContext;

/**
 * @author John Leacox
 */
public class DaggerApplication extends ResourceConfig {
    @Inject
    public DaggerApplication(ServiceLocator locator) {
        ServletContext servletContext = locator.getService(ServletContext.class);

        ObjectGraph objectGraph = (ObjectGraph) servletContext.getAttribute(DaggerServletContextListener.OBJECT_GRAPH_NAME);
        objectGraph.get(Jersey2Module.ResourceConfigProvider.class).set(this);

        objectGraph.get(DaggerInjectionRegisterer.class).registerInjections();

        register(DaggerComponentProvider.class);
    }
}
