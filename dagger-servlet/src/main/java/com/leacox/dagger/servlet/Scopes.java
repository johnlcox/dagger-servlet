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

    public static boolean isScopeAnnotation(Class<? extends Annotation> annotationType) {
        for (Annotation annotation : annotationType.getAnnotations()) {
            if (annotation.annotationType().equals(javax.inject.Scope.class)) {
                return true;
            }
        }

        return false;
    }
}
