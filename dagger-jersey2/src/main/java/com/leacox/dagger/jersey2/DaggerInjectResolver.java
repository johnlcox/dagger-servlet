package com.leacox.dagger.jersey2;

import dagger.ObjectGraph;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author John Leacox
 */
@Singleton
public class DaggerInjectResolver implements InjectionResolver<Inject> {
    private final ObjectGraph objectGraph;

    public DaggerInjectResolver(ObjectGraph objectGraph) {
        this.objectGraph = objectGraph;
    }

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> root) {
        Class<?> clazz = getClassFromType(injectee.getRequiredType());

        return objectGraph.get(clazz);
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        return false;
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return false;
    }

    private Class<?> getClassFromType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }

        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            return (Class<?>) parameterizedType.getRawType();
        }

        return null;
    }
}
