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
)
public class ServletModule {
}
