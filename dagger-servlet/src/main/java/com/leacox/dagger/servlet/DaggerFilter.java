package com.leacox.dagger.servlet;

import com.leacox.dagger.servlet.FilterPipeline;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

/**
 * @author John Leacox
 */
@Singleton
public class DaggerFilter implements Filter {
    private static ThreadLocal<DaggerContext> localContext = new ThreadLocal<DaggerContext>();

    private static volatile WeakReference<ServletContext> servletContext = new WeakReference<ServletContext>(null);

    // @Inject
    private final FilterPipeline filterPipeline;

    @Inject
    DaggerFilter(FilterPipeline filterPipeline) {
        this.filterPipeline = filterPipeline;
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        DaggerContext previousContext = localContext.get();

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest originalHttpRequest = (previousContext != null) ?
                previousContext.getOriginalRequest() : httpRequest;

        try {
            new DaggerContext(originalHttpRequest, httpRequest, httpResponse).call(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    filterPipeline.dispatch(request, response, chain);
                    return null;
                }
            });
        } catch (Exception e) {
            // TODO: Use Throwables.propogate
            throw new RuntimeException(e);
        }
    }

    // TODO: When ScopingObjectGraph doesn't need to be in the dagger package anymore these methods can become package private
    public static HttpServletRequest getRequest() {
        DaggerContext context = getDaggerContext();

        if (context == null) {
            return null;
        }

        return context.getRequest();
    }

    public static HttpServletResponse getResponse() {
        DaggerContext context = getDaggerContext();

        if (context == null) {
            return null;
        }

        return context.getResponse();
    }

//    static ServletContext getServletContext() {
//        return servletContext.get();
//    }

    private static DaggerContext getDaggerContext() {
        return localContext.get();
    }

    private FilterPipeline getFilterPipeline() {
        return filterPipeline;
    }

    private static void reset() {
        localContext.remove();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();

        DaggerFilter.servletContext = new WeakReference<ServletContext>(servletContext);

        FilterPipeline filterPipeline = getFilterPipeline();
        filterPipeline.initPipeline(servletContext);
    }

    @Override
    public void destroy() {
        try {
            FilterPipeline filterPipeline = getFilterPipeline();
            filterPipeline.destroyPipeline();
        } finally {
            reset();
            servletContext.clear();
        }
    }

    private static class DaggerContext {
        private final HttpServletRequest originalRequest;
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        volatile Thread owner;

        DaggerContext(HttpServletRequest originalRequest, HttpServletRequest request, HttpServletResponse response) {
            this.originalRequest = originalRequest;
            this.request = request;
            this.response = response;
        }

        HttpServletRequest getOriginalRequest() {
            return originalRequest;
        }

        HttpServletRequest getRequest() {
            return request;
        }

        HttpServletResponse getResponse() {
            return response;
        }

        <T> T call(Callable<T> callable) throws Exception {
            Thread oldOwner = owner;
            Thread newOwner = Thread.currentThread();
            owner = newOwner;

            DaggerContext previousContext = localContext.get();
            localContext.set(this);
            try {
                return callable.call();
            } finally {
                owner = oldOwner;
                localContext.set(previousContext);
            }
        }
    }
}
