package com.leacox.dagger.example.jersey2.simple;

//import com.leacox.dagger.jersey.JerseyModule;

import com.leacox.dagger.servlet.ServletModule;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * @author John Leacox
 */
@Module(
        injects = {
                SimpleService.class,
                SimpleResource.class
        },
        includes = {
                ServletModule.class//,
                //JerseyModule.class
        }
)
public class SimpleModule {
    @Provides
    @Singleton
    public String provideDisplay() {
        return "SimpleDisplay";
    }

    @Provides
    public SimpleService provideSimpleService(String display) {
        return new SimpleService(display);
    }
}
