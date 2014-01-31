package com.leacox.dagger.servlet;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * @author John Leacox
 */
@Singleton
public class DaggerFilter implements Filter {
    private static ThreadLocal<DaggerContext> localContext = new ThreadLocal<DaggerContext>();
    static volatile FilterPipeline pipeline = new DefaultFilterPipeline();

    private static volatile WeakReference<ServletContext> servletContext = new WeakReference<ServletContext>(null);

    private static final String MULTIPLE_INJECTORS_WARNING =
            "Multiple Servlet object graphs detected. This is a warning "
                    + "indicating that you have more than one "
                    + DaggerFilter.class.getSimpleName() + " running "
                    + "in your web application. If this is deliberate, you may safely "
                    + "ignore this message. If this is NOT deliberate however, "
                    + "your application may not work as expected.";

    // We allow both the static and dynamic versions of the pipeline to exist.

    // Default constructor needed for container managed construction
    public DaggerFilter() {
    }

    @Inject
    FilterPipeline injectedPipeline;

    @Inject
    DaggerFilter(FilterPipeline pipeline) {
        // This can happen if you create many injectors and they all have their own
        // servlet module. This is legal, caveat a small warning.
        if (DaggerFilter.pipeline instanceof ManagedFilterPipeline) {
            Logger.getLogger(DaggerFilter.class.getName()).warning(MULTIPLE_INJECTORS_WARNING);
        }

        // We overwrite the default pipeline
        DaggerFilter.pipeline = pipeline;
    }

    private FilterPipeline getPipeline() {
        return injectedPipeline != null ? injectedPipeline : pipeline;
    }

//    @Inject
//    DaggerFilter(FilterPipeline injectedPipeline) {
//        this.injectedPipeline = injectedPipeline;
//    }    @Inject
//    DaggerFilter(FilterPipeline injectedPipeline) {
//        this.injectedPipeline = injectedPipeline;
//    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        DaggerContext previousContext = localContext.get();

        // Prefer the injected pipeline, but fall back on the static one for web.xml users.
        final FilterPipeline filterPipeline = getPipeline();

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

//    private FilterPipeline getInjectedPipeline() {
//        return injectedPipeline;
//    }

    private static void reset() {
        localContext.remove();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();

        DaggerFilter.servletContext = new WeakReference<ServletContext>(servletContext);

        FilterPipeline filterPipeline = getPipeline();
        filterPipeline.initPipeline(servletContext);
    }

    @Override
    public void destroy() {
        try {
            FilterPipeline filterPipeline = getPipeline();
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
