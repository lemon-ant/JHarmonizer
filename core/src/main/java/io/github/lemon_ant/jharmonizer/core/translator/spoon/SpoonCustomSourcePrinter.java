package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.GROUP_HEADER_METADATA;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.detectDominantLineSeparator;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.findIndentationStart;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.needsSeparatorAfter;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.needsSeparatorBefore;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.optout.SourceCharacterRange;
import io.github.lemon_ant.jharmonizer.core.translator.SerializedSourceWithSkippedTypeRanges;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import spoon.compiler.Environment;
import spoon.reflect.code.CtComment;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
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
    private final Map<@NonNull CtType<?>, @NonNull SourceCharacterRange> sortingSkippedTypeRanges =
            new ConcurrentHashMap<>();

    @NonNull
    private final TypeStructurePrinter typeStructurePrinter;

    @NonNull
    private final String originalSourceCode;

    /**
     * Creates a new SpoonCustomSourcePrinter.
     *
     * @param env the env
     * @param srcFile the original source file
     * @param sortingSkippedTypes the types that must be copied without sorting
     */
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    SpoonCustomSourcePrinter(
            @NonNull Environment env, @NonNull SrcFile srcFile, @NonNull Set<CtType<?>> sortingSkippedTypes) {
        super(env);
        this.sortingSkippedTypes = Set.copyOf(sortingSkippedTypes);
        this.typeStructurePrinter = new TypeStructurePrinter();
        String lineSeparator = detectDominantLineSeparator(srcFile.getSrcCode());
        setLineSeparator(lineSeparator);
        this.originalSourceCode = srcFile.getSrcCode();
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

    @NonNull
    private spoon.reflect.visitor.TokenWriter printOriginalFragment(int start, int end) {
        int startWithIndent = findIndentationStart(start, originalSourceCode);
        if (startWithIndent <= end && end <= originalSourceCode.length()) {
            String originalCodeFragment =
                    StringUtils.stripEnd(originalSourceCode.substring(startWithIndent, end + 1), null);
            return getPrinterTokenWriter()
                    .writeCodeSnippet(originalCodeFragment)
                    .writeln();
        }
        throw new IllegalStateException("Invalid source fragment range: start=" + start
                + ", end=" + end
                + ", indentationStart=" + startWithIndent
                + ", sourceLength=" + originalSourceCode.length()
                + ". Expected indentationStart <= end < sourceLength.");
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
            super.visitCtCompilationUnit(compilationUnit);
        }
        CtCompilationUnit outerCompilationUnit = this.sourceCompilationUnit;
        try {
            this.sourceCompilationUnit = compilationUnit;
            int firstTypeStart = compilationUnit.getDeclaredTypes().stream()
                    .mapToInt(this::findRenderedTypeStart)
                    .min()
                    .orElseThrow(IllegalStateException::new);
            int typeDeclarationHeaderEnd = (firstTypeStart > 0) ? firstTypeStart - 1 : 0;
            if (typeDeclarationHeaderEnd > 0) {
                printOriginalFragment(0, typeDeclarationHeaderEnd);
            }

            compilationUnit.getDeclaredTypes().forEach(this::scan);
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

    @NonNull
    private int findRenderedTypeStart(CtType<?> type) {
        // TODO Find out why we are searching type's start differently
        if (!sortingSkippedTypes.contains(type)) {
            return type.getPosition().getSourceStart();
        }
        return findIndentationStart(
                type.getComments().stream()
                        .map(CtComment::getPosition)
                        .mapToInt(SourcePosition::getSourceStart)
                        .min()
                        .orElse(type.getPosition().getSourceStart()),
                originalSourceCode);
    }

    // This helper stays non-static because it must coordinate outer printer state such as token writer access,
    // preserved-range accumulation, and recursive rendering via findRenderedTypeStart/printOriginalFragment.
    private final class TypeStructurePrinter {
        private void printTypeStructure(CtType<?> type) {
            getPrinterTokenWriter().writeln();
            if (sortingSkippedTypes.contains(type)) {
                printPreservedSkippedType(type);
                return;
            }
            SourcePosition typePosition = type.getPosition();
            List<CtTypeMember> explicitTypeMembers = findExplicitTypeMembers(type);

            if (explicitTypeMembers.isEmpty()) {
                // If no nested elements, then print the original source fragment entirely
                // TODO Check if we have comments before and after
                printOriginalFragment(typePosition.getSourceStart(), typePosition.getSourceEnd())
                        .writeln();
                return;
            }
            printStructuredTypeMembers(typePosition, explicitTypeMembers);
        }

        private void printPreservedSkippedType(CtType<?> type) {
            int outputStart = getResult().length();
            printOriginalFragment(
                    findRenderedTypeStart(type), type.getPosition().getSourceEnd());
            int outputEndExclusive = getResult().length();
            sortingSkippedTypeRanges.put(type, new SourceCharacterRange(outputStart, outputEndExclusive));
        }

        @NonNull
        private List<CtTypeMember> findExplicitTypeMembers(CtType<?> type) {
            return type.getTypeMembers().stream()
                    // Spoon creates implicit constructors which don't exist in the source code
                    .filter(typeMember -> typeMember.getPosition().isValidPosition())
                    /* TODO(RECORDS_DISABLED): Remove this guard when record headers/components are printed correctly.
                    Today implicit record fields/components still produce wrong source-printer output. */
                    .filter(typeMember -> !typeMember.isImplicit())
                    .toList();
        }

        private void printStructuredTypeMembers(SourcePosition typePosition, List<CtTypeMember> explicitTypeMembers) {
            int minMemberStart = explicitTypeMembers.stream()
                    .mapToInt(this::findMemberStart)
                    .min()
                    .orElseThrow(IllegalStateException::new);

            printOriginalFragment(typePosition.getSourceStart(), minMemberStart - 1);

            boolean first = true;
            boolean previousElementNeedSeparatorAfter = false;
            for (CtTypeMember member : explicitTypeMembers) {
                previousElementNeedSeparatorAfter = printStructuredTypeMember(
                        member, explicitTypeMembers, first, previousElementNeedSeparatorAfter);
                first = false;
            }

            int maxMemberEnd = explicitTypeMembers.stream()
                    .mapToInt(typeMember -> typeMember.getPosition().getSourceEnd())
                    .max()
                    .orElseThrow(IllegalStateException::new);
            printOriginalFragment(maxMemberEnd + 1, typePosition.getSourceEnd());
        }

        private boolean printStructuredTypeMember(
                CtTypeMember member,
                List<CtTypeMember> explicitTypeMembers,
                boolean first,
                boolean previousElementNeedSeparatorAfter) {
            // TODO Orphaned comments
            boolean needsSeparatorBeforeCurrentMember = needsSeparatorBefore(member, first);
            if (needsSeparatorBeforeCurrentMember || previousElementNeedSeparatorAfter) {
                getPrinterTokenWriter().writeln();
            }
            boolean currentElementNeedsSeparatorAfter = needsSeparatorAfter(member);

            if (member instanceof CtType<?> typeMember) {
                printTypeStructure(typeMember);
                return currentElementNeedsSeparatorAfter;
            }

            Optional<String> groupHeaderMetadata = Optional.ofNullable(member.getMetadata(GROUP_HEADER_METADATA))
                    .map(Object::toString);
            groupHeaderMetadata.ifPresent(groupHeader -> {
                if (!groupHeader.isEmpty()) {
                    getPrinterTokenWriter()
                            .writeCodeSnippet("// " + groupHeader)
                            .writeln();
                } else {
                    getPrinterTokenWriter().writeln();
                }
            });
            int nextElementStart = explicitTypeMembers.stream()
                    .mapToInt(this::findMemberStart)
                    .filter(start -> start > member.getPosition().getSourceEnd())
                    .min()
                    .orElse(member.getPosition().getSourceEnd() + 1);
            printOriginalFragment(member.getPosition().getSourceStart(), nextElementStart - 1);
            return currentElementNeedsSeparatorAfter;
        }

        private int findMemberStart(CtTypeMember typeMember) {
            if (typeMember instanceof CtType<?> nestedType) {
                return findRenderedTypeStart(nestedType);
            }
            return typeMember.getPosition().getSourceStart();
        }
    }
}
