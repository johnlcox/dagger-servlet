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

        synchronized (request) {
            HttpSession session = request.getSession();

            ObjectGraph sessionGraph = (ObjectGraph) session.getAttribute(ScopingObjectGraph.class.getName());
            if (sessionGraph == null) {
                Object[] modules = scopedModules.get(SessionScoped.class);
                sessionGraph = modules != null ? objectGraph.plus(modules) : objectGraph;
                session.setAttribute(ScopingObjectGraph.class.getName(), sessionGraph);
            }

            ObjectGraph requestGraph = (ObjectGraph) request.getAttribute(ScopingObjectGraph.class.getName());
            if (requestGraph == null) {
                Object[] modules = scopedModules.get(RequestScoped.class);
                requestGraph = modules != null ? sessionGraph.plus(modules) : sessionGraph;
                request.setAttribute(ScopingObjectGraph.class.getName(), requestGraph);
            }

            return requestGraph.get(type);
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
