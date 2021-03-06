/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.diff.util

import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.diff.Diff
import junit.framework.TestCase
import java.util.*

class DiffPerformanceTest : TestCase() {
  companion object {
    private var needWarmUp = true
  }

  private val arr_20000 = generateData(20000).toTypedArray()
  private val arr_2000 = arr_20000.take(2000).toTypedArray()
  private val arr_1000 = arr_20000.take(1000).toTypedArray()
  private val arr_100 = arr_20000.take(100).toTypedArray()

  private val shuffled_2000 = shuffle(arr_2000)
  private val shuffled_1000 = shuffled_2000.take(1000).toTypedArray()
  private val shuffled_100 = shuffled_2000.take(100).toTypedArray()

  private val altered_20000 = alter(arr_20000)
  private val altered_2000 = alter(arr_2000)
  private val altered_1000 = alter(arr_1000)
  private val altered_100 = alter(arr_100)

  private val heavy_altered_20000 = heavy_alter(arr_20000)
  private val heavy_altered_2000 = heavy_alter(arr_2000)
  private val heavy_altered_1000 = heavy_alter(arr_1000)
  private val heavy_altered_100 = heavy_alter(arr_100)

  private val reversed_2000 = arr_2000.reversedArray()
  private val reversed_1000 = arr_1000.reversedArray()
  private val reversed_100 = arr_100.reversedArray()

  override fun setUp() {
    if (needWarmUp) {
      needWarmUp = false
      warmUp()
    }
    super.setUp()
  }

  private fun warmUp() {
    for (i in 0..20) {
      Diff.buildChanges(arr_2000, shuffled_2000)
    }
  }

  fun `test altered 20000`() {
    testCpu(20, 400) {
      Diff.buildChanges(arr_20000, altered_20000)
    }
  }

  fun `test heavy altered 20000`() {
    testCpu(10, 550) {
      Diff.buildChanges(arr_20000, heavy_altered_20000)
    }
  }

  fun `test altered 2000`() {
    testCpu(400, 450) {
      Diff.buildChanges(arr_2000, altered_2000)
    }
  }

  fun `test heavy altered 2000`() {
    testCpu(400, 600) {
      Diff.buildChanges(arr_2000, heavy_altered_2000)
    }
  }

  fun `test shuffled 2000`() {
    testCpu(1, 700) {
      Diff.buildChanges(arr_2000, shuffled_2000)
    }
  }

  fun `test reversed 2000`() {
    testCpu(1, 700) {
      Diff.buildChanges(arr_2000, reversed_2000)
    }
  }

  fun `test altered 1000`() {
    testCpu(700, 400) {
      Diff.buildChanges(arr_1000, altered_1000)
    }
  }

  fun `test heavy altered 1000`() {
    testCpu(700, 500) {
      Diff.buildChanges(arr_1000, heavy_altered_1000)
    }
  }

  fun `test shuffled 1000`() {
    testCpu(5, 500) {
      Diff.buildChanges(arr_1000, shuffled_1000)
    }
  }

  fun `test reversed 1000`() {
    testCpu(5, 550) {
      Diff.buildChanges(arr_1000, reversed_1000)
    }
  }

  fun `test altered 100`() {
    testCpu(10000, 600) {
      Diff.buildChanges(arr_100, altered_100)
    }
  }

  fun `test heavy altered 100`() {
    testCpu(10000, 600) {
      Diff.buildChanges(arr_100, heavy_altered_100)
    }
  }

  fun `test shuffled 100`() {
    testCpu(2000, 500) {
      Diff.buildChanges(arr_100, shuffled_100)
    }
  }

  fun `test reversed 100`() {
    testCpu(500, 450) {
      Diff.buildChanges(arr_100, reversed_100)
    }
  }


  private fun generateData(size: Int): List<String> {
    return (1..size).map { "${it % 200}" }
  }

  private fun alter(arr: Array<String>): Array<String> {
    val altered = arr.copyOf()
    altered[0] = "===" // avoid "common prefix/suffix" optimisation
    altered[altered.size / 2] = "???"
    altered[altered.lastIndex] = "==="
    return altered
  }

  private fun heavy_alter(arr: Array<String>): Array<String> {
    val altered = arr.copyOf()
    for (i in 1..altered.lastIndex step 20) {
      altered[i] = "${i % 200}"
    }
    altered[0] = "===" // avoid "common prefix/suffix" optimisation
    altered[altered.lastIndex] = "==="
    return altered
  }

  private fun shuffle(arr: Array<String>): Array<String> {
    val list = arr.toMutableList()
    Collections.shuffle(list, Random(0))
    return list.toTypedArray()
  }


  private inline fun testCpu(iterations: Int, expectedMs: Int, crossinline test: () -> Unit) {
    PlatformTestUtil.startPerformanceTest(PlatformTestUtil.getTestName(name, true), expectedMs) {
      for (i in 0..iterations) {
        test()
      }
    }.cpuBound().assertTiming()
  }
}
