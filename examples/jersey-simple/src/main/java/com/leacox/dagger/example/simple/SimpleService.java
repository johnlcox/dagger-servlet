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

package com.leacox.dagger.example.simple;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author John Leacox
 */
@Singleton
public class SimpleService {
    private final String display;

    @Inject
    SimpleService(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
