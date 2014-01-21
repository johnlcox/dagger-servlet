package com.leacox.dagger.servlet;

import dagger.Module;

/**
 * @author John Leacox
 */
@Module(
        injects = {
        },
        addsTo = ServletModule.class,
        includes = {
                InternalServletRequestModule.class
        }
)
public class ServletRequestModule {
}
