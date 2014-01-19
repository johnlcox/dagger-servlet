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

package com.leacox.guice.example.simple;

import com.google.inject.servlet.RequestScoped;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * @author John Leacox
 */
@Path("/")
@RequestScoped
public class SimpleResource {
    private final SimpleService simpleService;

    @Inject
    SimpleResource(SimpleService simpleService) {
        this.simpleService = simpleService;
    }

    @Path("/display")
    @GET
    public Response getDisplay() {
        return Response.ok(simpleService.getDisplay()).build();
    }
}
