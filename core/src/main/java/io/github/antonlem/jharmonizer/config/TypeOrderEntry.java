package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import lombok.Value;

/**
 * A single line in type-order: either a string or list of strings.
 * Supports both: "- enum" and "- [class, record]"
 */
@Value
@JsonDeserialize(using = TypeOrderEntryDeserializer.class)
public class TypeOrderEntry {
    @SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
    List<TypeKind> kinds;

    public enum TypeKind {
        public_class, // mapped from "public-class"
        class_,
        interface_,
        enum_,
        record_;

        public static TypeKind fromRaw(String raw) {
            return switch (raw.replace("-", "_")) {
                case "public_class" -> public_class;
                case "class" -> class_;
                case "interface" -> interface_;
                case "enum" -> enum_;
                case "record" -> record_;
                default -> throw new IllegalArgumentException("Unsupported type: " + raw);
            };
        }
    }
}
