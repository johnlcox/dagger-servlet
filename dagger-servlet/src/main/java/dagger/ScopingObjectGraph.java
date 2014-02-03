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

package dagger;

import com.google.common.collect.Maps;
import com.leacox.dagger.servlet.RequestScoped;
import com.leacox.dagger.servlet.DaggerFilter;
import com.leacox.dagger.servlet.SessionScoped;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * An ObjectGraph with request and session scoping. This class should not be used directly.
 * <p/>
 * HACK: This has to be in the dagger package due to the ObjectGraph constructor being package private. Once dagger
 * supports scoping itself, this can hopefully be reworked.
 *
 * @author John Leacox
 */
public class ScopingObjectGraph extends ObjectGraph {
    private final ObjectGraph objectGraph;
    private final Map<Class<? extends Annotation>, Object[]> scopedModules;

    ScopingObjectGraph(ObjectGraph objectGraph, Map<Class<? extends Annotation>, Object[]> scopedModules) {
        this.objectGraph = objectGraph;
        this.scopedModules = scopedModules;
    }

    public static ScopingObjectGraph create(ObjectGraph objectGraph) {
        return new ScopingObjectGraph(objectGraph, Maps.<Class<? extends Annotation>, Object[]>newHashMap());
    }

    public ScopingObjectGraph addScopedModules(Class<? extends Annotation> scope, Object... modules) {
        scopedModules.put(scope, modules);
        return new ScopingObjectGraph(objectGraph, scopedModules);
    }

    @Override
    public <T> T get(Class<T> type) {
        HttpServletRequest request = DaggerFilter.getRequest();
        if (request == null) {
            return objectGraph.get(type);
        }

        ObjectGraph scopedGraph = objectGraph.plus(scopedModules.get(SessionScoped.class)).plus(
                scopedModules.get(RequestScoped.class));

        if (isRequestScoped(type)) {
            return requestScopedGet(type, scopedGraph);
        } else if (isSessionScoped(type)) {
            return sessionScopedGet(type, scopedGraph);
        } else {
            return scopedGraph.get(type);
        }

//        synchronized (request) {
//            HttpSession session = request.getSession();
//
//            ObjectGraph sessionGraph = (ObjectGraph) session.getAttribute(ScopingObjectGraph.class.getName());
//            if (sessionGraph == null) {
//                Object[] modules = scopedModules.get(SessionScoped.class);
//                sessionGraph = modules != null ? objectGraph.plus(modules) : objectGraph;
//                session.setAttribute(ScopingObjectGraph.class.getName(), sessionGraph);
//            }
//
//            ObjectGraph requestGraph = (ObjectGraph) request.getAttribute(ScopingObjectGraph.class.getName());
//            if (requestGraph == null) {
//                Object[] modules = scopedModules.get(RequestScoped.class);
//                requestGraph = modules != null ? sessionGraph.plus(modules) : sessionGraph;
//                request.setAttribute(ScopingObjectGraph.class.getName(), requestGraph);
//            }
//
//            return requestGraph.get(type);
//        }
    }

    private <T> T requestScopedGet(Class<T> type, ObjectGraph objectGraph) {
        // Check if the alternate request scope should be used, if no HTTP
        // request is in progress.
//            if (null == DaggerFilter.localContext.get()) {
//
//                // NOTE(dhanji): We don't need to synchronize on the scope map
//                // unlike the HTTP request because we're the only ones who have
//                // a reference to it, and it is only available via a threadlocal.
//                Map<String, Object> scopeMap = requestScopeContext.get();
//                if (null != scopeMap) {
//                    @SuppressWarnings("unchecked")
//                    T t = (T) scopeMap.get(name);
//
//                    // Accounts for @Nullable providers.
//                    if (NullObject.INSTANCE == t) {
//                        return null;
//                    }
//
//                    if (t == null) {
//                        t = creator.get();
//                        // Store a sentinel for provider-given null values.
//                        scopeMap.put(name, t != null ? t : NullObject.INSTANCE);
//                    }
//
//                    return t;
//                } // else: fall into normal HTTP request scope and out of scope
//                // exception is thrown.
//            }

        HttpServletRequest request = DaggerFilter.getRequest();
        String name = "DaggerKey[type=" + type + "]";

        synchronized (request) {
            Object obj = request.getAttribute(name);
//            if (NullObject.INSTANCE == obj) {
//                return null;
//            }
            @SuppressWarnings("unchecked")
            T t = (T) obj;
            if (t == null) {
                t = objectGraph.get(type);
                //request.setAttribute(name, (t != null) ? t : NullObject.INSTANCE);
                request.setAttribute(name, t);
            }
            return t;
        }
    }

    private <T> T sessionScopedGet(Class<T> type, ObjectGraph objectGraph) {
        HttpSession session = DaggerFilter.getRequest().getSession();
        synchronized (session) {
            String name = "DaggerKey[type=" + type + "]";
            Object obj = session.getAttribute(name);
//            if (NullObject.INSTANCE == obj) {
//                return null;
//            }
            @SuppressWarnings("unchecked")
            T t = (T) obj;
            if (t == null) {
                t = objectGraph.get(type);
                //session.setAttribute(name, (t != null) ? t : NullObject.INSTANCE);
                session.setAttribute(name, t);
            }
            return t;
        }
    }

    @Override
    public <T> T inject(T instance) {
        HttpServletRequest request = DaggerFilter.getRequest();

        if (request == null) {
            return objectGraph.inject(instance);
        }

        synchronized (request) {
            HttpSession session = request.getSession();

            ObjectGraph sessionGraph = (ObjectGraph) session.getAttribute(ScopingObjectGraph.class.getName());
            if (sessionGraph == null) {
                sessionGraph = objectGraph.plus(scopedModules.get(SessionScoped.class));
                session.setAttribute(ScopingObjectGraph.class.getName(), sessionGraph);
            }

            ObjectGraph requestGraph = (ObjectGraph) request.getAttribute(ScopingObjectGraph.class.getName());
            if (requestGraph == null) {
                requestGraph = sessionGraph.plus(scopedModules.get(RequestScoped.class));
                request.setAttribute(ScopingObjectGraph.class.getName(), requestGraph);
            }

            return requestGraph.inject(instance);
        }
    }

    private <T> boolean isRequestScoped(Class<T> type) {
        Object[] requestScopedModules = scopedModules.get(RequestScoped.class);
        for (Object requestScopedModule : requestScopedModules) {
            Module module;
            if (requestScopedModule instanceof Class<?>) {
                module = ((Class<?>) requestScopedModule).getAnnotation(Module.class);
            } else {
                module = requestScopedModule.getClass().getAnnotation(Module.class);
            }

            if (contains(module.injects(), type)) {
                return true;
            }
        }

        return false;
    }

    private <T> boolean isSessionScoped(Class<T> type) {
        Object[] sessionScopedModules = scopedModules.get(SessionScoped.class);
        for (Object sessionScopedModule : sessionScopedModules) {
            Module module;
            if (sessionScopedModule instanceof Class<?>) {
                module = ((Class<?>) sessionScopedModule).getAnnotation(Module.class);
            } else {
                module = sessionScopedModule.getClass().getAnnotation(Module.class);
            }

            if (contains(module.injects(), type)) {
                return true;
            }
        }

        return false;
    }

    private static <T> boolean contains(final T[] array, final T value) {
        for (final T element : array) {
            if (element == value || (value != null && value.equals(element))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public ObjectGraph plus(Object... modules) {
        return new ScopingObjectGraph(objectGraph.plus(modules), scopedModules);
    }

    @Override
    public void validate() {
        objectGraph.validate();

        for (Object[] modules : scopedModules.values()) {
            objectGraph.plus(modules).validate();
        }
    }

    // TODO: Add validation of classes and objects so we can make sure the filter/servlet classes have injections
    // and fail early instead of when we try to inject it on an actual request.

    @Override
    public void injectStatics() {
        // TODO: Should this do anything scope related?
        objectGraph.injectStatics();
    }
}
