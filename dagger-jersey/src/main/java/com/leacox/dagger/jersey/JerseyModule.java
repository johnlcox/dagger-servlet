package com.leacox.dagger.jersey;

import com.sun.jersey.core.util.FeaturesAndProperties;
import com.sun.jersey.spi.container.WebApplication;
import dagger.Module;
import dagger.Provides;

import javax.ws.rs.ext.Providers;

/**
 * @author John Leacox
 */
@Module
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
}
