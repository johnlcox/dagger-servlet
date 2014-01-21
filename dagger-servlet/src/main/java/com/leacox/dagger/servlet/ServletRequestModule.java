package com.leacox.dagger.servlet;

import dagger.Module;

/**
 * @author John Leacox
 */
@Module(
        injects = {
        },
        addsTo = ServletRequestModule.class,
        includes = {
                InternalServletRequestModule.class
        },
        library = true
)
public class ServletRequestModule {
}
