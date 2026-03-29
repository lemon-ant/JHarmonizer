class FieldInitializerExplicitThisForwardReferenceMethodReferenceFixture {

    private final int alpha = this.bravo;

    private final int bravo = java.util.Optional.<java.util.function.Supplier<Integer>>of(() -> 0)
            .map(java.util.function.Supplier::get)
            .orElse(0);
}
