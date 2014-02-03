/**
 * Copyright (C) 2006 Google Inc.
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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.leacox.dagger.servlet.scope.Scope;
import dagger.Lazy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author John Leacox
 */
public class ServletScopes {
    private ServletScopes() {}

    /**
     * A sentinel attribute value representing null.
     */
    enum NullObject {
        INSTANCE
    }

    private static class DaggerKey {
        private final Class<?> type;

        private DaggerKey(Class<?> type) {
            this.type = type;
        }

        public static DaggerKey get(Class<?> type) {
            return new DaggerKey(type);
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DaggerKey that = (DaggerKey) o;
            return Objects.equal(this.type, that.type);
        }

        @Override
        public final int hashCode() {
            return Objects.hashCode(type);
        }

        @Override
        public final String toString() {
            return "DaggerKey[type=" + type + "]";
        }
    }

    /**
     * HTTP servlet request scope.
     */
    public static final Scope REQUEST = new Scope() {
        @Override
        public <T> Lazy<T> scope(Class<T> type, final Lazy<T> creator) {
            final String name = DaggerKey.get(type).toString(); //"DaggerKey[type=" + type + "]";
            return new Lazy<T>() {
                @Override
                public T get() {
                    // Check if the alternate request scope should be used, if no HTTP
                    // request is in progress.
                    if (null == DaggerFilter.localContext.get()) {

                        // NOTE(dhanji): We don't need to synchronize on the scope map
                        // unlike the HTTP request because we're the only ones who have
                        // a reference to it, and it is only available via a threadlocal.
                        Map<String, Object> scopeMap = requestScopeContext.get();
                        if (null != scopeMap) {
                            @SuppressWarnings("unchecked")
                            T t = (T) scopeMap.get(name);

                            // Accounts for @Nullable providers.
                            if (NullObject.INSTANCE == t) {
                                return null;
                            }

                            if (t == null) {
                                t = creator.get();
                                // Store a sentinel for provider-given null values.
                                scopeMap.put(name, t != null ? t : NullObject.INSTANCE);
                            }

                            return t;
                        } // else: fall into normal HTTP request scope and out of scope
                        // exception is thrown.
                    }

                    HttpServletRequest request = DaggerFilter.getRequest();

                    synchronized (request) {
                        Object obj = request.getAttribute(name);
                        if (NullObject.INSTANCE == obj) {
                            return null;
                        }
                        @SuppressWarnings("unchecked")
                        T t = (T) obj;
                        if (t == null) {
                            t = creator.get();
                            request.setAttribute(name, (t != null) ? t : NullObject.INSTANCE);
                        }
                        return t;
                    }
                }

                public String toString() {
                    return String.format("%s[%s]", creator, REQUEST);
                }
            };
        }
    };

    /**
     * HTTP session scope.
     */
    public static final Scope SESSION = new Scope() {
        @Override
        public <T> Lazy<T> scope(Class<T> type, final Lazy<T> creator) {
            final String name = DaggerKey.get(type).toString(); //"DaggerKey[type=" + type + "]";
            return new Lazy<T>() {
                @Override
                public T get() {
                    HttpSession session = DaggerFilter.getRequest().getSession();
                    synchronized (session) {
                        Object obj = session.getAttribute(name);
                        if (NullObject.INSTANCE == obj) {
                            return null;
                        }
                        @SuppressWarnings("unchecked")
                        T t = (T) obj;
                        if (t == null) {
                            t = creator.get();
                            session.setAttribute(name, (t != null) ? t : NullObject.INSTANCE);
                        }
                        return t;
                    }
                }

                public String toString() {
                    return String.format("%s[%s]", creator, SESSION);
                }
            };
        }

        public String toString() {
            return "ServletScopes.SESSION";
        }
    };

    /**
     * Wraps the given callable in a contextual callable that "continues" the
     * HTTP request in another thread. This acts as a way of transporting
     * request context data from the request processing thread to to worker
     * threads.
     * <p/>
     * There are some limitations:
     * <ul>
     * <li>Derived objects (i.e. anything marked @RequestScoped will not be
     * transported.</li>
     * <li>State changes to the HttpServletRequest after this method is called
     * will not be seen in the continued thread.</li>
     * <li>Only the HttpServletRequest, ServletContext and request parameter
     * map are available in the continued thread. The response and session
     * are not available.</li>
     * </ul>
     *
     * @param callable code to be executed in another thread, which depends on
     * the request scope.
     * @param seedMap the initial set of scoped instances for Guice to seed the
     * request scope with.  To seed a key with null, use {@code null} as
     * the value.
     * @return a callable that will invoke the given callable, making the request
     * context available to it.
     * @throws com.leacox.dagger.servlet.scope.OutOfScopeException if this method is called from a non-request
     *                                                             thread, or if the request has completed.
     */
    public static <T> Callable<T> continueRequest(final Callable<T> callable,
                                                  final Map<Class<?>, Object> seedMap) {
        Preconditions.checkArgument(null != seedMap,
                "Seed map cannot be null, try passing in Collections.emptyMap() instead.");

        // Snapshot the seed map and add all the instances to our continuing HTTP request.
        final ContinuingHttpServletRequest continuingRequest =
                new ContinuingHttpServletRequest(DaggerFilter.getRequest());
        for (Map.Entry<Class<?>, Object> entry : seedMap.entrySet()) {
            Object value = validateAndCanonicalizeValue(entry.getKey(), entry.getValue());
            continuingRequest.setAttribute(DaggerKey.get(entry.getKey()).toString(), value);
        }

        return new Callable<T>() {
            private HttpServletRequest request = continuingRequest;

            @Override
            public T call() throws Exception {
                DaggerFilter.Context context = DaggerFilter.localContext.get();
                Preconditions.checkState(null == context,
                        "Cannot continue request in the same thread as a HTTP request!");

                // Only set up the request continuation if we're running in a
                // new vanilla thread.
                DaggerFilter.localContext.set(new DaggerFilter.Context(request, null));
                try {
                    return callable.call();
                } finally {
                    // Clear the copied context if we set one up.
                    if (null == context) {
                        DaggerFilter.localContext.remove();
                    }
                }
            }
        };
    }

    /**
     * A threadlocal scope map for non-http request scopes. The {@link #REQUEST}
     * scope falls back to this scope map if no http request is available, and
     * requires {@link #scopeRequest} to be called as an alertnative.
     */
    private static final ThreadLocal<Map<String, Object>> requestScopeContext
            = new ThreadLocal<Map<String, Object>>();

    /**
     * Scopes the given callable inside a request scope. This is not the same
     * as the HTTP request scope, but is used if no HTTP request scope is in
     * progress. In this way, keys can be scoped as @RequestScoped and exist
     * in non-HTTP requests (for example: RPC requests) as well as in HTTP
     * request threads.
     *
     * @param callable code to be executed which depends on the request scope.
     * Typically in another thread, but not necessarily so.
     * @param seedMap the initial set of scoped instances for Guice to seed the
     * request scope with.  To seed a key with null, use {@code null} as
     * the value.
     * @return a callable that when called will run inside the a request scope
     * that exposes the instances in the {@code seedMap} as scoped keys.
     * @since 3.0
     */
    public static <T> Callable<T> scopeRequest(final Callable<T> callable,
                                               Map<Class<?>, Object> seedMap) {
        Preconditions.checkNotNull(seedMap, "Seed map cannot be null, try passing in Collections.emptyMap() instead.");

        // Copy the seed values into our local scope map.
        final Map<String, Object> scopeMap = Maps.newHashMap();
        for (Map.Entry<Class<?>, Object> entry : seedMap.entrySet()) {
            Object value = validateAndCanonicalizeValue(entry.getKey(), entry.getValue());
            scopeMap.put(DaggerKey.get(entry.getKey()).toString(), value);
        }

        return new Callable<T>() {
            @Override
            public T call() throws Exception {
                Preconditions.checkState(null == DaggerFilter.localContext.get(),
                        "An HTTP request is already in progress, cannot scope a new request in this thread.");
                Preconditions.checkState(null == requestScopeContext.get(),
                        "A request scope is already in progress, cannot scope a new request in this thread.");

                requestScopeContext.set(scopeMap);

                try {
                    return callable.call();
                } finally {
                    requestScopeContext.remove();
                }
            }
        };
    }

    /**
     * Validates the type and object, ensuring the value matches the type, and
     * canonicalizing null objects to the null sentinel.
     */
    private static Object validateAndCanonicalizeValue(Class<?> type, Object object) {
        if (object == null || object == NullObject.INSTANCE) {
            return NullObject.INSTANCE;
        }

        if (!type.isInstance(object)) {
            throw new IllegalArgumentException("Value[" + object + "] of type["
                    + object.getClass().getName() + "] is not compatible with type[" + type + "]");
        }

        return object;
    }
}
