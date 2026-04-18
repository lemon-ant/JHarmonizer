package io.github.lemon_ant.jharmonizer.jharmonizer_maven_plugin;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import lombok.NonNull;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Checks all Java source files under the configured base directory and reports every file that
 * does not conform to the configured member ordering. All violations are collected before the
 * build is failed (when {@code failOnViolation=true}).
 *
 * <p>Source files are <strong>never modified</strong> by this goal.
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public final class CheckMojo extends AbstractJHarmonizerMojo {

    /**
     * Creates a new CheckMojo.
     */
    public CheckMojo() {}

    @Override
    @NonNull
    protected FlowType getFlowType() {
        return FlowType.CHECK_ALL;
    }
}
