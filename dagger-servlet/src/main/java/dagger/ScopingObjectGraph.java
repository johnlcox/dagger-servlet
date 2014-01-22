package dagger;

import com.google.common.collect.Maps;
import com.leacox.dagger.servlet.RequestScoped;
import com.leacox.dagger.servlet.DaggerFilter;
import com.leacox.dagger.servlet.SessionScoped;

import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * @author John Leacox
 */
// HACK: This has to be in the dagger package due to the ObjectGraph constructor being package private. Once dagger
// supports scoping itself, this can hopefully be reworked.
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

//        Class<? extends Annotation> scope = findNonSingletonScope(type.getAnnotations());
//
//        if (scope == null) {
//            return objectGraph.get(type);
//        } else if (scope.equals(RequestScoped.class)) {
//            ServletRequest request = DaggerFilter.getRequest();
//            synchronized (request) {
//                ObjectGraph requestGraph = (ObjectGraph) request.getAttribute(ScopingObjectGraph.class.getName());
//                if (requestGraph == null) {
//                    requestGraph = objectGraph.plus(scopedModules.get(RequestScoped.class));
//                    request.setAttribute(ScopingObjectGraph.class.getName(), requestGraph);
//                }
//
//                return requestGraph.get(type);
//            }
//        } // TODO: SessionScope
//
//        // TODO: Unknown scope?
//        throw new RuntimeException("Unknown scope: " + scope);
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

//        Class<? extends Annotation> scope = findNonSingletonScope(instance.getClass().getAnnotations());
//
//        if (scope == null) {
//            return objectGraph.inject(instance);
//        } else if (scope.equals(RequestScoped.class)) {
//            ServletRequest request = DaggerFilter.getRequest();
//            synchronized (request) {
//                ObjectGraph requestGraph = (ObjectGraph) request.getAttribute(ScopingObjectGraph.class.getName());
//                if (requestGraph == null) {
//                    requestGraph = objectGraph.plus(scopedModules.get(RequestScoped.class));
//                    request.setAttribute(ScopingObjectGraph.class.getName(), requestGraph);
//                }
//
//                return requestGraph.inject(instance);
//            }
//        } // TODO: SessionScope
//
//        // TODO: Unknown scope?
//        throw new RuntimeException("Unknown scope: " + scope);
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

    @Override
    public void injectStatics() {
        // TODO: Should this do anything scope related?
        objectGraph.injectStatics();
    }

    private static boolean hasSingletonScope(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.equals(Singleton.class)) {
                return true;
            }
        }

        return false;
    }

    private static Class<? extends Annotation> findNonSingletonScope(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (!annotationType.equals(Singleton.class) && isScopeAnnotation(annotationType)) {
                return annotationType;
            }
        }

        return null;
    }

    public static boolean isScopeAnnotation(Class<? extends Annotation> annotationType) {
        for (Annotation annotation : annotationType.getAnnotations()) {
            if (annotation.annotationType().equals(javax.inject.Scope.class)) {
                return true;
            }
        }

        return false;
    }
}
