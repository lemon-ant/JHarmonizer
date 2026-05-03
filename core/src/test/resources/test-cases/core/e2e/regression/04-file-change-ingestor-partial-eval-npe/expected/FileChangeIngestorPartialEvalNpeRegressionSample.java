/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.nifi.minifi.bootstrap.configuration.ingestors;

import static org.apache.nifi.minifi.bootstrap.configuration.ConfigurationChangeCoordinator.NOTIFIER_INGESTORS_KEY;
import static org.apache.nifi.minifi.bootstrap.configuration.differentiators.WholeConfigDifferentiator.WHOLE_CONFIG_KEY;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.nifi.minifi.bootstrap.configuration.differentiators.Differentiator;
import org.apache.nifi.minifi.bootstrap.configuration.differentiators.WholeConfigDifferentiator;

public class FileChangeIngestorPartialEvalNpeRegressionSample {
    static final String CONFIG_FILE_BASE_KEY = NOTIFIER_INGESTORS_KEY + ".file";
    static final String DIFFERENTIATOR_KEY = CONFIG_FILE_BASE_KEY + ".differentiator";
    private static final Map<String, Supplier<Differentiator<ByteBuffer>>> DIFFERENTIATOR_CONSTRUCTOR_MAP =
            Map.of(WHOLE_CONFIG_KEY, WholeConfigDifferentiator::getByteBufferDifferentiator);

    private final String selectedDifferentiator = DIFFERENTIATOR_KEY;
}
