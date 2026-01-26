package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.GROUP_HEADER_METADATA;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.detectDominantLineSeparator;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.findIndentationStart;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.needsSeparatorAfter;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.needsSeparatorBefore;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import spoon.compiler.Environment;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtCompilationUnit.UNIT_TYPE;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.reflect.visitor.TokenWriter;
import spoon.reflect.visitor.printer.CommentOffset;

class SpoonCustomSourcePrinter extends DefaultJavaPrettyPrinter {
    private final String originalSourceCode;

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    SpoonCustomSourcePrinter(Environment env, String originalSourceCode) {
        super(env);
        this.originalSourceCode = originalSourceCode;
        String lineSeparator = detectDominantLineSeparator(originalSourceCode);
        setLineSeparator(lineSeparator);
    }

    @Override
    public <A extends Annotation> void visitCtAnnotationType(CtAnnotationType<A> annotationType) {
        printTypeStructure(annotationType);
    }

    @Override
    public <T> void visitCtClass(CtClass<T> ctClass) {
        printTypeStructure(ctClass);
    }

    @Override
    public <T extends Enum<?>> void visitCtEnum(CtEnum<T> ctEnum) {
        printTypeStructure(ctEnum);
    }

    @Override
    public <T> void visitCtInterface(CtInterface<T> intrface) {
        printTypeStructure(intrface);
    }

    @Override
    public void visitCtRecord(CtRecord recordType) {
        printTypeStructure(recordType);
    }

    private TokenWriter printOriginalFragment(SourcePosition pos) {
        return printOriginalFragment(pos.getSourceStart(), pos.getSourceEnd());
    }

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

    private void printTypeStructure(CtType<?> type) {
        getPrinterTokenWriter().writeln();
        SourcePosition typePosition = type.getPosition();

        List<CtTypeMember> explicitTypeMembers = type.getTypeMembers().stream()
                // Spoon creates implicit constructors which don't exist in the source code
                .filter(typeMember -> !typeMember.isImplicit())
                .toList();

        if (explicitTypeMembers.isEmpty()) {
            // If no nested elements, then print the original source fragment entirely
            printOriginalFragment(typePosition).writeln();
            return;
        }

        // Find the minimal nested element start position
        int minMemberStart = explicitTypeMembers.stream()
                .mapToInt(typeMember -> typeMember.getPosition().getSourceStart())
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

            // Copy class member code from the original code without changes
            int nextElementStart = explicitTypeMembers.stream()
                    .mapToInt(nextMember -> nextMember.getPosition().getSourceStart())
                    .filter(start -> start > member.getPosition().getSourceEnd())
                    .min()
                    .orElse(member.getPosition().getSourceEnd() + 1);
            printOriginalFragment(member.getPosition().getSourceStart(), nextElementStart - 1);
        }

        // Print the type footer from the end of the bottommost nested element until end of the type fragment
        int maxMemberEnd = explicitTypeMembers.stream()
                .mapToInt(typeMember -> typeMember.getPosition().getSourceEnd())
                .max()
                .orElseThrow(IllegalStateException::new);

        // TODO Check trailing comments
        printOriginalFragment(maxMemberEnd + 1, typePosition.getSourceEnd());
        // TODO Check trailing indents
    }

    @Override
    public void visitCtCompilationUnit(CtCompilationUnit compilationUnit) {
        if (compilationUnit.getUnitType() != UNIT_TYPE.TYPE_DECLARATION) {
            super.visitCtCompilationUnit(compilationUnit);
        }
        CtCompilationUnit outerCompilationUnit = this.sourceCompilationUnit;
        try {
            this.sourceCompilationUnit = compilationUnit;
            int firstTypeStart = compilationUnit.getDeclaredTypes().stream()
                    .map(CtElement::getPosition)
                    .mapToInt(SourcePosition::getSourceStart)
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
}
