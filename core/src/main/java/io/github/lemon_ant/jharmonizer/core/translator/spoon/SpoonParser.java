package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import io.github.lemon_ant.jharmonizer.core.common.SrcFile;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutResolver;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOuts;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils;
import io.github.lemon_ant.jharmonizer.core.translator.SerializedSourceWithSkippedTypeRanges;
import java.io.IOException;
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
     * @param javaSrcPath the Java source path to parse
     * @return the java source resource
     */
    @NonNull
    public static SpoonAstModel parseJavaSourceResource(@NonNull Path javaSourcePath) throws IOException {
        Path normalizedSourcePath = javaSourcePath.normalize().toAbsolutePath();
        return parseJavaSourceResource(SourceFilesHandler.readFile(normalizedSourcePath));
    }

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
    private static SpoonAstModel buildSpoonAstModel(@NonNull SrcFile srcFile, @NonNull Launcher launcher) {
        CtCompilationUnit compilationUnit = extractCompilationUnit(launcher);
        CtType<?> mainType = SpoonTypeUtils.findMainType(compilationUnit);
        JHarmonizerOptOuts optOuts = JHarmonizerOptOutResolver.resolve(srcFile, compilationUnit);
        Supplier<SerializedSourceWithSkippedTypeRanges> serializedSourceCode = () -> {
            SpoonCustomSourcePrinter printer =
                    new SpoonCustomSourcePrinter(launcher.getEnvironment(), srcFile, optOuts.getSortingSkippedTypes());
            return printer.serializeCompilationUnit(compilationUnit);
        };
        return SpoonAstModel.builder()
                .originalElements2OrderIndices(RelocationDetector.indexElementsByOrder(compilationUnit))
                .compilationUnit(compilationUnit)
                .mainType(mainType)
                .serializedSourceCode(serializedSourceCode)
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
    private static CtCompilationUnit extractCompilationUnit(Launcher launcher) {
        Collection<CtType<?>> allTypes = launcher.buildModel().getAllTypes();
        // TODO Flesh out the corner cases with package-info.java and module-info.java
        Validate.notEmpty(allTypes, "AllTypes cannot be empty");
        CtType<?> firstType = allTypes.iterator().next();
        return firstType.getPosition().getCompilationUnit();
    }
}
