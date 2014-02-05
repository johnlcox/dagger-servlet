package com.leacox.dagger.jersey;

import com.sun.jersey.api.core.ExtendedUriInfo;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.api.core.HttpResponseContext;
import com.sun.jersey.spi.container.WebApplication;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

/**
 * A Dagger module that provides request scoped JAX-RS and Jersey related bindings. In addition to the internal
 * bindings the following bindings are provided:
 * <ul>
 * <li>{@link com.sun.jersey.api.core.HttpContext}</li>
 * <li>{@link javax.ws.rs.core.UriInfo}</li>
 * <li>{@link com.sun.jersey.api.core.ExtendedUriInfo}</li>
 * <li>{@link com.sun.jersey.api.core.HttpRequestContext}</li>
 * <li>{@link javax.ws.rs.core.HttpHeaders}</li>
 * <li>{@link javax.ws.rs.core.Request}</li>
 * <li>{@link javax.ws.rs.core.SecurityContext}</li>
 * <li>{@link com.sun.jersey.api.core.HttpResponseContext}</li>
 * </ul>
 *
 * @author John Leacox
 */
@Module(
        injects = {},
        addsTo = JerseyModule.class,
        library = true
)
public class JerseyRequestModule {
    //@RequestScoped
    @Singleton
    @Provides
    public HttpContext provideHttpContext(WebApplication webApplication) {
        return webApplication.getThreadLocalHttpContext();
    }

    //@RequestScoped
    @Singleton
    @Provides
    public UriInfo provideUriInfo(WebApplication webApplication) {
        return webApplication.getThreadLocalHttpContext().getUriInfo();
    }

    //@RequestScoped
    @Singleton
    @Provides
    public ExtendedUriInfo provideExtendedUriInfo(WebApplication webApplication) {
        return webApplication.getThreadLocalHttpContext().getUriInfo();
    }

    //@RequestScoped
    @Singleton
    @Provides
    public HttpRequestContext provideHttpRequestContext(WebApplication webApplication) {
        return webApplication.getThreadLocalHttpContext().getRequest();
    }

    //@RequestScoped
    @Singleton
    @Provides
    public HttpHeaders provideHttpHeaders(WebApplication webApplication) {
        return webApplication.getThreadLocalHttpContext().getRequest();
    }

    //@RequestScoped
    @Singleton
    @Provides
    public Request provideRequest(WebApplication webApplication) {
        return webApplication.getThreadLocalHttpContext().getRequest();
    }

    //@RequestScoped
    @Singleton
    @Provides
    public SecurityContext provideSecurityContext(WebApplication webApplication) {
        return webApplication.getThreadLocalHttpContext().getRequest();
    }

    //@RequestScoped
    @Singleton
    @Provides
    public HttpResponseContext provideResponseContext(WebApplication webApplication) {
        return webApplication.getThreadLocalHttpContext().getResponse();
    }
}
