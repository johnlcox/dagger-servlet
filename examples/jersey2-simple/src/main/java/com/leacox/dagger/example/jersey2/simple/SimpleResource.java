package com.leacox.dagger.example.jersey2.simple;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * @author John Leacox
 */
@Path("/")
public class SimpleResource {
    @Inject
    SimpleService simpleService;

//    @Inject
//    SimpleResource(ObjectGraph objectGraph) {
//        //super(objectGraph, modules);
//    }

    @Path("/display")
    @GET
    public Response getDisplay() {
        return Response.ok(simpleService.getDisplay()).build();
    }
}
