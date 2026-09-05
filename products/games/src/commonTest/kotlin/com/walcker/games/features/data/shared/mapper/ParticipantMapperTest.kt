package com.walcker.games.features.data.shared.mapper

import com.walcker.match.firestore.DocumentSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun snapshot(
    id: String = "user-1",
    data: Map<String, Any?>,
): DocumentSnapshot =
    DocumentSnapshot(
        path = "matches/match-1/participants/$id",
        id = id,
        data = data,
        exists = true,
    )

class ParticipantMapperTest {
    @Test
    fun `maps every field from a complete document`() {
        val participant =
            snapshot(
                data =
                    mapOf(
                        "userId" to "user-1",
                        "displayName" to "Ana Souza",
                        "photoUrl" to "https://example.com/ana.jpg",
                        "joinedAt" to 1_700_000_000L,
                        "isConfirmed" to true,
                        "positionInWaitlist" to 2L,
                        "hasPaid" to true,
                    ),
            ).toParticipant()!!

        assertEquals("user-1", participant.userId)
        assertEquals("Ana Souza", participant.displayName)
        assertEquals("https://example.com/ana.jpg", participant.photoUrl)
        assertEquals(1_700_000_000L, participant.joinedAt)
        assertEquals(true, participant.isConfirmed)
        assertEquals(2, participant.positionInWaitlist)
        assertEquals(true, participant.hasPaid)
    }

    @Test
    fun `falls back to the document id when userId is missing`() {
        val participant = snapshot(id = "user-7", data = mapOf("displayName" to "Bruno")).toParticipant()!!

        assertEquals("user-7", participant.userId)
    }

    @Test
    fun `missing optional fields fall back to their defaults`() {
        val participant = snapshot(data = emptyMap()).toParticipant()!!

        assertEquals("Jogador", participant.displayName)
        assertEquals(0L, participant.joinedAt)
        assertEquals(true, participant.isConfirmed)
        assertNull(participant.photoUrl)
        assertNull(participant.positionInWaitlist)
        assertEquals(false, participant.hasPaid)
    }
}
