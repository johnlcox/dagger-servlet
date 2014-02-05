package com.leacox.dagger.servlet.scope;

import dagger.Lazy;
import dagger.ObjectGraph;

/**
 * @author John Leacox
 */
public interface Scope {
    public <T> T scope(Class<T> type, ObjectGraph baseGraph, Object[] scopedModules); //ObjectGraph objectGraph);

    public <T> T scopeInstance(T value, ObjectGraph baseGraph, Object[] scopedModules); //ObjectGraph objectGraph);

    String toString();
}
