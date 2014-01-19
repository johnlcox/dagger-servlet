package com.leacox.guice.example.simple;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

import javax.servlet.annotation.WebListener;

/**
 * @author John Leacox
 */
public class SimpleContextListener extends GuiceServletContextListener {
    @Override
    protected Injector getInjector() {
        return Guice.createInjector(new JerseyServletModule() {
            @Override
            protected void configureServlets() {
                bind(SimpleService.class);
                bind(SimpleResource.class);

                serve("/*").with(GuiceContainer.class);
            }

            @Provides
            String provideDisplay() {
                return "SimpleDisplay";
            }
        });
    }
}
