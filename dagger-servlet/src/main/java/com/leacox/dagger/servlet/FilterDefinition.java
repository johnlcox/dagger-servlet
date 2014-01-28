package com.leacox.dagger.servlet;

import dagger.ObjectGraph;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author John Leacox
 */
public class FilterDefinition {
    //private static final Logger LOGGER = Logger.getLogger(FilterDefinition.class);

    private final String pattern;
    private final Map<String, String> initParams;

    private boolean isInitialized = false;

    FilterDefinition(String pattern, Map<String, String> initParams) {
        this.pattern = pattern;
        this.initParams = initParams;
    }

    public void init(ServletContext servletContext, ObjectGraph objectGraph) {
        if (isInitialized) {
            return;
        }

        isInitialized = true;
    }

    public void destroy() {

    }

    public boolean shouldFilter(String uri) {
        return false;
    }

    public Filter getFilterIfMatching(HttpServletRequest request) {
        return null;
    }
}
