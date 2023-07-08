# Cactus-DI - A simple dependency injection container


This is Java library that provides a class `com.qualifiedcactus.cactusDi.DiContainer` that manage dependency-injected objects.
Instance, singleton and scoped dependency types is supported.
This library is an alternative to existing annotation-based dependency injection frameworks.

## Install from dependency maven central

```xml

```

## How to use

Create classes, register them and then call `DiContainer.getDependency()`.

```kotlin
// create dependencies
class DependencyA  {
    // ...
}

class DependencyB(
    val dependencyA: DependencyA // dependency injected via constructor
) {
    // ...
}


fun main() {
    // create a container
    val container = DiContainer()
    
    // register dependencies
    container.registerSingleton(DependencyA::class.java)
    container.registerSingleton(DependencyB::class.java)
    
    // get dependency
    val dependencyB = container.getDependency(DependencyB::class.java) 
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

Apache License 2.0. See [NOTICE](NOTICE) and [LICENSE](LICENSE) for more information.