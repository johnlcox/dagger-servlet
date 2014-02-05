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
import com.leacox.dagger.servlet.ServletScopes;
import com.leacox.dagger.servlet.SessionScoped;
import com.leacox.dagger.servlet.scope.Scope;

import javax.servlet.http.HttpServletRequest;
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

    private final Scope requestScope = ServletScopes.REQUEST;
    private final Scope sessionScope = ServletScopes.SESSION;

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
        if (request == null && !ServletScopes.isNonHttpRequestScope()) {
            return objectGraph.get(type);
        }

//        ObjectGraph scopedGraph = objectGraph.plus(scopedModules.get(SessionScoped.class)).plus(
//                scopedModules.get(RequestScoped.class));

        if (isRequestScoped(type)) {
            return requestScope.scope(type, objectGraph, scopedModules.get(RequestScoped.class)); //scopedGraph);
        } else if (isSessionScoped(type)) {
            return sessionScope.scope(type, objectGraph, scopedModules.get(SessionScoped.class)); //scopedGraph);
        } else {
            return objectGraph.get(type);
        }
    }

    @Override
    public <T> T inject(T instance) {
        HttpServletRequest request = DaggerFilter.getRequest();

        if (request == null && !ServletScopes.isNonHttpRequestScope()) {
            return objectGraph.inject(instance);
        }

        //synchronized (request) {
        if (isRequestScoped(instance.getClass())) {
            //ObjectGraph scopedGraph = objectGraph.plus(scopedModules.get(RequestScoped.class));
            //return scopedGraph.inject(instance);
            return requestScope.scopeInstance(instance, objectGraph, scopedModules.get(RequestScoped.class)); //scopedGraph);
        } else if (isSessionScoped(instance.getClass())) {
            //ObjectGraph scopedGraph = objectGraph.plus(scopedModules.get(SessionScoped.class));
            //return scopedGraph.inject(instance);
            return requestScope.scopeInstance(instance, objectGraph, scopedModules.get(SessionScoped.class)); //scopedGraph);
        } else {
            return objectGraph.inject(instance);
        }
        //}
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
        // Only need to inject on the base object graph. Plused modules do not do static injection.
        objectGraph.injectStatics();
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

            if (arrayContains(module.injects(), type)) {
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

            if (arrayContains(module.injects(), type)) {
                return true;
            }
        }

        return false;
    }

    private static <T> boolean arrayContains(final T[] array, final T value) {
        for (final T element : array) {
            if (element == value || (value != null && value.equals(element))) {
                return true;
            }
        }

        return false;
    }
}
