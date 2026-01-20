package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;

/**
 * Build element→sequentialIndex (0..N-1) for the CURRENT model order.
 */
@Slf4j
@UtilityClass
class ElementsFlatOrderIndexer {

    /**
     * Main entry: map of CtElement to its current encounter index.
     */
    // TODO Create a dedicated type instead of Map
    static Map<SourcePosition, Integer> indexElementsByOrder(CtCompilationUnit compilationUnit) {
        AtomicInteger runningIndex = new AtomicInteger(0);
        return SpoonTypeUtils.streamDeclaredHierarchy(compilationUnit)
                .map(CtElement::getPosition)
                .filter(SourcePosition::isValidPosition)
                .collect(Collectors.toMap(
                        Function.identity(), // key: the element itself
                        e -> runningIndex.getAndIncrement() // value: the element's sequential index
                        ));
    }
}
