package com.leacox.dagger.servlet;

import javax.servlet.*;
import java.io.IOException;

/**
 * @author John Leacox
 */
public interface FilterPipeline {
    void initPipeline(ServletContext context) throws ServletException;

    void destroyPipeline();

    void dispatch(ServletRequest request, ServletResponse response, FilterChain defaultFilterChain)
            throws IOException, ServletException;
}
