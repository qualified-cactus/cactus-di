/*
 * Copyright 2023 qualified-cactus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("UNCHECKED_CAST")
package com.qualifiedcactus.cactusDi

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.hasAnnotation

/**
 * To use a container, register dependencies then call [get] / [startRunnables].
 *
 * A dependency can be of 3 types:
 * - Instance: an object initialized outside this container. Can be registered using [registerInstance]
 * - Singleton: A class with only 1 lazily initialized instance (thread-safe). Can be registered using [registerSingleton]
 * - Scoped: A class whose new instance is created for each singleton or [get] call. Can be registered using [registerScoped]
 *
 * Classes (can use registered dependencies) or instances implementing [Runnable] can be registered via [registerRunnable] or [registerRunnableInstance].
 * They can be started in REGISTRATION ORDER using [startRunnables].
 *
 * A dependency can have child dependencies injected into its constructor.
 * A child dependency is specified using constructor's parameters' type (class's type parameter is ignored).
 * If more than 1 public constructor exists, [DiConstructor] annotation must be used to specify which constructor to use.
 *
 * A dependency can have alias (usually an interface that the dependency itself implement).
 * This is useful during test when you want to substitute an existing dependency with a mock object.
 *
 * Singleton and instance dependencies that implement [AutoCloseable] can be closed in registration order using [closeDependencies].
 *
 * Calling [startRunnables] or [get] will lock this container, preventing new dependency to be registered.
 *
 * Circular dependency will cause [StackOverflowError].
 *
 * ## Thread safety
 * The only thread-safe method of this class is [get]
 *
 */
class DiContainer : AutoCloseable {

    private val dependencies = LinkedHashMap<KClass<*>, Dependency<Any>>()
    private val runnableDependencies = ArrayList<Dependency<out Runnable>>()

    @Volatile
    private var locked: Boolean = false

    /**
     * @throws OverrideNotSpecifiedException if dependency is already registered and [overrideIfExists] is false
     * @throws NoValidConstructorFound
     * @throws ContainerLockedException
     */
    fun <T : Any> registerSingleton(
        singleton: Class<out T>,
        alias: Class<out T> = singleton,
        overrideIfExists: Boolean= false,
    ) {
        if (locked) throw ContainerLockedException()

        val aliasKClass = alias.kotlin
        if (dependencies.containsKey(aliasKClass) && !overrideIfExists) {
            throw OverrideNotSpecifiedException(aliasKClass)
        }
        dependencies[aliasKClass] = SingletonDependency(findConstructor(singleton.kotlin), dependencies)
    }

    /**
     *
     * @throws OverrideNotSpecifiedException if dependency is already registered and [overrideIfExists] is false
     * @throws NoValidConstructorFound
     * @throws ContainerLockedException
     */
    @JvmOverloads
    fun <T : Any> registerInstance(
        singletonInstance: T,
        alias: Class<out T> = singletonInstance::class.java,
        overrideIfExists: Boolean= false,
    ) {
        if (locked) throw ContainerLockedException()

        val aliasKClass = alias.kotlin
        if (dependencies.containsKey(aliasKClass) && !overrideIfExists) {
            throw OverrideNotSpecifiedException(aliasKClass)
        }
        dependencies[aliasKClass] = InstanceDependency(singletonInstance)
    }

    /**
     * Register a scoped dependency.
     * @throws OverrideNotSpecifiedException if dependency is already registered and [overrideIfExists] is false
     * @throws NoValidConstructorFound
     * @throws ContainerLockedException
     */
    @JvmOverloads
    fun <T : Any> registerScoped(
        scoped: Class<out T>,
        alias: Class<out T> = scoped,
        overrideIfExists: Boolean= false,
    ) {
        if (locked) throw ContainerLockedException()
        val aliasKClass = alias.kotlin
        if (dependencies.containsKey(aliasKClass) && !overrideIfExists) {
            throw OverrideNotSpecifiedException(aliasKClass)
        }
        dependencies[aliasKClass] = ClassDependency(findConstructor(scoped.kotlin), dependencies)
    }

    /**
     * @throws NoValidConstructorFound
     * @throws ContainerLockedException
     */
    fun registerRunnable(
        runnableClass: Class<out Runnable>,
    ) {
        if (locked) throw ContainerLockedException()
        runnableDependencies.add(SingletonDependency(findConstructor(runnableClass.kotlin), dependencies))
    }

    /**
     * @throws ContainerLockedException
     */
    fun registerRunnableInstance(runnable: Runnable) {
        if (locked) throw ContainerLockedException()
        runnableDependencies.add(InstanceDependency(runnable))
    }

    private fun <T:Any> findConstructor(kClass: KClass<T>): KFunction<T> {
        val constructors = kClass.constructors.filter { it.visibility == KVisibility.PUBLIC }
        if (constructors.isEmpty()) {
            throw NoValidConstructorFound(kClass)
        }
        return if (constructors.size == 1) {
            constructors.first()
        } else {
            constructors.firstOrNull { it.hasAnnotation<DiConstructor>() } ?: throw NoValidConstructorFound(kClass)
        }
    }

    /**
     * Start registered [Runnable] classes and instances in registration order.
     *
     * @throws DependencyNotFoundException
     */
    fun startRunnables() {
        if (!locked) locked = true
        runnableDependencies.forEach { runnableDependency ->
            runnableDependency.createInstance().run()
        }
    }

    /**
     * Get an instance of a registered dependency in this container.
     * @throws DependencyNotFoundException
     */
    operator fun <T:Any> get(clazz: Class<T>): T {
        if (!locked) locked = true
        val dependency = dependencies[clazz.kotlin] ?: throw DependencyNotFoundException(clazz.kotlin)
        return dependency.createInstance() as T
    }

    /**
     * Close all registered singleton and instance that implement [AutoCloseable]
     */
    fun closeDependencies() {
        if (!locked) locked = true
        dependencies.values.forEach {dependency ->
            if (dependency is AutoCloseable) {
                dependency.close()
            }
        }
    }

    /**
     * Call [closeDependencies]
     */
    override fun close() {
        closeDependencies()
    }
}
