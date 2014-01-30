package com.leacox.dagger.example.simple;

import com.leacox.dagger.servlet.DaggerServletContextListener;

import javax.servlet.annotation.WebListener;

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
    protected Class<?>[] getSessionScopedModules() {
        return new Class<?>[0];
    }

    @Override
    protected void configureServlets() {
        
    }
}
