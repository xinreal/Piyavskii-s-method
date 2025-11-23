import kotlin.math.*
import kotlin.system.measureTimeMillis
import kotlin.random.Random

// Data classes for results
data class OptimizationResult(
    val x: Double,
    val y: Double,
    val iterations: Int,
    val time: Long,
    val evaluations: List<Pair<Double, Double>>,
    val lipshitzConstant: Double
)

class GlobalOptimization {

    // функции для тестирования
    fun rastrigin(x: Double): Double {
        return x * x - 10 * cos(2 * PI * x) + 10
    }

    fun easom(x: Double): Double {
        return -cos(x) * exp(-(x - PI) * (x - PI))
    }

    fun ackley(x: Double): Double {
        return -20 * exp(-0.2 * sqrt(0.5 * x * x)) - exp(0.5 * cos(2 * PI * x)) + 20 + E
    }

    fun customFunction(x: Double): Double {
        return x + sin(3.14159 * x)
    }

    // This algorithm finds the global minimum of a function on [a, b] using
// an estimate of the Lipschitz constant
    fun adaptivePiyavsky(
        func: (Double) -> Double,
        a: Double,
        b: Double,
        eps: Double,    // precision tolerance
        maxIterations: Int = 1000
    ): OptimizationResult {
        // Store all function evaluations (x, f(x))
        val evaluations = mutableListOf<Pair<Double, Double>>()

        val time = measureTimeMillis {
            // STEP 1: Estimate Lipschitz constant
            val L = estimateLipschitzConstant(func, a, b, 50)

            // STEP 2: Initialize with boundary points
            val points = mutableListOf(a, b)
            val values = mutableListOf(func(a), func(b))
            evaluations.addAll(listOf(Pair(a, values[0]), Pair(b, values[1])))

            var iterations = 0

            // MAIN OPTIMIZATION LOOP
            while (iterations < maxIterations) {
                iterations++

                // Find interval with maximum potential for improvement
                var bestIndex = -1
                var bestPotential = Double.POSITIVE_INFINITY

                // Evaluate all current intervals
                for (i in 0 until points.size - 1) {
                    val x1 = points[i]      // Left point of interval
                    val x2 = points[i + 1]  // Right point of interval
                    val y1 = values[i]      // Function value at x1
                    val y2 = values[i + 1]  // Function value at x2

                    // Calculate potential (lower bound estimate) using Piyavsky's sawtooth
                    val midpoint = (x1 + x2) / 2
                    val potential = (0.5 * (y1 + y2)) - (0.5 * L * (x2 - x1))

                    // Select interval with best (lowest) potential
                    if (potential < bestPotential) {
                        bestPotential = potential
                        bestIndex = i
                    }
                }

                // Safety check - should not happen in normal execution
                if (bestIndex == -1) break

                // Evaluate function at midpoint of selected interval
                val x1 = points[bestIndex]
                val x2 = points[bestIndex + 1]
                val xNew = (x1 + x2) / 2.0
                val yNew = func(xNew)

                // Store evaluation result
                evaluations.add(Pair(xNew, yNew))

                // Insert new point maintaining sorted order
                val insertIndex = points.indexOfFirst { it > xNew }
                if (insertIndex == -1) {
                    // New point is the rightmost
                    points.add(xNew)
                    values.add(yNew)
                } else {
                    // Insert at correct position to keep points sorted
                    points.add(insertIndex, xNew)
                    values.add(insertIndex, yNew)
                }

                // Check stopping condition based on interval sizes
                var maxGap = 0.0
                for (i in 0 until points.size - 1) {
                    maxGap = max(maxGap, points[i + 1] - points[i])
                }

                // Stop if maximum gap between points is below tolerance
                if (maxGap < eps) break
            }
        }

        // Find global minimum
        val minIndex = evaluations.indices.minByOrNull { evaluations[it].second }!!
        val (minX, minY) = evaluations[minIndex]

        return OptimizationResult(minX, minY, evaluations.size, time, evaluations, 0.0)
    }

    // Estimate Lipschitz constant for the function on interval [a, b]
    private fun estimateLipschitzConstant(func: (Double) -> Double, a: Double, b: Double, samples: Int): Double {
        var maxSlope = 0.0
        val step = (b - a) / (samples - 1)

        for (i in 0 until samples - 1) {
            val x1 = a + i * step
            val x2 = a + (i + 1) * step
            val y1 = func(x1)
            val y2 = func(x2)

            val slope = abs(y2 - y1) / abs(x2 - x1)
            maxSlope = max(maxSlope, slope)
        }

        return maxSlope * 1.2 // Safety margin
    }

    // Simple ASCII visualization
    class AsciiPlotter {
        fun plotFunction(
            func: (Double) -> Double,
            result: OptimizationResult,
            a: Double,
            b: Double,
            title: String,
            width: Int = 80,
            height: Int = 20
        ) {
            println("\n" + "=".repeat(width))
            println(title.center(width))
            println("=".repeat(width))

            // Find range
            var minY = Double.MAX_VALUE
            var maxY = Double.MIN_VALUE

            for (i in 0..100) {
                val x = a + (b - a) * i / 100.0
                val y = func(x)
                minY = min(minY, y)
                maxY = max(maxY, y)
            }

            val yRange = maxY - minY
            val xRange = b - a

            // Create canvas
            val canvas = Array(height) { CharArray(width) { ' ' } }

            // Draw axes
            val zeroY = ((0 - minY) / yRange * (height - 1)).toInt().coerceIn(0, height - 1)
            for (x in 0 until width) {
                canvas[zeroY][x] = '─'
            }

            // Plot function
            for (x in 0 until width) {
                val xVal = a + x * xRange / (width - 1)
                val yVal = func(xVal)
                val yPos = ((maxY - yVal) / yRange * (height - 1)).toInt().coerceIn(0, height - 1)
                canvas[yPos][x] = '•'
            }

            // Plot evaluation points
            for ((xVal, yVal) in result.evaluations) {
                val xPos = ((xVal - a) / xRange * (width - 1)).toInt().coerceIn(0, width - 1)
                val yPos = ((maxY - yVal) / yRange * (height - 1)).toInt().coerceIn(0, height - 1)
                canvas[yPos][xPos] = '×'
            }

            // Plot minimum point
            val minXPos = ((result.x - a) / xRange * (width - 1)).toInt().coerceIn(0, width - 1)
            val minYPos = ((maxY - result.y) / yRange * (height - 1)).toInt().coerceIn(0, height - 1)
            canvas[minYPos][minXPos] = '★'

            // Print canvas
            for (row in canvas) {
                println("│${row.joinToString("")}│")
            }

            // Print x-axis labels
            println("└" + "─".repeat(width) + "┘")
            println("  ${String.format("%.2f", a)}${" ".repeat(width - 8)}${String.format("%.2f", b)}")
        }
    }
}

    fun main() {
        val optimizer = GlobalOptimization()
        val plotter = GlobalOptimization.AsciiPlotter()

        println("\n" + " ТЕСТ 1: Функция Растригина".padEnd(50, '─'))

        val result1 = optimizer.adaptivePiyavsky(
            func = { x -> optimizer.rastrigin(x) },
            a = -2.0,
            b = 2.0,
            eps = 0.001
        )

        plotter.plotFunction(
            { x -> optimizer.rastrigin(x) },
            result1,
            -2.0, 2.0,
            "Функция Растригина: x² - 10*cos(2πx) + 10"
        )

        println("\n📊 РЕЗУЛЬТАТЫ:")
        println("   Минимум: f(${String.format("%.6f", result1.x)}) = ${String.format("%.6f", result1.y)}")
        println("   Итерации: ${result1.iterations}")
        println("   Время: ${result1.time} мс")
        println("   Точек вычислений: ${result1.evaluations.size}")

        println("\n" + " ТЕСТ 2: Функция Экли".padEnd(50, '─'))

        val result2 = optimizer.adaptivePiyavsky(
            func = { x -> optimizer.easom(x) },
            a = -10.0,
            b = 10.0,
            eps = 0.001
        )

        plotter.plotFunction(
            { x -> optimizer.easom(x) },
            result2,
            -10.0, 10.0,
            "Функция Экли: -cos(x)*exp(-(x-π)²)"
        )

        println("\n РЕЗУЛЬТАТЫ:")
        println("   Минимум: f(${String.format("%.6f", result2.x)}) = ${String.format("%.6f", result2.y)}")
        println("   Итерации: ${result2.iterations}")
        println("   Время: ${result2.time} мс")

        // Test 3: случайная пользовательская функция
        println("\n" + "🔍 ТЕСТ 3: Пользовательская функция".padEnd(50, '─'))

        val result3 = optimizer.adaptivePiyavsky(
            func = { x -> optimizer.customFunction(x) },
            a = 0.0,
            b = 4.0,
            eps = 0.001
        )

        plotter.plotFunction(
            { x -> optimizer.customFunction(x) },
            result3,
            0.0, 4.0,
            "Пользовательская: x + sin(3.14159*x)"
        )

        println("\n РЕЗУЛЬТАТЫ:")
        println("   Минимум: f(${String.format("%.6f", result3.x)}) = ${String.format("%.6f", result3.y)}")
        println("   Итерации: ${result3.iterations}")
        println("   Время: ${result3.time} мс")

        println("\n" + " ТЕСТ 4: Функция Экли (модифицированная)".padEnd(50, '─'))

        val result4 = optimizer.adaptivePiyavsky(
            func = { x -> optimizer.ackley(x) },
            a = -5.0,
            b = 5.0,
            eps = 0.001
        )

        plotter.plotFunction(
            { x -> optimizer.ackley(x) },
            result4,
            -5.0, 5.0,
            "Функция Экли: -20*exp(-0.2√(0.5x²)) - exp(0.5*cos(2πx)) + e + 20"
        )

        println("\nРЕЗУЛЬТАТЫ:")
        println("   Минимум: f(${String.format("%.6f", result4.x)}) = ${String.format("%.6f", result4.y)}")
        println("   Итерации: ${result4.iterations}")
        println("   Время: ${result4.time} мс")

    }

    // Function centering
    fun String.center(width: Int): String {
        if (this.length >= width) return this
        val padding = width - this.length
        val leftPadding = padding / 2
        val rightPadding = padding - leftPadding
        return " ".repeat(leftPadding) + this + " ".repeat(rightPadding)
    }
