package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOuts;
import io.github.lemon_ant.jharmonizer.core.translator.SerializedSrcWithSkippedTypeRanges;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

@Value
// TODO Remove builder
@Builder(access = PACKAGE)
@AllArgsConstructor(access = PRIVATE)
public class SpoonAstModel {

    /**
     * This is a working model copy for actual resorting changes
     */
    @NonNull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    CtCompilationUnit compilationUnit;

    @Nullable
    CtType<?> mainType;

    @NonNull
    List<CtTypeMember> originalMemberOrder;

    @NonNull
    Path path;

    @NonNull
    Supplier<SerializedSrcWithSkippedTypeRanges> serializedSrcCode;

    @NonNull
    JHarmonizerOptOuts optOuts;

    /**
     * Returns the main type.
     * @return the main type
     */
    @NonNull
    public Optional<CtType<?>> getMainType() {
        return Optional.ofNullable(mainType);
    }
}
