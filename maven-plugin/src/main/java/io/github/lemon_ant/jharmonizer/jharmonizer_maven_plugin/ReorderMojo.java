package io.github.lemon_ant.jharmonizer.jharmonizer_maven_plugin;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import lombok.NonNull;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Rewrites Java source files in-place so that their members conform to the configured ordering.
 * Creates {@code .bak} backup files alongside modified sources when backups are enabled in the
 * active configuration.
 */
@Mojo(name = "reorder", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true)
public final class ReorderMojo extends AbstractJHarmonizerMojo {

    /**
     * Creates a new ReorderMojo.
     */
    public ReorderMojo() {}

    @Override
    @NonNull
    protected FlowType getFlowType() {
        return FlowType.REORDER;
    }
}
