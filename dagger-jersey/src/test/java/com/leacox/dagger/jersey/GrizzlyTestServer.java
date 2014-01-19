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

package com.leacox.dagger.jersey;

import com.google.common.base.Throwables;
import com.leacox.dagger.servlet.DaggerFilter;
import com.leacox.dagger.servlet.DaggerServletContextListener;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.FilterRegistration;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

/**
 * @author John Leacox
 */
class GrizzlyTestServer {
    private final static Logger LOGGER = LoggerFactory.getLogger(GrizzlyTestServer.class);

    private DaggerFilter daggerFilter;
    private HttpServer httpServer;

    public <T extends DaggerServletContextListener> void startServer(Class<T> listenerClass) {
        LOGGER.info("Starting test server");

        WebappContext context = new WebappContext("Test", getUri().getRawPath());
        context.addListener(listenerClass);

        daggerFilter = new DaggerFilter();
        FilterRegistration filterRegistration = context.addFilter("daggerFilter", daggerFilter);
        filterRegistration.addMappingForUrlPatterns(null, "/*");

        ServletRegistration servletRegistration = context.addServlet("TestServlet", new HttpServlet() {
        });
        servletRegistration.addMapping("/dagger-jersey/*");

        try {
            httpServer = GrizzlyServerFactory.createHttpServer(getUri(), (HttpHandler) null);
            context.deploy(httpServer);
            httpServer.start();
            LOGGER.info("Test server started");
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public WebResource getRootResource() {
        Client client = Client.create();
        return client.resource(getUri());
    }

    public void stopServer() {
        if (httpServer != null) {
            daggerFilter.destroy();
            httpServer.stop();
            httpServer = null;
        }
        LOGGER.info("Test server stopped");
    }

    private URI getUri() {
        return UriBuilder.fromUri("http://localhost").port(8080).path("/dagger-jersey").build();
    }
}
