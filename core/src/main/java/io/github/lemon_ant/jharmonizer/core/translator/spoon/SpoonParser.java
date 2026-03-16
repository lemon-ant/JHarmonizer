package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutResolver;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOuts;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils;
import io.github.lemon_ant.jharmonizer.core.translator.SerializedSourceSnapshot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.Validate;
import spoon.Launcher;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.support.compiler.VirtualFile;

@UtilityClass
public class SpoonParser {

    private static final int JAVA_VERSION = 21;

    public static SpoonAstModel parseJavaSourceResource(Path javaSourcePath) throws IOException {
        Path normalizedSourcePath = javaSourcePath.normalize().toAbsolutePath();
        String originalSourceCode = Files.readString(normalizedSourcePath);
        return parseJavaSourceResource(normalizedSourcePath, originalSourceCode);
    }

    public static SpoonAstModel parseJavaSourceResource(Path originalSourceFile, String originalSourceCode) {
        VirtualFile virtualFile = new VirtualFile(originalSourceCode, originalSourceFile.toString());

        Launcher launcher = createPreconfiguredParserLauncher();
        launcher.addInputResource(virtualFile);

        return buildSpoonAstModel(originalSourceFile, originalSourceCode, launcher);
    }

    private static SpoonAstModel buildSpoonAstModel(
            @NonNull Path path, @NonNull String originalSourceCode, @NonNull Launcher launcher) {
        CtCompilationUnit compilationUnit = extractCompilationUnit(launcher);
        CtType<?> mainType = SpoonTypeUtils.findMainType(compilationUnit);
        JHarmonizerOptOuts optOuts = JHarmonizerOptOutResolver.resolve(path, originalSourceCode, compilationUnit);
        Supplier<SerializedSourceSnapshot> serializedSourceCode = () -> {
            SpoonCustomSourcePrinter printer = new SpoonCustomSourcePrinter(
                    launcher.getEnvironment(), originalSourceCode, optOuts.getFormattingSkippedTypes());
            printer.printCompilationUnit(compilationUnit);
            return new SerializedSourceSnapshot(printer.getResult(), printer.getFormattingExclusionRanges());
        };
        return SpoonAstModel.builder()
                .originalElements2OrderIndices(RelocationDetector.indexElementsByOrder(compilationUnit))
                .compilationUnit(compilationUnit)
                .mainType(mainType)
                .serializedSourceCode(serializedSourceCode)
                .optOuts(optOuts)
                .path(path)
                .build();
    }

    /**
     * The Launcher is not a thread-safe and must be initialized for each thread
     *
     * @return preconfigured Launcher to parse a stand-alone Java source file without package directory structure
     */
    private static Launcher createPreconfiguredParserLauncher() {
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setComplianceLevel(JAVA_VERSION);
        launcher.getEnvironment().setCommentEnabled(true);
        launcher.getEnvironment().setPreviewFeaturesEnabled(false);
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setAutoImports(true);
        return launcher;
    }

    private static CtCompilationUnit extractCompilationUnit(Launcher launcher) {
        Collection<CtType<?>> allTypes = launcher.buildModel().getAllTypes();
        // TODO Flesh out the corner cases with package-info.java and module-info.java
        Validate.notEmpty(allTypes, "AllTypes cannot be empty");
        CtType<?> firstType = allTypes.iterator().next();
        return firstType.getPosition().getCompilationUnit();
    }
}
