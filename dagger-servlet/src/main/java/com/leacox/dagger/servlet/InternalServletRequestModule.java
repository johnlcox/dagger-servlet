package com.leacox.dagger.servlet;

import dagger.Module;
import dagger.Provides;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author John Leacox
 */
@Module(
        injects = {
                ServletRequest.class,
                ServletResponse.class,
                HttpSession.class
        }
)
class InternalServletRequestModule {
    @Provides
    @RequestScoped
    ServletRequest provideServletRequest() {
        return DaggerFilter.getRequest();
    }

    @Provides
    @RequestScoped
    ServletResponse provideServletResponse() {
        return DaggerFilter.getResponse();
    }

    @Provides
    HttpSession provideHttpSession() {
        return DaggerFilter.getRequest().getSession();
    }

    // TODO: RequestParameters Map
}
