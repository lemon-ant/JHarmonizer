// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.printerlab;

import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

/** Serializes a fixed batch; JMH requires public benchmark and state classes. */
public class PrinterBenchmark {

    /**
     * Measures fresh printer construction and serialization of already prepared models.
     * @param state models private to this worker
     * @param blackhole consumes every result, including its skipped ranges
     */
    @Benchmark
    public void serialize(@NonNull ThreadState state, @NonNull Blackhole blackhole) {
        for (PrinterWorkloads.PreparedInput input : state.inputs) {
            blackhole.consume(input.getModel().getSerializedSrcCode().get());
        }
    }

    /** Keeps parsing, sorting, verification and mutable Spoon state outside the timed operation. */
    @State(Scope.Thread)
    @SuppressWarnings("NotNullFieldNotInitialized")
    public static class ThreadState {
        @Param({"fixtures", "flat128", "flat1024", "nested128"})
        @NonNull
        public String workload;

        @NonNull
        private List<PrinterWorkloads.PreparedInput> inputs;

        /** Builds independent models for this worker before warmup. */
        @Setup(Level.Trial)
        public void prepare() throws Exception {
            inputs = PrinterWorkloads.prepare(Path.of(System.getProperty("printer.lab.corpus")), workload);
            PrinterWorkloads.verify(inputs);
        }

        /** Verifies that repeated serialization did not change output or source-model structure. */
        @TearDown(Level.Trial)
        public void verify() {
            PrinterWorkloads.verify(inputs);
        }
    }
}
