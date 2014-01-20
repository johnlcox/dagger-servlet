package com.leacox.dagger.jersey;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * @author John Leacox
 */
public class DaggerFilter implements Filter {
    private static ThreadLocal<DaggerContext> localContext = new ThreadLocal<DaggerContext>();

    private FilterPipeline filterPipeline;

    DaggerFilter(FilterPipeline filterPipeline) {
        this.filterPipeline = filterPipeline;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

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

    @Override
    public void destroy() {

    }

    // TODO: What is the purpose of originalRequest and owner?
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
