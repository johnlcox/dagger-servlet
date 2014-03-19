package com.leacox.dagger.jersey2;

import com.leacox.dagger.servlet.DaggerServletContextListener;
import dagger.ObjectGraph;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerProvider;

import javax.servlet.ServletContext;
import javax.ws.rs.ProcessingException;

/**
 * @author John Leacox
 */
public class DaggerContainerProvider implements ContainerProvider {
    public DaggerContainerProvider() {
        boolean blah = false;
    }

    @Override
    public <T> T createContainer(Class<T> type, ApplicationHandler appHandler) throws ProcessingException {
        if (type == Container.class || type == DaggerContainer.class) {
            final ServiceLocator locator = appHandler.getServiceLocator();
            ServletContext servletContext = locator.getService(ServletContext.class);

            ObjectGraph objectGraph = (ObjectGraph) servletContext.getAttribute(DaggerServletContextListener.OBJECT_GRAPH_NAME);
            objectGraph.get(Jersey2Module.ResourceConfigProvider.class).set(appHandler.getConfiguration());
            return type.cast(objectGraph.get(DaggerContainer.class));
        }

        return null;
    }
}
