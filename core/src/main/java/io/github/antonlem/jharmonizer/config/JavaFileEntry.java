package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class JavaFileEntry {

    @JsonProperty("main-type-first")
    boolean mainTypeFirst;

    @JsonProperty("type-order")
    @SuppressFBWarnings("EI_EXPOSE_REP")
    List<TypeOrderEntry> typeOrder;

    @JsonProperty("sort")
    SortStrategy sort;

    public enum SortStrategy {
        ALPHA,
        PRESERVE
    }
}
