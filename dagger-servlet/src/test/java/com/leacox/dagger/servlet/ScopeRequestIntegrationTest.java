/**
 * Copyright (C) 2010 Google Inc.
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import dagger.ScopingObjectGraph;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

/**
 * Tests continuation of requests
 */
public class ScopeRequestIntegrationTest {
    private static final String A_VALUE = "thereaoskdao";
    private static final String A_DIFFERENT_VALUE = "hiaoskd";

    private static final String SHOULDNEVERBESEEN = "Shouldneverbeseen!";

    @Module(
            injects = {
                    OffRequestCallable.class,
                    Caller.class,
                    ObjectGraph.class
            },
            includes = {
                    ServletModule.class
            }
    )
    static class TestAppModule {
    }

    @Module(
            injects = {
                    SomeObject.class
            },
            includes = {
                    ServletRequestModule.class
            }
    )
    static class TestRequestModule {
        @Provides
        @Named(SomeObject.INVALID)
        String provideInvalidString() {
            return SHOULDNEVERBESEEN;
        }
    }

    @Test
    public final void testNonHttpRequestScopedCallable()
            throws ServletException, IOException, InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ObjectGraph baseGraph = ObjectGraph.create(TestAppModule.class);
        ObjectGraph scopingGraph = ScopingObjectGraph.create(baseGraph)
                .addScopedModules(RequestScoped.class, TestRequestModule.class);
        ObjectGraph scopedGraph = baseGraph.plus(TestRequestModule.class);

        scopingGraph.get(InternalServletModule.ObjectGraphProvider.class).set(scopingGraph);

        SomeObject someObject = new SomeObject(A_VALUE);
        OffRequestCallable offRequestCallable = scopingGraph.get(OffRequestCallable.class); //new OffRequestCallable(scopingGraph);
        executor.submit(ServletScopes.scopeRequest(offRequestCallable,
                ImmutableMap.<Class<?>, Object>of(SomeObject.class, someObject,
                        ObjectGraph.class, scopedGraph))).get();

        assertSame(scopingGraph.get(OffRequestCallable.class), offRequestCallable);

        // Make sure the value was passed on.
        assertEquals(someObject.value, offRequestCallable.value);
        assertFalse(SHOULDNEVERBESEEN.equals(someObject.value));

        // Now create a new request and assert that the scopes don't cross.
        someObject = new SomeObject(A_DIFFERENT_VALUE);
        executor.submit(ServletScopes.scopeRequest(offRequestCallable,
                ImmutableMap.<Class<?>, Object>of(SomeObject.class, someObject))).get();

        assertSame(scopingGraph.get(OffRequestCallable.class), offRequestCallable);

        // Make sure the value was passed on.
        assertEquals(someObject.value, offRequestCallable.value);
        assertFalse(SHOULDNEVERBESEEN.equals(someObject.value));
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }

    @Test
    public final void testWrongValueClasses() throws Exception {
        ObjectGraph baseGraph = ObjectGraph.create(TestAppModule.class);
        ObjectGraph scopingGraph = ScopingObjectGraph.create(baseGraph)
                .addScopedModules(RequestScoped.class, TestRequestModule.class);
        ObjectGraph scopedGraph = baseGraph.plus(TestRequestModule.class);

        scopingGraph.get(InternalServletModule.ObjectGraphProvider.class).set(scopingGraph);

        OffRequestCallable offRequestCallable = scopingGraph.get(OffRequestCallable.class);
        try {
            ServletScopes.scopeRequest(offRequestCallable,
                    ImmutableMap.<Class<?>, Object>of(SomeObject.class, "Boo!",
                            ObjectGraph.class, scopedGraph));
            fail();
        } catch (IllegalArgumentException iae) {
            assertEquals("Value[Boo!] of type[java.lang.String] is not compatible with key[" +
                    ServletScopes.DaggerKey.get(SomeObject.class) + "]", iae.getMessage());
        }
    }

    @Test
    public final void testNullReplacement() throws Exception {
        ObjectGraph baseGraph = ObjectGraph.create(TestAppModule.class);
        ObjectGraph scopingGraph = ScopingObjectGraph.create(baseGraph)
                .addScopedModules(RequestScoped.class, TestRequestModule.class);

        scopingGraph.get(InternalServletModule.ObjectGraphProvider.class).set(scopingGraph);

        Callable<SomeObject> callable = scopingGraph.get(Caller.class);
        try {
            assertNotNull(callable.call());
            fail();
        } catch (IllegalArgumentException iae) {
        }

        // Validate that an actual null entry in the map results in a null injected object.
        Map<Class<?>, Object> map = Maps.newHashMap();
        map.put(SomeObject.class, null);
        callable = ServletScopes.scopeRequest(new Caller(scopingGraph), map);
        assertNull(callable.call());
    }

    @Singleton
    public static class SomeObject {
        private static final String INVALID = "invalid";

        @Inject
        public SomeObject(@Named(INVALID) String value) {
            this.value = value;
        }

        private String value;
    }

    @Singleton
    public static class OffRequestCallable implements Callable<String> {
        ObjectGraph objectGraph;

        @Inject
        OffRequestCallable(ObjectGraph objectGraph) {
            this.objectGraph = objectGraph;
        }

        public String value;

        @Override
        public String call() throws Exception {
            SomeObject someObject = objectGraph.get(SomeObject.class);

            // Inside this request, we should always get the same instance.
            assertSame(someObject, objectGraph.get(SomeObject.class));

            value = someObject.value;
            assertFalse(SHOULDNEVERBESEEN.equals(value));

            return value;
        }
    }

    public static class Caller implements Callable<SomeObject> {
        ObjectGraph objectGraph;

        @Inject
        Caller(ObjectGraph objectGraph) {
            this.objectGraph = objectGraph;
        }

        @Override
        public SomeObject call() throws Exception {
            return objectGraph.get(SomeObject.class);
        }
    }
}
