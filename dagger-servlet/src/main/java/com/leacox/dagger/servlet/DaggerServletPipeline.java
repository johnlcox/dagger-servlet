package com.leacox.dagger.servlet;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author John Leacox
 */
@Singleton
public class DaggerServletPipeline implements ServletPipeline {
    @Inject
    DaggerServletPipeline() {
    }
}
