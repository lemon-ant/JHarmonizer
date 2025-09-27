package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;

@Value
@Builder(access = PACKAGE)
@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
@AllArgsConstructor(access = PRIVATE)
public class SpoonAstModel {

    @Nullable
    CtType<?> mainType;

    /**
     * Do not change this model under any circumstances!!!
     * TODO: For what do we need it???
     */
    /*@NonNull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    CtCompilationUnit originalCompilationUnit;*/

    @NonNull
    Supplier<String> serializedSourceCode;

    /**
     * This is a working model copy for actual resorting changes
     */
    @NonNull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    CtCompilationUnit workingCompilationUnit;

    public Optional<CtType<?>> getMainType() {
        return Optional.ofNullable(mainType);
    }
}
