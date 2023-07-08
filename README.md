# Cactus-DI - A simple dependency injection container

This is a light-weight Java dependency injection library written in Kotlin.
Instance, singleton and scoped dependency types are supported.
This is an alternative to existing annotation-based dependency injection frameworks.

## Maven central

```xml
<dependency>
    <groupId>com.qualifiedcactus</groupId>
    <artifactId>cactus-di</artifactId>
    <version>0.0.1</version>
</dependency>
```

## How to use

To create a container, follow these steps:

- Write your classes and its dependencies (please no circular dependency).
- Initialize `com.qualifiedcactus.cactusDi.DiContainer`.
- Register your classes and instances using methods provided by `DiContainer`
- (Optional) Register a runnable class that be dependency injected, or a runnble instance
- Now you can use `DiContainer.startRunnables()` and `DiContainer.getDependency()`
- (Optional) If your dependencies implement `Autoclosble`, you can close them using `DiContainer.close()` 

Below is a simple example:

```kotlin
// create dependencies
class DependencyA  {
    // ...
}

class DependencyC {
    // ...
}

class DependencyB(
    val dependencyA: DependencyA, // dependency injected via constructor
    val dependencyC: DependencyC,
) {
    // ...
}

fun main() {
    val instanceOfDependencyC = DependencyC()
    // create a container
    val container = DiContainer()
    
    // register dependencies
    container.registerInstance(instanceOfDependencyC)
    container.registerScoped(DependencyA::class.java)
    container.registerSingleton(DependencyB::class.java)
    
    // get dependency
    val dependencyB = container[DependencyB::class.java]
}
```

For more information, see the documentation of the class `DiContainer`.


## Testing a dependency-injected class

During test, if you wish to replace a dependency with a mock object, use true on `overrideIfExists`:

```kotlin
diContainer.registerInstance(
    singletonInstance = mockObject, // mocked object which can provided by you or other libraries such as Mockito
    alias = DependencyA::class.java, // DependencyA  will be replaced with mock object
    overrideIfExists = true
)
```

## License

Apache License Version 2.0. See [NOTICE](NOTICE) and [LICENSE](LICENSE) for more information.