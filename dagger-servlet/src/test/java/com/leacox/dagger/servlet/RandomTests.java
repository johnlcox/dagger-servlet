package com.leacox.dagger.servlet;

import dagger.Module;
import dagger.ObjectGraph;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author John Leacox
 */
public class RandomTests {
    @Test
    public void testObjectGraphInjextion() {
        ObjectGraph objectGraph = ObjectGraph.create(new ServletModule());

        FilterPipeline filterPipeline = objectGraph.get(FilterPipeline.class);

        assertEquals(DaggerFilterPipeline.class, FilterPipeline.class);
    }
}
