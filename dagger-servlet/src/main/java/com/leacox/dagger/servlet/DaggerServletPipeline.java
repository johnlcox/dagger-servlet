package com.leacox.dagger.servlet;

import dagger.ObjectGraph;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContext;

/**
 * @author John Leacox
 */
@Singleton
public class DaggerServletPipeline implements ServletPipeline {
    private final ObjectGraph objectGraph;

    @Inject
    DaggerServletPipeline(ObjectGraph objectGraph) {
        this.objectGraph = objectGraph;
    }
}
