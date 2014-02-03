package com.leacox.dagger.servlet.scope;

import dagger.Lazy;
import dagger.ObjectGraph;

/**
 * @author John Leacox
 */
public interface Scope {
    public <T> Lazy<T> scope(Class<T> type, ObjectGraph objectGraph);

    String toString();
}
