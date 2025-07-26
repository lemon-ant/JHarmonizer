package io.github.antonlem.jharmonizer.config;

public enum TypeKind {
    class_,
    interface_,
    enum_,
    annotation,
    record_;

    public static TypeKind fromRaw(String raw) {
        return switch (raw.replace("-", "_")) {
            case "class" -> class_;
            case "interface" -> interface_;
            case "enum" -> enum_;
            case "annotation" -> annotation;
            case "record" -> record_;
            default -> throw new IllegalArgumentException("Unsupported type: " + raw);
        };
    }
}
