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

internal sealed interface Dependency<T:Any> {
    fun createInstance(): T
}

internal class InstanceDependency<T:Any>(
    private val instance: T,
) : Dependency<T>, AutoCloseable {
    override fun createInstance(): T = instance
    override fun close() {
        if (instance is AutoCloseable) instance.close()
    }
}

internal open class ClassDependency<T:Any>(
    protected val constructor: KFunction<T>,
    protected val dependenciesTree: Map<KClass<*>, Dependency<Any>>,
) : Dependency<T> {
    override fun createInstance(): T {
        val arguments = arrayOfNulls<Any?>(constructor.parameters.size)

        constructor.parameters.forEachIndexed { index, kParameter ->
            val parameterKClass = kParameter.type.classifier as KClass<*>
            val dependency = dependenciesTree[parameterKClass] ?: throw DependencyNotFoundException(parameterKClass)
            arguments[index] = dependency.createInstance()
        }
        return constructor.call(*arguments)
    }
}

internal class SingletonDependency<T:Any>(
    constructor: KFunction<T>,
    dependenciesTree: Map<KClass<*>, Dependency<Any>>
) : ClassDependency<T>(constructor, dependenciesTree), AutoCloseable {

    @Volatile
    private lateinit var instance: T

    override fun createInstance(): T {
        if (this::instance.isInitialized) {
            return instance
        }
        return synchronized(this) {
            instance = super.createInstance()
            instance
        }
    }

    override fun close() {
        if (this::instance.isInitialized) {
            val i = instance
            if (i is AutoCloseable) {
                i.close()
            }
        }
    }
}

/**
 * Mark a constructor to be used for dependency injection.
 */
@[Target(AnnotationTarget.CONSTRUCTOR)
Retention(AnnotationRetention.RUNTIME)
MustBeDocumented]
annotation class DiConstructor


