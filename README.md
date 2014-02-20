dagger-servlet
==============
Dagger Servlet is a port of the Guice Servlet project and Jersey Guice project for use with Dagger injection. It is split into two projects, dagger-servlet and dagger-jersey, for servlets and jersey respectively. Similarly to guice-servlet and jersey-guice these projects allow you to move most of your web application configuration out of the `web.xml` and into your Java code.

## Using dagger-servlet

### Include the dagger-servlet jar

```xml
<dependency>
  <groupId>com.leacox.dagger</groupId>
  <artifactId>dagger-servlet<artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Configuring the `web.xml`
Create a `ServletContextListener` for your application that extends from `DaggerServletContextListener`. Add your application context listener and `DaggerFilter` to the `web.xml` file.

```xml
<web-app>
    <listener>
        <listener-class>com.example.MyServletContextListener</listener-class>
    </listener>
    <filter>
        <filter-name>Dagger Filter</filter-name>
        <filter-class>com.leacox.dagger.servlet.DaggerFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>Dagger Filter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>
</web-app>
```

### Configuring Dagger modules
Dagger modules can be configured for application wide scope and for request scope. Start by creating an application wide Dagger module containing all of your application wide bindings. The application wide module should declare `ServletModule` as an `include` to get the `dagger-servlet` defined bindings. Next create another module for all of your request scoped bindings. Any `@Singleton` bindings defined in the request scoped module will be singletons for a given request. The request module must declare `ServletRequestModule` as an `include` to get the `dagger-servlet` request scoped bindings.

Add the modules to your `DaggerServletContextListener` implementation so that the dagger injections are available when the web application starts up.

```java
@Module(injects = {...}, includes = ServletModule.class)
public class MyModule {}

@Module(injects = {...}, includes = ServletRequestModule.class)
public class MyRequestModule {}

public class MyServletContextListener extends DaggerServletContextListener {
    @Override
    protected Class<?>[] getBaseModules() {
        return new Class<?>[]{MyModule.class};
    }

    @Override
    protected Class<?>[] getRequestScopedModules() {
        return new Class<?>[]{MyRequestModule.class};
    }
}
```

### Binding Servlets and Filters
With `DaggerServletContextListener` servlet and filter mappings can be configured in code instead of the `web.xml` file. These bindings are configured in the `configureServlets` method. Filters will be applied in the order they are declared.

```java
public class MyServletContextListener extends DaggerServletContextListener {
    @Override
    protected void configureServlets() {
        filter("/my/*").through(MyFilter.class);
        
        serve("/my/*").with(MyServlet.class);
    }
}
```

Every `Servlet` and `Filter` class must be a singleton. If you cannot add the `@Singleton` binding, then they must be defined in a singleton `@Provides` method.

### Using request scope
All request scoped bindings should be configured in a module that is included in the `DaggerServletContextListener#getRequestScopedModules`. To create a binding that has the same lifetime as a request declare it in your request scoped module and annotate the binding as `@Singleton`. Non-singleton bindings in the request module will create a new instance for each injection. Your module should also include `ServletRequestModule` to get the request scoped bindings provided by dagger-servlet.

`ServletRequestModule` provides the following request scoped bindings:
* javax.servlet.ServletRequest
* javax.servlet.ServletResponse
* javax.servlet.http.HttpSession

## Using dagger-jersey

### Configuring dagger-jersey
Start by configuring the web.xml file just like above for dagger-servlet. dagger-jersey provides `JerseyModule` and `JerseyRequestModule` that should be included in your application wide module and request module respectively. Jersey resource classes are typically request scoped, so you should add them to the `injects` field of your request module and mark them as `@Singleton`. Finally bind the `DaggerContainer` to `/*`.

Below is a simple example of a `DaggerServletContextListener` implementation and modules for Jersey:
```java
MyContextListener extends DaggerServletContextListener {
   @Override
   protected Class<?>[] getBaseModules() {
       return new Class<?>[]{ MyAppModule.class };
   }

   @Override
   protected Class<?>[] getRequestScopedModules() {
       return new Class<?>[]{ MyRequestModule.class };
   }

   @Override
   protected void configureServlets() {
       serve("/*").with(DaggerContainer.class);
   }
}

@Module(
        injects = MyClass.class,
        includes = JerseyModule.class
)
class MyAppModule {
    /* Your app wide provides here {@literal *}/
}

@Module(
        injects = {
            MyRequestScopedClass.class,
            MyResource.class
        },
        includes = JerseyRequestModule.class
)
class MyRequestModule {
    /* Your request scoped provides here {@literal *}/
}

@Singleton
class MyResource {}
```
