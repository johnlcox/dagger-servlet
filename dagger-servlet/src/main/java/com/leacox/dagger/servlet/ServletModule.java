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
import javax.servlet.ServletContext;

/**
 * A Dagger module that provides application wide servlet related bindings. In addition to the internal bindings the
 * following bindings are provided:
 * <ul>
 * <li>{@link ServletContext}</li>
 * </ul>
 *
 * @author John Leacox
 */
@Module(
        injects = {
                ServletContext.class
        },
        includes = {
                InternalServletModule.class
        }
)
public class ServletModule {
    @Provides
    @Singleton
    ServletContext provideServletContext(ServletContextProvider servletContextProvider) {
        return servletContextProvider.get();
    }
}
