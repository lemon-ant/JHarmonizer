// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.sorting.benchmark;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Entry point to run all sorting benchmark scenarios programmatically.
 *
 * <p>Includes {@link SimplifiedSortingBenchmark} (original vs. simplified comparison).</p>
 *
 * <p>Usage:
 * <pre>
 *   mvn test-compile exec:java
 * </pre>
 *
 * <p>Results are printed to stdout and also written to {@code benchmark-results.json}.
 */
public class BenchmarkMain {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SimplifiedSortingBenchmark.class.getSimpleName())
                .resultFormat(ResultFormatType.JSON)
                .result("benchmark-results.json")
                .build();
        new Runner(opt).run();
    }
}
