package com.leacox.dagger.servlet;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.container.servlet.WebConfig;
import dagger.ObjectGraph;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import java.util.Map;

/**
 * @author John Leacox
 */
@Singleton
public class DaggerContainer extends ServletContainer {
    private final ObjectGraph objectGraph;
    private final Class<?>[] modules;
    //private final JerseyModule module;
    //private final JerseyRequestModule requestModule;

    private WebApplication webApplication;

    @Inject
    public DaggerContainer(ObjectGraph objectGraph, Class<?>[] modules) {
        this.objectGraph = objectGraph;
        this.modules = modules;
    }

    @Override
    protected ResourceConfig getDefaultResourceConfig(Map<String, Object> properties, WebConfig webConfig)
            throws ServletException {
        return new DefaultResourceConfig();
    }

    @Override
    protected void initiate(ResourceConfig config, WebApplication webApplication) {
        this.webApplication = webApplication;
        webApplication.initiate(config, new DaggerComponentProviderFactory(config, objectGraph, modules));
    }

    public WebApplication getWebApplication() {
        return webApplication;
    }
}
