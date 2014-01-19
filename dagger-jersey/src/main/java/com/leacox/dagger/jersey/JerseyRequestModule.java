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
 * @author John Leacox
 */
@Module(
        injects = {},
        addsTo = JerseyModule.class,
        library = true
)
public abstract class JerseyRequestModule {
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
