/**
 * Copyright (C) 2008 Google Inc.
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

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * An internal dispatcher for dagger-servlet registered servlets and filters.
 * By default we dispatch directly to the web.xml pipeline.
 * <p/>
 * <p/>
 * If on the other hand, {@link com.leacox.dagger.servlet.DaggerServletContextListener}
 * is used to register managed servlets and/or filters, then a different pipeline is
 * bound instead. Which, after dispatching to Dagger-injected filters and servlets
 * continues to the web.xml pipeline (if necessary).
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author John Leacox
 */
interface FilterPipeline {
    void initPipeline(ServletContext context) throws ServletException;

    void destroyPipeline();

    void dispatch(ServletRequest request, ServletResponse response, FilterChain defaultFilterChain)
            throws IOException, ServletException;
}
