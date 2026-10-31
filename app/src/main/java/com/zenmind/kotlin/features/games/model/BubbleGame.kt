package com.zenmind.kotlin.features.games.model

import kotlin.math.min
import kotlin.random.Random

enum class GamePhase { Ready, Running, Paused, Finished }

data class Bubble(val number: Int, val x: Float, val y: Float)

data class GameState(
    val phase: GamePhase = GamePhase.Ready,
    val bubbles: List<Bubble> = emptyList(),
    val diameter: Float = 0f,
    val nextNumber: Int = 1,
    val correctTaps: Int = 0,
    val errors: Int = 0,
    val elapsedMs: Long = 0L,
    val displayMs: Long = 0L
)

/** Local game rules; all coordinates and sizes are in dp. */
class BubbleGame(
    private val clock: () -> Long,
    private val random: Random = Random.Default
) {
    var state = GameState()
        private set

    private var width = 0f
    private var height = 0f
    private var startedAt = 0L
    private var lastTapNumber = 0
    private var lastTapAt = Long.MIN_VALUE

    fun resize(width: Float, height: Float) {
        if (this.width == width && this.height == height) return
        this.width = width
        this.height = height
        if (state.phase == GamePhase.Running || state.phase == GamePhase.Paused) {
            state = state.copy(bubbles = place((state.nextNumber..20).toList()), diameter = diameter())
        }
    }

    fun start() {
        if (state.phase == GamePhase.Running || state.phase == GamePhase.Paused) return
        val bubbles = place((1..20).toList())
        if (bubbles.size != 20) return
        startedAt = clock()
        lastTapNumber = 0
        lastTapAt = Long.MIN_VALUE
        state = GameState(phase = GamePhase.Running, bubbles = bubbles, diameter = diameter())
    }

    fun tap(number: Int) {
        if (state.phase != GamePhase.Running || state.bubbles.none { it.number == number }) return
        val now = clock()
        if (number == lastTapNumber && now - lastTapAt in 0..249) return
        lastTapNumber = number
        lastTapAt = now

        if (number != state.nextNumber) {
            state = state.copy(errors = state.errors + 1,
                bubbles = place(state.bubbles.map { it.number }, state.bubbles))
            return
        }

        val remaining = state.bubbles.filterNot { it.number == number }
        state = state.copy(
            phase = if (number == 20) GamePhase.Finished else GamePhase.Running,
            bubbles = remaining,
            nextNumber = number + 1,
            correctTaps = state.correctTaps + 1,
            elapsedMs = if (number == 20) state.elapsedMs + (now - startedAt).coerceAtLeast(0) else state.elapsedMs,
            displayMs = state.elapsedMs + (now - startedAt).coerceAtLeast(0)
        )
    }

    fun tick() {
        if (state.phase == GamePhase.Running) {
            // Display only; pause and finish calculate against the original start time.
            state = state.copy(displayMs = state.elapsedMs + (clock() - startedAt).coerceAtLeast(0))
        }
    }

    fun pause() {
        if (state.phase != GamePhase.Running) return
        val elapsed = state.elapsedMs + (clock() - startedAt).coerceAtLeast(0)
        state = state.copy(phase = GamePhase.Paused, elapsedMs = elapsed, displayMs = elapsed)
    }

    fun resume() {
        if (state.phase != GamePhase.Paused) return
        startedAt = clock()
        state = state.copy(phase = GamePhase.Running)
    }

    private fun diameter(): Float = min(56f, min(width / 4f - 8f, height / 5f - 8f))

    private fun place(numbers: List<Int>, previous: List<Bubble> = emptyList()): List<Bubble> {
        val size = diameter()
        if (size < 40f) return emptyList()
        val cellWidth = width / 4f
        val cellHeight = height / 5f
        val cells = (0 until 20).shuffled(random).toMutableList()
        if (previous.size == numbers.size && numbers.isNotEmpty() &&
            numbers.indices.all { index ->
                val bubble = previous[index]
                val cell = (bubble.y / cellHeight).toInt() * 4 + (bubble.x / cellWidth).toInt()
                cell == cells[index]
            }) {
            cells.add(cells.removeAt(0))
        }
        // Each bubble stays inside its own cell, but its offset is randomized.
        // This keeps the field less grid-like without relying on collision retries.
        val inset = 4f
        return numbers.mapIndexed { index, number ->
            val cell = cells[index]
            Bubble(number,
                (cell % 4) * cellWidth + inset + random.nextFloat() * (cellWidth - size - 2 * inset),
                (cell / 4) * cellHeight + inset + random.nextFloat() * (cellHeight - size - 2 * inset))
        }
    }
}
