package com.leacox.dagger.jersey2;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * @author John Leacox
 */
public class DaggerApplication extends ResourceConfig {
    public DaggerApplication() {
        register(DaggerComponentProvider.class);
    }
}
