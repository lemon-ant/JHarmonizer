package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;

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
    Map<SourcePosition, Integer> originalElements2OrderIndices;

    @NonNull
    Path path;

    @NonNull
    Supplier<String> serializedSourceCode;

    /**
     * Returns the main type.
     * @return the main type
     */
    public Optional<CtType<?>> getMainType() {
        return Optional.ofNullable(mainType);
    }
}
