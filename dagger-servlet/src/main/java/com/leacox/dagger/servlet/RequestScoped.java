package com.leacox.dagger.servlet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author John Leacox
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ScopeAnnotation
public @interface RequestScoped {
}

// TODO: Figure out how to actually implement scope.
// This might be helpful: https://github.com/square/dagger/pull/72/files