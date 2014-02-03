package com.leacox.dagger.servlet.scope;

import dagger.Lazy;

/**
 * @author John Leacox
 */
public interface Scope {
    public <T> Lazy<T> scope(Class<T> type, Lazy<T> unscoped);

    String toString();
}
