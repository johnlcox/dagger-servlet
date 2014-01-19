/*
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

/**
 * Provides support for Dagger-based Jersey web applications.
 *
 * To enable Dagger support add the Dagger filter {@link com.leacox.dagger.servlet.DaggerFilter} and an application
 * specific {@code ServletContextListener} that extends from
 * {@link com.leacox.dagger.servlet.DaggerServletContextListener} in the web.xml:
 * <pre>
 *  &lt;web-app&gt;
 *    &lt;listener&gt;
 *      &lt;listener-class&gt;foo.MyContextListener&lt;/listener-class&gt;
 *    &lt;/listener&gt;
 *    &lt;filter&gt;
 *      &lt;filter-name&gt;daggerFilter&lt;/filter-name&gt;
 *      &lt;filter-class&gt;com.leacox.dagger.servlet.DaggerFilter&lt;/filter-class&gt;
 *    &lt;/filter&gt;
 *
 *    &lt;filter-mapping&gt;
 *      &lt;filter-name&gt;daggerFilter&lt;/filter-name&gt;
 *      &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 *    &lt;/filter-mapping&gt;
 *  &lt;/web-app&gt;
 * </pre>
 * <p>
 * In your context listener add your base and request scoped modules and bind the
 * {@link com.leacox.dagger.jersey.DaggerContainer} to "/*". A typical example is seen below with the modules
 * 'including' the proper Jersey module:
 * <pre>
 *  MyContextListener extends DaggerServletContextListener {
 *      {@literal @}Override
 *      protected Class<?>[] getBaseModules() {
 *          return new Class<?>[]{ MyAppModule.class };
 *      }
 *
 *      {@literal @}Override
 *      protected Class<?>[] getRequestScopedModules() {
 *          return new Class<?>[]{ MyRequestModule.class };
 *      }
 *
 *      {@literal @}Override
 *      protected void configureServlets() {
 *          serve("/*").with(DaggerContainer.class);
 *      }
 *  }
 * </pre>
 * <p>
 * Your dagger modules should include the proper Jersey modules (which provide JAX-RS and Jersey bindings and include
 * the {@link com.leacox.dagger.servlet.ServletModule} and {@link com.leacox.dagger.servlet.ServletRequestModule}
 * bindings respectively) and inject any {@code Resource} classes that should be injected by Dagger, like so:
 * <pre>
 *     {@literal @}Module(
 *             injects = MyClass.class,
 *             includes = JerseyModule.class
 *             )
 *     class MyAppModule {
 *         /* Your app wide provides here {@literal *}/
 *     }
 *
 *     {@literal @}Module(
 *             injects = {
 *                 MyRequestScopedClass.class,
 *                 DaggerResource.class
 *             },
 *             includes = JerseyRequestModule.class
 *             )
 *     class MyRequestModule {
 *         /* Your request scoped provides here {@literal *}/
 *     }
 * </pre>
 * <p>
 * Instances of the {@code DaggerResource} class will be managed by dagger within the scope of the module it is injected
 * by. By default resources are created for each request scope graph available to each resource is defined by the scope
 * of the module it is injected by. A resource injected by the app module will not be able to be injected by types
 * provided from a request scoped module. Because of this it is best to put your resource classes in your request scoped
 * module, unless you need to make a resource a singleton. If a resource should be a singleton it must be in an app
 * wide module and annotated with the {@link javax.inject.Singleton} annotation. Below are a couple example resources.
 * <p>
 * Default request scoped resource:
 * <pre>
 *     {@literal @}Module(
 *             injects = {
 *                 MySingletonResource.class,
 *                 MyObject.class
 *             },
 *             includes = JerseyModule.class
 *             )
 *     class MyAppModule {
 *         /* Your app wide provides here {@literal *}/
 *     }
 *
 *     {@literal @}Module(
 *             injects = {
 *                 MyRequestScopedResource.class,
 *                 MyNoScopedResource.class
 *             },
 *             includes = JerseyRequestModule.class
 *             )
 *     class MyRequestModule {
 *         /* Your request scoped provides here {@literal *}/
 *     }
 *
 *     // Even though this is annotated with {@literal @}Singleton it will be request scoped since it is bound in a
 *     // request scoped dagger module
 *     {@literal @}Path("requestscope")
 *     {@literal @}Singleton
 *     public class MyRequestScopedResource {
 *         private final MyObject myObject;
 *
 *         {@literal @}QueryParam param;
 *
 *         {@literal @}Inject
 *         MyRequestScopedResource(MyObject myObject) {
 *             this.myObject = myObject;
 *
 *             {@literal @}GET
 *             {@literal @}Produces("text/plain")
 *             public String getValue() {
 *                 return myObject.getValue() + param;
 *             }
 *         }
 *     }
 *
 *     // Will be request scoped with no scoping annotation
 *     {@literal @}Path("noscope")
 *     public class MyRequestScopedResource {
 *         private final MyObject myObject;
 *
 *         {@literal @}QueryParam param;
 *
 *         {@literal @}Inject
 *         MyRequestScopedResource(MyObject myObject) {
 *             this.myObject = myObject;
 *
 *             {@literal @}GET
 *             {@literal @}Produces("text/plain")
 *             public String getValue() {
 *                 return myObject.getValue() + param;
 *             }
 *         }
 *     }
 *
 *     // Only one instance of this class will exist for the lifetime of the web application.
 *     {@literal @}Path("singleton")
 *     {@literal @}Singleton
 *     public class MySingletonResource {
 *         private final MyObject myObject;
 *
 *         {@literal @}QueryParam param;
 *
 *         {@literal @}Inject
 *         MySingletonResource(MyObject myObject) {
 *             this.myObject = myObject;
 *
 *             {@literal @}GET
 *             {@literal @}Produces("text/plain")
 *             public String getValue() {
 *                 return myObject.getValue() + param;
 *             }
 *         }
 *     }
 * </pre>
 */
package com.leacox.dagger.jersey;