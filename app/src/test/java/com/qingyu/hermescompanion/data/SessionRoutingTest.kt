package com.qingyu.hermescompanion.data

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class SessionRoutingTest {
    @Test fun runtimeIdsRouteToTheirExactOwner() {
        val a = Any(); val b = Any()
        val streams = mapOf("a" to a, "b" to b)
        assertSame(a, routeSessionEvent(streams, "a"))
        assertSame(b, routeSessionEvent(streams, "b"))
        assertNull(routeSessionEvent(streams, "unknown"))
    }
    @Test fun unlabelledEventsAreNotGuessedAcrossParallelRuns() {
        assertNull(routeSessionEvent(mapOf("a" to 1, "b" to 2), null))
        assertEquals(1, routeSessionEvent(mapOf("a" to 1), null))
        assertNull(routeSessionEvent(emptyMap<String, Int>(), null))
    }
    @Test fun previousTurnCleanupCannotRemoveNewRegistration() {
        val old = Any(); val fresh = Any()
        val streams = ConcurrentHashMap<String, Any>()
        streams["a"] = old
        streams.remove("a", old)
        streams["a"] = fresh
        assertFalse(streams.remove("a", old))
        assertSame(fresh, routeSessionEvent(streams, "a"))
    }
    @Test fun rootAndUnicodeDirectoryPathsArePreserved() {
        assertEquals("/", normalizeWorkspacePath(" / "))
        assertEquals("/projects/我的项目", normalizeWorkspacePath(" /projects/我的项目/ "))
    }
}
