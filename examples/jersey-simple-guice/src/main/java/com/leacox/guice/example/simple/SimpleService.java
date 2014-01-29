package com.leacox.guice.example.simple;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author John Leacox
 */
@Singleton
public class SimpleService {
    private final String display;

    @Inject
    SimpleService(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
