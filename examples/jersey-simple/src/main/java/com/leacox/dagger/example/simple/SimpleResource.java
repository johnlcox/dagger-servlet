package com.leacox.dagger.example.simple;

import com.leacox.dagger.servlet.DaggerContainer;
import dagger.ObjectGraph;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * @author John Leacox
 */
@Path("/")
public class SimpleResource extends DaggerContainer {
    @Inject
    SimpleService simpleService;

    @Inject
    SimpleResource(ObjectGraph objectGraph, Class<?>[] modules) {
        super(objectGraph, modules);
    }

    @Path("display")
    @GET
    public Response getDisplay() {
        return Response.ok(simpleService.getDisplay()).build();
    }
}
