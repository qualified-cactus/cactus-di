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

package com.qualifiedcactus.cactusDi

import kotlin.reflect.KClass

open class CactusDiException : RuntimeException {

    internal constructor() : super()
    internal constructor(message: String?) : super(message)
    internal constructor(message: String?, cause: Throwable?) : super(message, cause)
    internal constructor(cause: Throwable?) : super(cause)
    internal constructor(message: String?, cause: Throwable?, enableSuppression: Boolean, writableStackTrace: Boolean) : super(
        message,
        cause,
        enableSuppression,
        writableStackTrace
    )
}

class DependencyNotFoundException
internal constructor(
    missingDependency: KClass<*>,
) : CactusDiException("Dependency ${missingDependency.qualifiedName} is not registered")


class OverrideNotSpecifiedException
internal constructor(
    dependencyToOverride: KClass<*>,
) : CactusDiException("${dependencyToOverride.qualifiedName} already exists in the dependencies. " +
    "Please explicitly specify if you want to override it using method argument")

class NoValidConstructorFound
internal constructor(
    kClass: KClass<*>,
) : CactusDiException(
    "${kClass.qualifiedName} must have at least 1 public constructor. If there are more than 1 public constructor, " +
    "${DiConstructor::class.qualifiedName} must be used to annotate the constructor to be used")

class ContainerLockedException
internal constructor() : CactusDiException("Can't register dependency because container is locked")