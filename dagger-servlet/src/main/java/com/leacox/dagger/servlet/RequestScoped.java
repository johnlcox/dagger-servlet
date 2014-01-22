package com.leacox.dagger.servlet;

import javax.inject.Scope;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Currently Dagger only allows only a single scope annotation on a type, but does not support actual scoping. For now
 * ll request scoped types should be annotated with {@code Singleton} and configured in a request module provided to
 * dagger-servlet via {@link DaggerServletContextListener#getRequestScopedModules()}.
 *
 * @author John Leacox
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Scope
public @interface RequestScoped {
}

// TODO: Figure out how to actually implement scope.
// This might be helpful: https://github.com/square/dagger/pull/72/files