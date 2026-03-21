package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.detectDominantLineSeparator;

import io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils;
import io.github.lemon_ant.jharmonizer.core.translator.SerializedSourceWithSkippedTypeRanges;
import io.github.lemon_ant.jharmonizer.core.translator.SrcCharacterRange;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import spoon.compiler.Environment;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.reflect.visitor.printer.CommentOffset;

/**
 * Custom Spoon source printer that inserts group-separator headers between member groups,
 * preserves the original source fragments for opt-out ranges,
 * and normalises line separators to match the dominant separator of the original file.
 */
class SpoonCustomSourcePrinter extends DefaultJavaPrettyPrinter {
    @NonNull
    private final Set<CtType<?>> sortingSkippedTypes;

    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<@NonNull CtType<?>, @NonNull SrcCharacterRange> sortingSkippedTypeRanges = new HashMap<>();

    @NonNull
    private final SpoonTypeStructurePrinter typeStructurePrinter;

    /**
     * Creates a new SpoonCustomSourcePrinter.
     *
     * @param env the Spoon printing environment
     * @param srcCode the original source text being re-serialized
     * @param sortingSkippedTypes the types that must be copied without sorting
     */
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    SpoonCustomSourcePrinter(
            @NonNull Environment env, @NonNull String srcCode, @NonNull Set<CtType<?>> sortingSkippedTypes) {
        super(env);
        this.sortingSkippedTypes = Collections.unmodifiableSet(sortingSkippedTypes);
        String lineSeparator = detectDominantLineSeparator(srcCode);
        setLineSeparator(lineSeparator);
        this.typeStructurePrinter = new SpoonTypeStructurePrinter(
                srcCode,
                this.sortingSkippedTypes,
                sortingSkippedTypeRanges,
                this::getResult,
                this::getPrinterTokenWriter);
    }

    /**
     * Visits an annotation type and prints it using the shared type-structure logic.
     *
     * @param annotationType the annotation type to print
     */
    @Override
    public <A extends Annotation> void visitCtAnnotationType(@NonNull CtAnnotationType<A> annotationType) {
        typeStructurePrinter.printTypeStructure(annotationType);
    }

    /**
     * Performs the visit ct class.
     * @param ctClass the ct class
     */
    @Override
    public <T> void visitCtClass(@NonNull CtClass<T> ctClass) {
        typeStructurePrinter.printTypeStructure(ctClass);
    }

    /**
     * Performs the visit ct enum.
     * @param ctEnum the ct enum
     */
    @Override
    public <T extends Enum<?>> void visitCtEnum(@NonNull CtEnum<T> ctEnum) {
        typeStructurePrinter.printTypeStructure(ctEnum);
    }

    /**
     * Performs the visit ct interface.
     * @param intrface the intrface
     */
    @Override
    public <T> void visitCtInterface(@NonNull CtInterface<T> intrface) {
        typeStructurePrinter.printTypeStructure(intrface);
    }

    /**
     * Performs the visit ct record.
     * @param recordType the record type
     */
    @Override
    public void visitCtRecord(@NonNull CtRecord recordType) {
        typeStructurePrinter.printTypeStructure(recordType);
    }

    /**
     * Serializes the compilation unit and returns both the source code and skipped-type ranges.
     *
     * @param compilationUnit the compilation unit to print
     * @return the serialized source with skipped-type ranges
     */
    @NonNull
    SerializedSourceWithSkippedTypeRanges serializeCompilationUnit(@NonNull CtCompilationUnit compilationUnit) {
        printCompilationUnit(compilationUnit);
        return new SerializedSourceWithSkippedTypeRanges(getResult(), sortingSkippedTypeRanges);
    }

    /**
     * Performs the visit ct compilation unit.
     * @param compilationUnit the compilation unit to inspect
     */
    @Override
    public void visitCtCompilationUnit(@NonNull CtCompilationUnit compilationUnit) {
        if (compilationUnit.getUnitType() != CtCompilationUnit.UNIT_TYPE.TYPE_DECLARATION) {
            // TODO Test this logic, I think we must return after the super.visitCtCompilationUnit(compilationUnit);
            super.visitCtCompilationUnit(compilationUnit);
        }
        CtCompilationUnit outerCompilationUnit = this.sourceCompilationUnit;
        try {
            this.sourceCompilationUnit = compilationUnit;
            List<CtType<?>> rootTypes = SpoonTypeUtils.getRootTypes(compilationUnit);
            int firstTypeStart = rootTypes.stream()
                    .mapToInt(typeMember -> typeMember.getPosition().getSourceStart())
                    .min()
                    .orElseThrow(IllegalStateException::new);
            int typeDeclarationHeaderEnd = Math.max(firstTypeStart - 1, 0);
            if (typeDeclarationHeaderEnd > 0) {
                typeStructurePrinter.printOriginalFragment(0, typeDeclarationHeaderEnd);
            }

            rootTypes.forEach(this::scan);
            getElementPrinterHelper().writeComment(compilationUnit, CommentOffset.AFTER);
        } finally {
            this.sourceCompilationUnit = outerCompilationUnit;
        }
        // by convention, we add a newline at the end of the file
        // we guard this with a check to avoid adding a newline if there is already one
        if (!getResult().endsWith(getLineSeparator())) {
            getPrinterTokenWriter().writeln();
        }
    }
}
