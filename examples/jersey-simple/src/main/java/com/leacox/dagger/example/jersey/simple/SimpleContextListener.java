package com.leacox.dagger.example.jersey.simple;

import com.leacox.dagger.jersey.DaggerContainer;
import com.leacox.dagger.servlet.DaggerServletContextListener;

/**
 * @author John Leacox
 */
public class SimpleContextListener extends DaggerServletContextListener {
    @Override
    protected Class<?>[] getBaseModules() {
        return new Class<?>[]{SimpleModule.class};
    }

    @Override
    protected Class<?>[] getRequestScopedModules() {
        return new Class<?>[0];
    }

    @Override
    protected void configureServlets() {
        serve("/*").with(DaggerContainer.class);
    }
}
