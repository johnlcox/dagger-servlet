package com.leacox.dagger.servlet;

import javax.inject.Singleton;
import java.lang.annotation.Annotation;

/**
 * @author John Leacox
 */
class Scopes {
    public static boolean isSingleton(Class clazz) {
        Annotation[] annotations = clazz.getAnnotations();
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.equals(Singleton.class)) {
                return true;
            }
        }

        return false;
    }
}
