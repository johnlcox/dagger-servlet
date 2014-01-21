package com.leacox.dagger.servlet;

import dagger.Module;

/**
 * @author John Leacox
 */
@Module(
        injects = {
        },
        includes = {
                InternalServletModule.class
        }
        //library = true
)
public class ServletModule {
    // TODO: Filter and Servlet bindings?
}
