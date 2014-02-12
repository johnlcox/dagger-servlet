/**
 * Copyright (C) 2014 John Leacox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leacox.dagger.servlet;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * @author John Leacox
 */
@Module(
        injects = {
                ServletRequest.class,
                ServletResponse.class,
                HttpServletRequest.class,
                HttpServletResponse.class,
                HttpSession.class,
        },
        library = true
)
class InternalServletRequestModule {
    @Provides
    @Singleton
    ServletRequest provideServletRequest() {
        return DaggerFilter.getRequest();
    }

    @Provides
    @Singleton
    ServletResponse provideServletResponse() {
        return DaggerFilter.getResponse();
    }

    @Provides
    @Singleton
    HttpServletRequest provideHttpServletRequest() {
        return DaggerFilter.getRequest();
    }

    @Provides
    @Singleton
    HttpServletResponse provideHttpServletResponse() {
        return DaggerFilter.getResponse();
    }

    @Provides
    HttpSession provideHttpSession() {
        return DaggerFilter.getRequest().getSession();
    }

    @Provides
    @Singleton
    @RequestParameters
    Map<String, String[]> provideRequestParameters() {
        return DaggerFilter.getRequest().getParameterMap();
    }
}
