// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.detectDominantLineSeparator;

import io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils;
import io.github.lemon_ant.jharmonizer.core.translator.SerializedSrcWithSkippedTypeRanges;
import java.lang.annotation.Annotation;
import java.util.List;
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
import spoon.reflect.visitor.DefaultTokenWriter;
import spoon.reflect.visitor.printer.CommentOffset;

/**
 * Custom Spoon source printer that inserts group-separator headers between member groups,
 * preserves the original source fragments for opt-out ranges,
 * and normalises line separators to match the dominant separator of the original file.
 */
class SpoonCustomSrcPrinter extends DefaultJavaPrettyPrinter {

    @NonNull
    private final SpoonPrinterHelper printerHelper;

    @NonNull
    private final SpoonTypeStructurePrinter typeStructurePrinter;

    /**
     * Visits an annotation type and prints it using the shared type-structure logic.
     *
     * @param annotationType the annotation type to print
     */
    @Override
    public <A extends Annotation> void visitCtAnnotationType(@NonNull CtAnnotationType<A> annotationType) {
        typeStructurePrinter.printType(annotationType);
    }

    /**
     * Performs the visit ct class.
     * @param ctClass the ct class
     */
    @Override
    public <T> void visitCtClass(@NonNull CtClass<T> ctClass) {
        typeStructurePrinter.printType(ctClass);
    }

    /**
     * Performs the visit ct compilation unit.
     * @param compilationUnit the compilation unit to inspect
     */
    @Override
    public void visitCtCompilationUnit(@NonNull CtCompilationUnit compilationUnit) {
        if (SpoonTypeUtils.hasNoDeclaredTypes(compilationUnit)) {
            // For non-type-declaration units and type-declaration files without any declared types,
            // delegate to the default implementation and stop.
            super.visitCtCompilationUnit(compilationUnit);
            return;
        }
        List<CtType<?>> rootTypes = SpoonTypeUtils.getRootTypes(compilationUnit);
        int firstTypeStart = rootTypes.stream()
                .mapToInt(typeMember -> typeMember.getPosition().getSourceStart())
                .min()
                .orElseThrow(IllegalStateException::new);
        if (typeStructurePrinter.printOriginalFragment(0, firstTypeStart - 1)) {
            printerHelper.writeln();
        }

        // The compilation unit owns root-type separators; nested-type separators belong to their parent.
        boolean first = true;
        for (CtType<?> rootType : rootTypes) {
            if (!first) {
                printerHelper.writeln();
            }
            scan(rootType);
            first = false;
        }
        // Preserve trailing comments attached to the compilation unit, which type fragments do not cover.
        getElementPrinterHelper().writeComment(compilationUnit, CommentOffset.AFTER);
        // TODO Can we avoid this method call and logically understand that we are at the end of the file and we
        // must add a new line or it was added previously
        printerHelper.terminateLine();
    }

    /**
     * Performs the visit ct enum.
     * @param ctEnum the ct enum
     */
    @Override
    public <T extends Enum<?>> void visitCtEnum(@NonNull CtEnum<T> ctEnum) {
        typeStructurePrinter.printType(ctEnum);
    }

    /**
     * Performs the visit ct interface.
     * @param intrface the intrface
     */
    @Override
    public <T> void visitCtInterface(@NonNull CtInterface<T> intrface) {
        typeStructurePrinter.printType(intrface);
    }

    /**
     * Performs the visit ct record.
     * @param recordType the record type
     */
    @Override
    public void visitCtRecord(@NonNull CtRecord recordType) {
        typeStructurePrinter.printType(recordType);
    }

    /**
     * Creates a new SpoonCustomSrcPrinter.
     *
     * @param env the Spoon printing environment
     * @param srcCode the original source text being re-serialized
     * @param sortingSkippedTypes the types that must be copied without sorting
     * @param printerConfig the printer configuration
     */
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    SpoonCustomSrcPrinter(
            @NonNull Environment env,
            @NonNull String srcCode,
            @NonNull Set<CtType<?>> sortingSkippedTypes,
            @NonNull PrinterConfig printerConfig) {
        super(env);
        this.printerHelper = new SpoonPrinterHelper(env);
        setPrinterTokenWriter(new DefaultTokenWriter(printerHelper));
        String lineSeparator = detectDominantLineSeparator(srcCode);
        setLineSeparator(lineSeparator);
        this.typeStructurePrinter =
                new SpoonTypeStructurePrinter(srcCode, sortingSkippedTypes, printerHelper, printerConfig);
    }

    /**
     * Serializes the compilation unit and returns both the source code and skipped-type ranges.
     *
     * @param compilationUnit the compilation unit to print
     * @return the serialized source with skipped-type ranges
     */
    @NonNull
    SerializedSrcWithSkippedTypeRanges serializeCompilationUnit(@NonNull CtCompilationUnit compilationUnit) {
        String serializedSrcCode = printCompilationUnit(compilationUnit);
        return new SerializedSrcWithSkippedTypeRanges(
                serializedSrcCode, typeStructurePrinter.getSortingSkippedTypeRanges());
    }
}
