package com.walcker.match.navigator

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeepLinkCoordinatorTest {
    @Test
    fun `a link that arrives before anyone listens is delivered when the app starts listening`() =
        runTest {
            val coordinator = DeepLinkCoordinator()

            coordinator.navigate(DeepLink.OpenMatch("m1"))

            assertEquals(DeepLink.OpenMatch("m1"), coordinator.links.first())
        }

    @Test
    fun `links are delivered in the order they arrived`() =
        runTest {
            val coordinator = DeepLinkCoordinator()

            coordinator.navigate(DeepLink.OpenMatch("m1"))
            coordinator.navigate(DeepLink.OpenMatch("m2"))

            assertEquals(listOf(DeepLink.OpenMatch("m1"), DeepLink.OpenMatch("m2")), coordinator.links.take(2).toList())
        }

    @Test
    fun `a link is consumed once and not replayed to the next listener`() =
        runTest {
            val coordinator = DeepLinkCoordinator()
            coordinator.navigate(DeepLink.OpenMatch("m1"))
            coordinator.links.first()

            coordinator.navigate(DeepLink.OpenMatch("m2"))

            assertEquals(DeepLink.OpenMatch("m2"), coordinator.links.first())
        }
}
