package com.example

import com.example.data.DemonstrationRepository
import com.example.data.model.ActionType
import com.example.data.model.GameAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testGameActionSummary() {
        val tapAction = GameAction(
            action = ActionType.TAP,
            x = 500,
            y = 750,
            reasoning = "Test tap"
        )
        assertTrue(tapAction.summary().contains("TAP at (500, 750)"))

        val swipeAction = GameAction(
            action = ActionType.SWIPE,
            startX = 100,
            startY = 200,
            endX = 300,
            endY = 400,
            durationMs = 250L
        )
        assertTrue(swipeAction.summary().contains("SWIPE from (100, 200) to (300, 400)"))
    }

    @Test
    fun testDemonstrationRepository() {
        val repo = DemonstrationRepository()
        assertTrue(repo.demonstrations.value.isNotEmpty())

        val initialCount = repo.demonstrations.value.size
        repo.addDemonstration(
            title = "UnitTest Demo",
            gameName = "TestGame",
            action = GameAction(action = ActionType.TAP, x = 100, y = 100)
        )
        assertEquals(initialCount + 1, repo.demonstrations.value.size)
    }
}
