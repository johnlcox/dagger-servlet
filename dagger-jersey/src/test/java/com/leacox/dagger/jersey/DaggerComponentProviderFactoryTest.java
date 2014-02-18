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

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author John Leacox
 */
public class DaggerComponentProviderFactoryTest {
    @Provider
    static class SomeProvider {
        @Inject
        SomeProvider() {
        }
    }

    @Provider
    static class SomeProvidesInjectableProvider {
    }

    @Path("/some")
    static class SomeResource {
        @Inject
        SomeResource() {
        }
    }

    @Path("/some-provides")
    static class SomeProvidesInjectableResource {
    }

    @Path("/some-other-provides")
    static class SomeOtherProvidesInjectableResource {
    }

    @Path("/some-other")
    static class SomeOtherResource {
        @Inject
        SomeOtherResource() {
        }
    }

    @Module(injects = {SomeResource.class, SomeProvider.class})
    static class SomeModule {
    }

    @Module(library = true)
    static class SomeProvidesModule {
        @Provides
        SomeResource provideSomeResource() {
            return new SomeResource();
        }

        @Provides
        SomeProvider provideSomeProvider() {
            return new SomeProvider();
        }
    }

    @Module(injects = {SomeProvidesInjectableResource.class, SomeProvidesInjectableProvider.class})
    static class SomeInjectableProvidesModule {
        @Provides
        SomeProvidesInjectableResource provideSomeResource() {
            return new SomeProvidesInjectableResource();
        }

        @Provides
        SomeProvidesInjectableProvider provideSomeProvider() {
            return new SomeProvidesInjectableProvider();
        }
    }

    @Module(injects = SomeOtherResource.class)
    static class SomeOtherModule {
    }

    @Module(library = true)
    static class SomeOtherProvidesModule {
        @Provides
        SomeOtherResource provideSomeOtherResource() {
            return new SomeOtherResource();
        }
    }

    @Module(injects = SomeOtherProvidesInjectableResource.class)
    static class SomeOtherInjectableProvidesModule {
        @Provides
        SomeOtherProvidesInjectableResource provideSomeOtherResource() {
            return new SomeOtherProvidesInjectableResource();
        }
    }

    static class SomeNonModule {
    }

    @Test
    public void testModuleAnnotationRequiredOnModules() {
        ObjectGraph objectGraph = ObjectGraph.create(SomeModule.class);
        try {
            new DaggerComponentProviderFactory(new DefaultResourceConfig(), objectGraph,
                    new Object[]{SomeNonModule.class});
            fail();
        } catch (IllegalStateException e) {
            assertEquals("All dagger modules must be annotated with @Module", e.getMessage());
        }
    }

    @Test
    public void testClassesFromModuleInstancesAndModuleClassesAreRegistered() {
        ResourceConfig config = new DefaultResourceConfig();
        ObjectGraph objectGraph = ObjectGraph.create(SomeModule.class, new SomeOtherModule());
        new DaggerComponentProviderFactory(config, objectGraph, new Object[]{SomeModule.class, new SomeOtherModule()});

        assertTrue(config.getClasses().contains(SomeResource.class));
        assertTrue(config.getClasses().contains(SomeOtherResource.class));
    }

    @Test
    public void testClassesFromProvidesMethodsWithoutModuleInjectsAreNotRegistered() {
        ResourceConfig config = new DefaultResourceConfig();
        ObjectGraph objectGraph = ObjectGraph.create(SomeProvidesModule.class, new SomeOtherProvidesModule());
        new DaggerComponentProviderFactory(config, objectGraph,
                new Object[]{SomeProvidesModule.class, new SomeOtherProvidesModule()});

        assertFalse(config.getClasses().contains(SomeResource.class));
        assertFalse(config.getClasses().contains(SomeOtherResource.class));
        assertFalse(config.getClasses().contains(SomeProvider.class));
    }

    @Test
    public void testClassesFromProvidesMethodsWithModuleInjectsAndNoInjectsConstructorAreRegistered() {
        ResourceConfig config = new DefaultResourceConfig();
        ObjectGraph objectGraph = ObjectGraph.create(SomeInjectableProvidesModule.class,
                new SomeOtherInjectableProvidesModule());
        IoCComponentProviderFactory factory = new DaggerComponentProviderFactory(config, objectGraph,
                new Object[]{SomeInjectableProvidesModule.class, new SomeOtherInjectableProvidesModule()});

        assertTrue(config.getClasses().contains(SomeProvidesInjectableResource.class));
        assertTrue(config.getClasses().contains(SomeOtherProvidesInjectableResource.class));
        assertTrue(config.getClasses().contains(SomeProvidesInjectableProvider.class));

        SomeProvidesInjectableResource someResource = (SomeProvidesInjectableResource) factory.
                getComponentProvider(SomeProvidesInjectableResource.class).getInstance();
        SomeOtherProvidesInjectableResource someOtherResource = (SomeOtherProvidesInjectableResource) factory
                .getComponentProvider(SomeOtherProvidesInjectableResource.class).getInstance();
        SomeProvidesInjectableProvider someProvider = (SomeProvidesInjectableProvider) factory
                .getComponentProvider(SomeProvidesInjectableProvider.class).getInstance();

        assertNotNull(someResource);
        assertNotNull(someOtherResource);
        assertNotNull(someProvider);
    }

    @Test
    public void testProviderClassesAreRegistered() {
        ResourceConfig config = new DefaultResourceConfig();
        ObjectGraph objectGraph = ObjectGraph.create(SomeModule.class, new SomeOtherModule());
        new DaggerComponentProviderFactory(config, objectGraph, new Object[]{SomeModule.class, new SomeOtherModule()});

        assertTrue(config.getClasses().contains(SomeProvider.class));
    }
}
