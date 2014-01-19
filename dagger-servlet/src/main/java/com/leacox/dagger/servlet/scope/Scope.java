package com.leacox.dagger.servlet.scope;

import dagger.ObjectGraph;

/**
 * @author John Leacox
 */
public interface Scope {
    public <T> T scope(Class<T> type, ObjectGraph baseGraph, Object[] scopedModules);

    public <T> T scopeInstance(T value, ObjectGraph baseGraph, Object[] scopedModules);

    String toString();
}
