package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutResolver;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOuts;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils;
import io.github.lemon_ant.jharmonizer.core.translator.SerializedSrcWithSkippedTypeRanges;
import io.github.lemon_ant.jharmonizer.core.translator.SpoonModelBuildException;
import java.util.Map;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.Launcher;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.support.compiler.VirtualFile;

@UtilityClass
public class SpoonParser {

    private static final int JAVA_VERSION = 21;

    /**
     * Parses the java source resource.
     * @param srcFile the original source file
     * @param printerConfig the printer configuration for the serialization supplier
     * @return the java source resource
     */
    @NonNull
    public static SpoonAstModel parseJavaSrcFile(@NonNull SrcFile srcFile, @NonNull PrinterConfig printerConfig) {
        VirtualFile virtualFile =
                new VirtualFile(srcFile.getSrcCode(), srcFile.getPath().toString());

        Launcher launcher = createPreconfiguredParserLauncher();
        launcher.addInputResource(virtualFile);

        return buildSpoonAstModel(srcFile, launcher, printerConfig);
    }

    @NonNull
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private static SpoonAstModel buildSpoonAstModel(SrcFile srcFile, Launcher launcher, PrinterConfig printerConfig) {
        try {
            launcher.buildModel();
        } catch (RuntimeException exception) {
            throw new SpoonModelBuildException(
                    srcFile.getPath(),
                    "Cannot build AST model from " + srcFile.getPath() + " with error "
                            + describeModelBuildFailure(exception),
                    exception);
        }
        CtCompilationUnit compilationUnit = extractCompilationUnit(srcFile, launcher);
        CtType<?> mainType = SpoonTypeUtils.findMainType(compilationUnit);
        JHarmonizerOptOuts optOuts = JHarmonizerOptOutResolver.resolve(srcFile, compilationUnit);
        Supplier<SerializedSrcWithSkippedTypeRanges> serializedSrcCode = () -> {
            if (SpoonTypeUtils.hasNoDeclaredTypes(compilationUnit)) {
                return new SerializedSrcWithSkippedTypeRanges(srcFile.getSrcCode(), Map.of());
            }

            SpoonCustomSrcPrinter printer = new SpoonCustomSrcPrinter(
                    launcher.getEnvironment(), srcFile.getSrcCode(), optOuts.getSortingSkippedTypes(), printerConfig);
            return printer.serializeCompilationUnit(compilationUnit);
        };
        return SpoonAstModel.builder()
                .originalMemberSuccessors(RelocationDetector.snapshotOriginalSuccessors(compilationUnit))
                .compilationUnit(compilationUnit)
                .mainType(mainType)
                .serializedSrcCode(serializedSrcCode)
                .optOuts(optOuts)
                .path(srcFile.getPath())
                .build();
    }

    @NonNull
    private static String describeModelBuildFailure(@NonNull RuntimeException exception) {
        String exceptionType = exception.getClass().getSimpleName();
        String exceptionMessage = exception.getMessage();
        if (exceptionMessage == null || exceptionMessage.isBlank()) {
            return exceptionType;
        }
        return exceptionType + ": " + exceptionMessage;
    }

    /**
     * The Launcher is not a thread-safe and must be initialized for each thread
     *
     * @return preconfigured Launcher to parse a stand-alone Java source file without package directory structure
     */
    @NonNull
    private static Launcher createPreconfiguredParserLauncher() {
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setComplianceLevel(JAVA_VERSION);
        launcher.getEnvironment().setCommentEnabled(true);
        launcher.getEnvironment().setPreviewFeaturesEnabled(false);
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setAutoImports(true);
        launcher.getEnvironment().setLevel("ERROR");
        return launcher;
    }

    @NonNull
    private static CtCompilationUnit extractCompilationUnit(@NonNull SrcFile srcFile, Launcher launcher) {
        return launcher.getFactory()
                .CompilationUnit()
                .getOrCreate(srcFile.getPath().toString());
    }
}
