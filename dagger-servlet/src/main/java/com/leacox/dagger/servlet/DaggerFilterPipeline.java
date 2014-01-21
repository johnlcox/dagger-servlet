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
    //private final Provider<ServletContext> servletContextProvider;


    @Inject
    public DaggerFilterPipeline(ObjectGraph objectGraph, ServletContext servletContext) { //Provider<ServletContext> servletContextProvider) {
        this.objectGraph = objectGraph;
        this.servletContext = servletContext;
        //this.servletContextProvider = servletContextProvider;
    }

    @Override
    public void initPipeline(ServletContext context) throws ServletException {

    }

    @Override
    public void destroyPipeline() {

    }

    @Override
    public void dispatch(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        // TODO: Investigate if this is enough? Probably not.
        filterChain.doFilter(request, response);
    }
}
