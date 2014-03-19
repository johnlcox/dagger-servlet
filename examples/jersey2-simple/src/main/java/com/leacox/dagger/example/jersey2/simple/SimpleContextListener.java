package com.leacox.dagger.example.jersey2.simple;

import com.leacox.dagger.jersey2.Jersey2Module;
import com.leacox.dagger.servlet.DaggerServletContextListener;

/**
 * @author John Leacox
 */
public class SimpleContextListener extends DaggerServletContextListener {
    @Override
    protected Class<?>[] getBaseModules() {
        return new Class<?>[]{SimpleModule.class, Jersey2Module.class};
    }

    @Override
    protected Class<?>[] getRequestScopedModules() {
        return new Class<?>[0];
    }
}
