package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutResolver;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOuts;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils;
import io.github.lemon_ant.jharmonizer.core.translator.SerializedSourceWithSkippedTypeRanges;
import java.util.Map;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.Validate;
import spoon.Launcher;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.support.compiler.VirtualFile;

@UtilityClass
public class SpoonParser {

    private static final int JAVA_VERSION = 21;

    /**
     * Parses the java source resource.
     * @param srcFile the original source file
     * @return the java source resource
     */
    @NonNull
    public static SpoonAstModel parseJavaSrcFile(@NonNull SrcFile srcFile) {
        VirtualFile virtualFile =
                new VirtualFile(srcFile.getSrcCode(), srcFile.getPath().toString());

        Launcher launcher = createPreconfiguredParserLauncher();
        launcher.addInputResource(virtualFile);

        return buildSpoonAstModel(srcFile, launcher);
    }

    @NonNull
    private static SpoonAstModel buildSpoonAstModel(SrcFile srcFile, Launcher launcher) {
        launcher.buildModel();
        CtCompilationUnit compilationUnit = extractCompilationUnit(srcFile, launcher);
        CtType<?> mainType = SpoonTypeUtils.findMainType(compilationUnit);
        JHarmonizerOptOuts optOuts = JHarmonizerOptOutResolver.resolve(srcFile, compilationUnit);
        Supplier<SerializedSourceWithSkippedTypeRanges> serializedSrcCode = () -> {
            if (SpoonTypeUtils.hasNoDeclaredTypes(compilationUnit)) {
                return new SerializedSourceWithSkippedTypeRanges(srcFile.getSrcCode(), Map.of());
            }

            SpoonCustomSourcePrinter printer = new SpoonCustomSourcePrinter(
                    launcher.getEnvironment(), srcFile.getSrcCode(), optOuts.getSortingSkippedTypes());
            return printer.serializeCompilationUnit(compilationUnit);
        };
        return SpoonAstModel.builder()
                .originalElements2OrderIndices(RelocationDetector.indexElementsByOrder(compilationUnit))
                .compilationUnit(compilationUnit)
                .mainType(mainType)
                .serializedSrcCode(serializedSrcCode)
                .optOuts(optOuts)
                .path(srcFile.getPath())
                .build();
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
        return launcher;
    }

    @NonNull
    private static CtCompilationUnit extractCompilationUnit(@NonNull SrcFile srcFile, Launcher launcher) {
        Map<String, CompilationUnit> compilationUnitsByPath =
                launcher.getFactory().CompilationUnit().getMap();
        Validate.validState(
                compilationUnitsByPath.size() <= 1,
                "Expected at most one parsed compilation unit, got %d",
                compilationUnitsByPath.size());

        CompilationUnit compilationUnit = compilationUnitsByPath.isEmpty()
                ? createMissingSrcCompilationUnit(srcFile, launcher)
                : compilationUnitsByPath.values().iterator().next();
        Validate.validState(
                compilationUnit instanceof CtCompilationUnit,
                "Expected Spoon compilation unit to implement CtCompilationUnit, got %s",
                compilationUnit.getClass().getName());
        return (CtCompilationUnit) compilationUnit;
    }

    @NonNull
    private static CompilationUnit createMissingSrcCompilationUnit(@NonNull SrcFile srcFile, Launcher launcher) {
        // Spoon does not always cache a compilation unit for comment-only / effectively empty source files.
        // We still create one keyed by the virtual src path so downstream code can keep treating the file as a
        // compilation unit and preserve the original src text instead of failing on missing declared types.
        return launcher.getFactory()
                .CompilationUnit()
                .getOrCreate(srcFile.getPath().toString());
    }
}
