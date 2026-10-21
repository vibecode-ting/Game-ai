package com.example.data

import com.example.data.model.ActionType
import com.example.data.model.DemonstrationItem
import com.example.data.model.GameAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class DemonstrationRepository {

    private val _demonstrations = MutableStateFlow<List<DemonstrationItem>>(emptyList())
    val demonstrations: StateFlow<List<DemonstrationItem>> = _demonstrations.asStateFlow()

    init {
        loadPreseededDemonstrations()
    }

    private fun loadPreseededDemonstrations() {
        val seeded = listOf(
            DemonstrationItem(
                id = UUID.randomUUID().toString(),
                title = "Match 3 Gem Swap (Horizontal)",
                gameName = "Gem Matcher",
                action = GameAction(
                    action = ActionType.SWIPE,
                    startX = 420,
                    startY = 650,
                    endX = 580,
                    endY = 650,
                    durationMs = 180L,
                    reasoning = "Swapping blue gem right to form horizontal match-of-3",
                    targetDescription = "Blue Sapphire Gem"
                ),
                description = "Swipe the middle gem horizontally to connect 3 identical colors."
            ),
            DemonstrationItem(
                id = UUID.randomUUID().toString(),
                title = "Obstacle Jump Tap",
                gameName = "Neon Runner",
                action = GameAction(
                    action = ActionType.TAP,
                    x = 500,
                    y = 850,
                    durationMs = 60L,
                    reasoning = "Tap lower screen to trigger jump before spiked barrier at x=600",
                    targetDescription = "Jump Zone"
                ),
                description = "Tap firmly when the runner is within 150px of an oncoming barrier."
            ),
            DemonstrationItem(
                id = UUID.randomUUID().toString(),
                title = "Close Promotional Ad Dialog",
                gameName = "General / System Edge Case",
                action = GameAction(
                    action = ActionType.TAP,
                    x = 920,
                    y = 110,
                    durationMs = 50L,
                    reasoning = "Detected interstitial ad popup with 'X' in top-right corner; tap to dismiss",
                    targetDescription = "Ad Dismiss 'X' Button",
                    isAdOrPopup = true
                ),
                description = "Always prioritize tapping the top-right or top-left 'X' button if a popup dialog covers the playfield."
            ),
            DemonstrationItem(
                id = UUID.randomUUID().toString(),
                title = "Powerup Hold & Drag",
                gameName = "Space Defender",
                action = GameAction(
                    action = ActionType.HOLD,
                    x = 500,
                    y = 750,
                    durationMs = 450L,
                    reasoning = "Hold touch to charge plasma cannon before releasing",
                    targetDescription = "Charged Blaster"
                ),
                description = "Hold finger down on the spaceship to charge the secondary beam attack."
            )
        )
        _demonstrations.value = seeded
    }

    fun addDemonstration(
        title: String,
        gameName: String,
        action: GameAction,
        description: String = ""
    ) {
        val item = DemonstrationItem(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Demonstration #${_demonstrations.value.size + 1}" },
            gameName = gameName.ifBlank { "Mobile Game" },
            action = action,
            description = description
        )
        _demonstrations.value = listOf(item) + _demonstrations.value
    }

    fun removeDemonstration(id: String) {
        _demonstrations.value = _demonstrations.value.filterNot { it.id == id }
    }

    fun clearAll() {
        _demonstrations.value = emptyList()
    }

    fun restoreDefaults() {
        loadPreseededDemonstrations()
    }
}
