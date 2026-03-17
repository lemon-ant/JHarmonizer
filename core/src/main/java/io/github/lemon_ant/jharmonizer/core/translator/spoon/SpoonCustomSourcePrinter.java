package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.GROUP_HEADER_METADATA;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.detectDominantLineSeparator;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.findIndentationStart;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.needsSeparatorAfter;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.needsSeparatorBefore;

import io.github.lemon_ant.jharmonizer.core.optout.SourceCharacterRange;
import io.github.lemon_ant.jharmonizer.core.translator.SerializedSourceSnapshot;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import spoon.compiler.Environment;
import spoon.reflect.code.CtComment;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtCompilationUnit.UNIT_TYPE;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.reflect.visitor.TokenWriter;
import spoon.reflect.visitor.printer.CommentOffset;

/**
 * Custom Spoon source printer that inserts group-separator headers between member groups,
 * preserves the original source fragments for opt-out ranges,
 * and normalises line separators to match the dominant separator of the original file.
 */
@SuppressWarnings("PMD") // TODO @Copilot fix all PMD warnings and delete this line
class SpoonCustomSourcePrinter extends DefaultJavaPrettyPrinter {
    @NonNull
    private final Set<CtType<?>> formattingSkippedTypes;

    @NonNull
    private final List<SourceCharacterRange> formattingExclusionRanges = new ArrayList<>();

    @NonNull
    private final String originalSourceCode;

    /**
     * Creates a new SpoonCustomSourcePrinter.
     *
     * @param env the env
     * @param originalSourceCode the original source code
     * @param formattingSkippedTypes the types that must stay unformatted
     */
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    SpoonCustomSourcePrinter(
            @NonNull Environment env,
            @NonNull String originalSourceCode,
            @NonNull Set<CtType<?>> formattingSkippedTypes) {
        super(env);
        this.formattingSkippedTypes = Set.copyOf(formattingSkippedTypes);
        this.originalSourceCode = originalSourceCode;
        String lineSeparator = detectDominantLineSeparator(originalSourceCode);
        setLineSeparator(lineSeparator);
    }

    /**
     * Visits an annotation type and prints it using the shared type-structure logic.
     *
     * @param annotationType the annotation type to print
     */
    @Override
    public <A extends Annotation> void visitCtAnnotationType(@NonNull CtAnnotationType<A> annotationType) {
        printTypeStructure(annotationType);
    }

    /**
     * Performs the visit ct class.
     * @param ctClass the ct class
     */
    @Override
    public <T> void visitCtClass(@NonNull CtClass<T> ctClass) {
        printTypeStructure(ctClass);
    }

    /**
     * Performs the visit ct enum.
     * @param ctEnum the ct enum
     */
    @Override
    public <T extends Enum<?>> void visitCtEnum(@NonNull CtEnum<T> ctEnum) {
        printTypeStructure(ctEnum);
    }

    /**
     * Performs the visit ct interface.
     * @param intrface the intrface
     */
    @Override
    public <T> void visitCtInterface(@NonNull CtInterface<T> intrface) {
        printTypeStructure(intrface);
    }

    /**
     * Performs the visit ct record.
     * @param recordType the record type
     */
    @Override
    public void visitCtRecord(@NonNull CtRecord recordType) {
        printTypeStructure(recordType);
    }

    @NonNull
    private TokenWriter printOriginalFragment(int start, int end) {
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
     * Serializes the compilation unit and returns both the source code and formatting exclusions.
     *
     * @param compilationUnit the compilation unit to print
     * @return the serialized source snapshot
     */
    @NonNull
    SerializedSourceSnapshot serializeCompilationUnit(@NonNull CtCompilationUnit compilationUnit) {
        printCompilationUnit(compilationUnit);
        return new SerializedSourceSnapshot(getResult(), formattingExclusionRanges);
    }

    @NonNull
    private void printTypeStructure(CtType<?> type) {
        getPrinterTokenWriter().writeln();
        if (formattingSkippedTypes.contains(type)) {
            // Once an outer type is preserved as-is, nested types are not traversed separately, so formatting
            // exclusions are emitted only for the outer preserved fragment and cannot overlap.
            SourceCharacterRange preservedSourceRange = new SourceCharacterRange(
                    findRenderedTypeStart(type), type.getPosition().getSourceEnd() + 1);
            int outputStart = getResult().length();
            printOriginalFragment(preservedSourceRange.getStartInclusive(), preservedSourceRange.getEndExclusive() - 1);
            int outputEndExclusive = getResult().length() - getLineSeparator().length();
            formattingExclusionRanges.add(new SourceCharacterRange(outputStart, outputEndExclusive));
            return;
        }
        SourcePosition typePosition = type.getPosition();

        List<CtTypeMember> explicitTypeMembers = type.getTypeMembers().stream()
                // Spoon creates implicit constructors which don't exist in the source code
                .filter(typeMember -> typeMember.getPosition().isValidPosition())
                /* TODO(RECORDS_DISABLED): Remove this guard to start processing record implicit fields/components.
                Disabled until the source printer can correctly print record headers/components. */
                .filter(typeMember -> !typeMember.isImplicit())
                .toList();

        if (explicitTypeMembers.isEmpty()) {
            // If no nested elements, then print the original source fragment entirely
            printOriginalFragment(typePosition.getSourceStart(), typePosition.getSourceEnd())
                    .writeln();
            return;
        }

        // TODO Optimize algorithm by precalculating of the original member sequence
        // Find the minimal nested element start position
        int minMemberStart = explicitTypeMembers.stream()
                .mapToInt(typeMember -> typeMember instanceof CtType<?> nestedType
                        ? findRenderedTypeStart(nestedType)
                        : typeMember.getPosition().getSourceStart())
                .min()
                .orElseThrow(IllegalStateException::new);

        // Print the type header until the start of the topmost element
        printOriginalFragment(typePosition.getSourceStart(), minMemberStart - 1);

        // Print elements in the actual possibly resorted order
        boolean first = true;
        boolean previousElementNeedSeparatorAfter = false;
        for (CtTypeMember member : explicitTypeMembers) {

            // TODO Orphaned comments

            boolean needsSeparatorBefore = needsSeparatorBefore(member, first);
            if (needsSeparatorBefore || previousElementNeedSeparatorAfter) {
                getPrinterTokenWriter().writeln();
            }
            previousElementNeedSeparatorAfter = needsSeparatorAfter(member);
            first = false;

            if (member instanceof CtType<?> typeMember) {
                // Nested type declaration
                printTypeStructure(typeMember);
                continue;
            }

            // The member was marked as the first member of a group
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

            // TODO Optimize algorithm by precalculating of the original member sequence
            // Copy class member code from the original code without changes
            int nextElementStart = explicitTypeMembers.stream()
                    .mapToInt(typeMember -> typeMember instanceof CtType<?> nestedType
                            ? findRenderedTypeStart(nestedType)
                            : typeMember.getPosition().getSourceStart())
                    .filter(start -> start > member.getPosition().getSourceEnd())
                    .min()
                    .orElse(member.getPosition().getSourceEnd() + 1);
            printOriginalFragment(member.getPosition().getSourceStart(), nextElementStart - 1);
        }

        // TODO Optimize algorithm by precalculating of the original member sequence
        // Print the type footer from the end of the bottommost nested element until end of the type fragment
        int maxMemberEnd = explicitTypeMembers.stream()
                .mapToInt(typeMember -> typeMember.getPosition().getSourceEnd())
                .max()
                .orElseThrow(IllegalStateException::new);

        // TODO Check trailing comments
        printOriginalFragment(maxMemberEnd + 1, typePosition.getSourceEnd());
        // TODO Check trailing indents
    }

    /**
     * Performs the visit ct compilation unit.
     * @param compilationUnit the compilation unit to inspect
     */
    @Override
    public void visitCtCompilationUnit(@NonNull CtCompilationUnit compilationUnit) {
        if (compilationUnit.getUnitType() != UNIT_TYPE.TYPE_DECLARATION) {
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
        if (!formattingSkippedTypes.contains(type)) {
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
}
