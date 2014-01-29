package com.leacox.dagger.example.simple;

import com.leacox.dagger.servlet.DaggerContainer;
import com.leacox.dagger.servlet.DaggerFilter;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.DefaultResourceConfig;

import javax.ws.rs.ApplicationPath;

/**
 * @author John Leacox
 */
@ApplicationPath("/")
public class SimpleApplication extends ClassNamesResourceConfig {
    public SimpleApplication() {
        super(DaggerContainer.class);
    }
}
