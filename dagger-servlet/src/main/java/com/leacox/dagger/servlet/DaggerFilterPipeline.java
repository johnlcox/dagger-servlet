package com.leacox.dagger.servlet;

import dagger.ObjectGraph;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.*;
import java.io.IOException;

/**
 * @author John Leacox
 */
@Singleton
public class DaggerFilterPipeline implements FilterPipeline {
    private final ObjectGraph objectGraph;
    private final ServletContext servletContext;
    private final ServletPipeline servletPipeline;


    @Inject
    public DaggerFilterPipeline(ObjectGraph objectGraph, ServletPipeline servletPipeline, ServletContext servletContext) {
        this.objectGraph = objectGraph;
        this.servletPipeline = servletPipeline;
        this.servletContext = servletContext;
    }

    @Override
    public void initPipeline(ServletContext context) throws ServletException {

    }

    @Override
    public void dispatch(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        // TODO: Investigate if this is enough? Probably not.
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroyPipeline() {

    }
}
