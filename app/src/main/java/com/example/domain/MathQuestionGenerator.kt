package com.example.domain

import com.example.model.Question
import kotlin.math.abs
import kotlin.random.Random

class MathQuestionGenerator {

    fun generate(level: Int, seed: Long): Question {
        val normalizedLevel = level.coerceIn(1, 3)
        val random = Random(seed)

        val (expression, answer, candidateOffsets) = when (normalizedLevel) {
            1 -> generateLevel1(random)
            2 -> generateLevel2(random)
            else -> generateLevel3(random)
        }

        val distractors = buildDistractors(
            answer = answer,
            candidateOffsets = candidateOffsets,
            random = random
        )

        val optionsList = (distractors + answer).shuffled(random)
        val correctIndex = optionsList.indexOf(answer)

        return Question(
            id = "math_${normalizedLevel}_$seed",
            category = "math",
            level = normalizedLevel,
            text = "$expression = ؟",
            options = optionsList.map { it.toString() },
            correctIndex = correctIndex,
            explanation = "$expression = $answer"
        )
    }

    private data class MathSpec(
        val expression: String,
        val answer: Int,
        val candidateOffsets: List<Int>
    )

    private fun generateLevel1(random: Random): MathSpec {
        val isAddition = random.nextBoolean()
        return if (isAddition) {
            val a = random.nextInt(14, 79)
            val b = random.nextInt(12, 69)
            val ans = a + b
            MathSpec(
                expression = "$a + $b",
                answer = ans,
                candidateOffsets = listOf(-10, 10, -2, 2, -1, 1, 12, -8)
            )
        } else {
            val b = random.nextInt(12, 49)
            val diff = random.nextInt(11, 55)
            val a = b + diff
            MathSpec(
                expression = "$a - $b",
                answer = diff,
                candidateOffsets = listOf(-10, 10, -2, 2, -1, 1, 4, -4)
            )
        }
    }

    private fun generateLevel2(random: Random): MathSpec {
        val a = random.nextInt(4, 16)
        val b = random.nextInt(3, 13)
        val ans = a * b
        val nearA = a
        val nearB = b
        return MathSpec(
            expression = "$a × $b",
            answer = ans,
            candidateOffsets = listOf(
                nearA,
                -nearA,
                nearB,
                -nearB,
                10,
                -10,
                2,
                -2
            )
        )
    }

    private fun generateLevel3(random: Random): MathSpec {
        val divisor = random.nextInt(2, 12)
        val quotient = random.nextInt(4, 15)
        val dividend = divisor * quotient
        val extra = random.nextInt(3, 19)
        val addExtra = random.nextBoolean()

        return if (addExtra) {
            val ans = quotient + extra
            MathSpec(
                expression = "($dividend ÷ $divisor) + $extra",
                answer = ans,
                candidateOffsets = listOf(-divisor, divisor, -2, 2, -1, 1, 5, -5)
            )
        } else {
            val safeExtra = extra.coerceAtMost(quotient + 6)
            val finalQuotient = if (quotient > safeExtra) quotient else safeExtra + random.nextInt(2, 9)
            val safeDividend = divisor * finalQuotient
            val ans = finalQuotient - safeExtra
            MathSpec(
                expression = "($safeDividend ÷ $divisor) - $safeExtra",
                answer = ans,
                candidateOffsets = listOf(divisor, -divisor, 2, -2, 1, -1, 4, -4)
            )
        }
    }

    private fun buildDistractors(
        answer: Int,
        candidateOffsets: List<Int>,
        random: Random
    ): List<Int> {
        val chosen = linkedSetOf<Int>()
        for (offset in candidateOffsets.shuffled(random)) {
            if (chosen.size == 3) break
            val candidate = answer + offset
            if (candidate != answer && candidate >= 0) {
                chosen.add(candidate)
            }
        }
        var delta = 1
        while (chosen.size < 3) {
            val up = answer + delta
            if (up != answer && up >= 0) chosen.add(up)
            if (chosen.size < 3) {
                val down = abs(answer - delta) + 1
                if (down != answer) chosen.add(down)
            }
            delta++
        }
        return chosen.take(3)
    }
}
