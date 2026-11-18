package com.example

import com.example.domain.MathQuestionGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathQuestionGeneratorTest {

    private val generator = MathQuestionGenerator()

    @Test
    fun sameLevelAndSeed_producesIdenticalQuestion() {
        for (level in 1..3) {
            val first = generator.generate(level = level, seed = 42_123L)
            val second = generator.generate(level = level, seed = 42_123L)
            assertEquals(first, second)
        }
    }

    @Test
    fun generatedQuestions_alwaysHaveFourUniqueOptionsAndValidCorrectIndex() {
        for (level in 1..3) {
            for (seed in 1L..30L) {
                val q = generator.generate(level = level, seed = seed)
                assertEquals("math", q.category)
                assertEquals(level, q.level)
                assertEquals(4, q.options.size)
                assertEquals(4, q.options.distinct().size)
                assertTrue(q.correctIndex in 0..3)
            }
        }
    }
}
