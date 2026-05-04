// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.jharmonizer_maven_plugin;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import lombok.NonNull;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Checks Java source files under the configured base directory and stops at the first file that
 * does not conform to the configured member ordering. Faster than {@code check} when a single
 * violation is sufficient to fail the build.
 *
 * <p>Source files are <strong>never modified</strong> by this goal.
 */
@Mojo(name = "check-fast", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public final class CheckFastMojo extends AbstractJHarmonizerMojo {

    /**
     * Creates a new CheckFastMojo.
     */
    public CheckFastMojo() {}

    @Override
    @NonNull
    protected FlowType getFlowType() {
        return FlowType.CHECK_FAIL_FAST;
    }
}
