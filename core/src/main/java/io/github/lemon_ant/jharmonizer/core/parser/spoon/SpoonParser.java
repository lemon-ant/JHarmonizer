package io.github.lemon_ant.jharmonizer.core.parser.spoon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
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

    public static SpoonASTModel parseJavaSourceResource(Path javaSourcePath) throws IOException {
        Path normalizedSourcePath = javaSourcePath.normalize().toAbsolutePath();
        String originalSourceCode = Files.readString(normalizedSourcePath);
        return parseJavaSourceResource(normalizedSourcePath, originalSourceCode);
    }

    public static SpoonASTModel parseJavaSourceResource(Path originalSourceFile, String originalSourceCode) {
        VirtualFile virtualFile = new VirtualFile(originalSourceCode, originalSourceFile.toString());

        Launcher originalLauncher = createPreconfiguredParserLauncher();
        originalLauncher.addInputResource(virtualFile);

        Launcher workingLauncher = createPreconfiguredParserLauncher();
        workingLauncher.addInputResource(virtualFile);
        workingLauncher
                .getEnvironment()
                .setPrettyPrinterCreator(
                        () -> new SpoonCustomSourcePrinter(workingLauncher.getEnvironment(), originalSourceCode));

        return buildSpoonASTModel(/*originalLauncher,*/ workingLauncher);
    }

    private static SpoonASTModel buildSpoonASTModel(
            /* @NonNull Launcher originalLauncher, */ @NonNull Launcher workingLauncher) {
        // var originalCompilationUnit = extractCompilationUnit(originalLauncher);
        var workingCompilationUnit = extractCompilationUnit(workingLauncher);
        var mainType = SpoonCompilationUnitUtilities.findMainType(workingCompilationUnit);
        Supplier<String> serializedSourceCode =
                () -> workingLauncher.createPrettyPrinter().printCompilationUnit(workingCompilationUnit);
        return SpoonASTModel.builder()
                // .originalCompilationUnit(originalCompilationUnit)
                .workingCompilationUnit(workingCompilationUnit)
                .mainType(mainType)
                .serializedSourceCode(serializedSourceCode)
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
        if (allTypes.isEmpty()) {
            throw new IllegalStateException(/*TODO*/ );
        }
        CtType<?> firstType = allTypes.iterator().next();
        return firstType.getPosition().getCompilationUnit();
    }
}
