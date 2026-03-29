package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupRuleLine;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Predicate;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MemberGroupRuleLineCompilerTestAccess {

    private static final String COMPILE_RULE_LINE_METHOD_NAME = "compileRuleLine";
    private static final Method COMPILE_RULE_LINE_METHOD = resolveCompileRuleLineMethod();

    /**
     * Invokes the private production compiler entrypoint for a single rule line.
     *
     * @param unifiedMemberGroupRuleLine the rule line to compile
     * @return compiled descriptor predicate
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static Predicate<MemberDescriptor> invokeCompileRuleLine(
            @NonNull UnifiedMemberGroupRuleLine unifiedMemberGroupRuleLine) {
        try {
            return (Predicate<MemberDescriptor>) COMPILE_RULE_LINE_METHOD.invoke(null, unifiedMemberGroupRuleLine);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Cannot access %s via reflection".formatted(COMPILE_RULE_LINE_METHOD_NAME), e);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            throw new IllegalStateException(
                    "Failed to invoke %s via reflection".formatted(COMPILE_RULE_LINE_METHOD_NAME), targetException);
        }
    }

    @NonNull
    private static Method resolveCompileRuleLineMethod() {
        try {
            Method compileRuleLineMethod = MemberGroupRuleLineCompiler.class.getDeclaredMethod(
                    COMPILE_RULE_LINE_METHOD_NAME, UnifiedMemberGroupRuleLine.class);
            compileRuleLineMethod.setAccessible(true);
            return compileRuleLineMethod;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Cannot resolve %s on %s"
                            .formatted(
                                    COMPILE_RULE_LINE_METHOD_NAME,
                                    MemberGroupRuleLineCompiler.class.getSimpleName()),
                    e);
        }
    }
}
