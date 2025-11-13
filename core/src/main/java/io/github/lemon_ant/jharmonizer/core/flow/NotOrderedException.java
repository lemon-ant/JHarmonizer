package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.spoon.RelocationDetector.printRelocations;
import static java.util.Collections.unmodifiableCollection;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serial;
import java.nio.file.Path;
import java.util.Collection;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtElement;

@Getter
@SuppressFBWarnings("EI_EXPOSE_REP")
public class NotOrderedException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1385695423360072326L;

    private final Path offendingFile;
    private final Collection<Pair<CtElement, Integer>> relocations;

    public NotOrderedException(@Nonnull Path offendingFile, @Nonnull Collection<Pair<CtElement, Integer>> relocations) {
        this.offendingFile = offendingFile;
        this.relocations = unmodifiableCollection(relocations);
    }

    @Override
    public String getMessage() {
        return printRelocations(offendingFile, relocations);
    }
}
