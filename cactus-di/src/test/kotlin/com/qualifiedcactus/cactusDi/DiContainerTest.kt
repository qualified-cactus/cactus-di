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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiContainerTest {

    class DependencyA  {
        var state = 0
    }

    class DependencyB(
        val dependencyA: DependencyA
    )

    class DependencyC {
        var state = 0
    }

    class DependencyD(
        val dependencyB: DependencyB,
        val dependencyC: DependencyC,
    )

    class RunnableA(
        val dependencyD: DependencyD
    ) : Runnable {
        override fun run() {
            dependencyD.dependencyB.dependencyA.state++
            dependencyD.dependencyC.state++
        }
    }

    @Test
    fun test() {
        val diContainer = DiContainer().apply {
            registerSingleton(DependencyA::class.java)
            registerSingleton(DependencyB::class.java)
            registerScoped(DependencyC::class.java)
            registerSingleton(DependencyD::class.java)
            registerRunnable(RunnableA::class.java)
        }
        diContainer.startRunnables()
        assertEquals(1, diContainer[DependencyA::class.java].state)
        assertEquals(0, diContainer[DependencyC::class.java].state)
    }

    @Test
    fun testLock() {
        val diContainer = DiContainer().apply {
            registerSingleton(DependencyA::class.java)
        }
        diContainer[DependencyA::class.java]
        assertThrowsExactly(ContainerLockedException::class.java) {
            diContainer.registerSingleton(DependencyB::class.java)
        }
    }

    class CirclicDependencyA(
        val a: CirclicDependencyB
    )
    class CirclicDependencyB(
        val a: CirclicDependencyA
    )

    fun testStackOverflow() {
        val diContainer = DiContainer().apply {
            registerSingleton(CirclicDependencyA::class.java)
            registerSingleton(CirclicDependencyB::class.java)
        }
        diContainer[CirclicDependencyA::class.java]
    }
}