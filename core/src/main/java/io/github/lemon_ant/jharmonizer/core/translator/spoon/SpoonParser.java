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

    /**
     * Parses the java source resource.
     * @param javaSourcePath the Java source path to parse
     * @return the java source resource
     */
    @NonNull
    public static SpoonAstModel parseJavaSourceResource(@NonNull Path javaSourcePath) throws IOException {
        Path normalizedSourcePath = javaSourcePath.normalize().toAbsolutePath();
        String originalSourceCode = Files.readString(normalizedSourcePath);
        return parseJavaSourceResource(normalizedSourcePath, originalSourceCode);
    }

    /**
     * Parses the java source resource.
     * @param originalSourceFile the original source file
     * @param originalSourceCode the original source code
     * @return the java source resource
     */
    @NonNull
    public static SpoonAstModel parseJavaSourceResource(
            @NonNull Path originalSourceFile, @NonNull String originalSourceCode) {
        VirtualFile virtualFile = new VirtualFile(originalSourceCode, originalSourceFile.toString());

        Launcher launcher = createPreconfiguredParserLauncher();
        launcher.addInputResource(virtualFile);

        return buildSpoonAstModel(originalSourceFile, originalSourceCode, launcher);
    }

    @NonNull
    private static SpoonAstModel buildSpoonAstModel(
            @NonNull Path path, @NonNull String originalSourceCode, @NonNull Launcher launcher) {
        CtCompilationUnit compilationUnit = extractCompilationUnit(launcher);
        CtType<?> mainType = SpoonTypeUtils.findMainType(compilationUnit);
        JHarmonizerOptOuts optOuts = JHarmonizerOptOutResolver.resolve(path, originalSourceCode, compilationUnit);
        Supplier<SerializedSourceSnapshot> serializedSourceCode = () -> {
            SpoonCustomSourcePrinter printer = new SpoonCustomSourcePrinter(
                    launcher.getEnvironment(), originalSourceCode, optOuts.getFormattingSkippedTypes());
            return printer.serializeCompilationUnit(compilationUnit);
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
    private static CtCompilationUnit extractCompilationUnit(Launcher launcher) {
        Collection<CtType<?>> allTypes = launcher.buildModel().getAllTypes();
        // TODO Flesh out the corner cases with package-info.java and module-info.java
        Validate.notEmpty(allTypes, "AllTypes cannot be empty");
        CtType<?> firstType = allTypes.iterator().next();
        return firstType.getPosition().getCompilationUnit();
    }
}
