package com.leacox.dagger.servlet;

import com.sun.jersey.api.core.*;
import com.sun.jersey.core.util.FeaturesAndProperties;
import com.sun.jersey.spi.MessageBodyWorkers;
import com.sun.jersey.spi.container.ExceptionMapperContext;
import com.sun.jersey.spi.container.WebApplication;
import dagger.Module;
import dagger.Provides;

import javax.ws.rs.ext.Providers;

/**
 * @author John Leacox
 */
@Module(
        injects = {},
        addsTo = JerseyModule.class,
        library = true
)
public abstract class JerseyModule {
    @Provides
    public WebApplication webApplication(DaggerContainer daggerContainer) {
        return daggerContainer.getWebApplication();
    }

    @Provides
    public Providers provideProviders(WebApplication webApplication) {
        return webApplication.getProviders();
    }

    @Provides
    public FeaturesAndProperties provideFeaturesAndProperties(WebApplication webApplication) {
        return webApplication.getFeaturesAndProperties();
    }

    @Provides
    public MessageBodyWorkers provideMessageBodyWorkers(WebApplication webApplication) {
        return webApplication.getMessageBodyWorkers();
    }

    @Provides
    public ExceptionMapperContext provideExceptionMapperContext(WebApplication webApplication) {
        return webApplication.getExceptionMapperContext();
    }

    @Provides
    public ResourceContext provideResourceContext(WebApplication webApplication) {
        return webApplication.getResourceContext();
    }
}
