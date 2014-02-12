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
