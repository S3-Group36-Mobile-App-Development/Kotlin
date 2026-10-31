package com.zenmind.kotlin

import com.zenmind.kotlin.features.games.model.BubbleGame
import com.zenmind.kotlin.features.games.model.GamePhase
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleGameTest {
    private var now = 1_000L
    private fun game() = BubbleGame(clock = { now }, random = Random(42)).apply {
        resize(320f, 400f)
        start()
    }

    @Test fun bubblesStayInsideBoardAndDoNotOverlap() {
        val game = game()
        assertEquals((1..20).toList(), game.state.bubbles.map { it.number })
        val size = game.state.diameter
        assertTrue(size >= 40f)
        game.state.bubbles.forEach { bubble ->
            assertTrue(bubble.x >= 0f && bubble.x + size <= 320f)
            assertTrue(bubble.y >= 0f && bubble.y + size <= 400f)
        }
        assertTrue(game.state.bubbles.any { bubble ->
            val cellWidth = 320f / 4
            val cellHeight = 400f / 5
            val centeredX = (bubble.x / cellWidth).toInt() * cellWidth + (cellWidth - size) / 2
            val centeredY = (bubble.y / cellHeight).toInt() * cellHeight + (cellHeight - size) / 2
            bubble.x != centeredX && bubble.y != centeredY
        })
        game.state.bubbles.forEachIndexed { i, a ->
            game.state.bubbles.drop(i + 1).forEach { b ->
                assertTrue(a.x + size <= b.x || b.x + size <= a.x ||
                    a.y + size <= b.y || b.y + size <= a.y)
            }
        }
    }

    @Test fun incorrectTapRearrangesBubblesAndRepeatedTapIsIgnored() {
        val game = game()
        val positions = game.state.bubbles

        game.tap(7)
        assertEquals(1, game.state.errors)
        assertEquals(1, game.state.nextNumber)
        assertEquals(20, game.state.bubbles.size)
        assertFalse(positions == game.state.bubbles)
        val size = game.state.diameter
        game.state.bubbles.forEachIndexed { i, a ->
            assertTrue(a.x >= 0f && a.x + size <= 320f && a.y >= 0f && a.y + size <= 400f)
            game.state.bubbles.drop(i + 1).forEach { b ->
                assertTrue(a.x + size <= b.x || b.x + size <= a.x ||
                    a.y + size <= b.y || b.y + size <= a.y)
            }
        }

        game.tap(7) // Accidental double tap within 250 ms.
        assertEquals(1, game.state.errors)
        now += 300
        game.tap(7)
        assertEquals(2, game.state.errors)
    }

    @Test fun smallerFieldStillKeepsTwentyBubblesWithinBounds() {
        val game = BubbleGame(clock = { now }, random = Random(7))
        game.resize(200f, 320f)
        game.start()

        assertEquals(20, game.state.bubbles.size)
        val size = game.state.diameter
        game.state.bubbles.forEachIndexed { i, a ->
            assertTrue(a.x >= 0f && a.x + size <= 200f && a.y >= 0f && a.y + size <= 320f)
            game.state.bubbles.drop(i + 1).forEach { b ->
                assertTrue(a.x + size <= b.x || b.x + size <= a.x ||
                    a.y + size <= b.y || b.y + size <= a.y)
            }
        }
    }

    @Test fun correctTapsFinishOnceAndFreezeDuration() {
        val game = game()
        for (number in 1..20) {
            now += 100
            game.tap(number)
            game.tap(number) // A bubble that was removed cannot be counted twice.
        }
        assertEquals(GamePhase.Finished, game.state.phase)
        assertEquals(20, game.state.correctTaps)
        assertEquals(0, game.state.errors)
        assertEquals(2_000L, game.state.elapsedMs)
        now += 10_000
        game.tick()
        game.tap(19)
        assertEquals(2_000L, game.state.elapsedMs)
        assertEquals(20, game.state.correctTaps)
    }

    @Test fun pauseResumeRotationAndNewGameResetCountersAndTime() {
        val game = game()
        now += 500
        game.pause()
        assertEquals(GamePhase.Paused, game.state.phase)
        game.tap(1)
        now += 10_000
        game.resize(280f, 340f)
        assertEquals(20, game.state.bubbles.size)
        game.resume()
        now += 500
        game.tap(1)
        assertEquals(1_000L, game.state.displayMs)
        assertEquals(1, game.state.correctTaps)
        game.start() // An accidental second start while running must not erase progress.
        assertEquals(1, game.state.correctTaps)
        game.pause()
        game.start()
        assertEquals(GamePhase.Paused, game.state.phase)
        game.resume()
        for (number in 2..20) {
            now += 300
            game.tap(number)
        }
        assertEquals(GamePhase.Finished, game.state.phase)
        game.start()
        assertEquals(GamePhase.Running, game.state.phase)
        assertEquals(0, game.state.correctTaps)
        assertEquals(0L, game.state.elapsedMs)
        assertEquals(0, game.state.errors)
        assertEquals(20, game.state.bubbles.size)
    }
}
