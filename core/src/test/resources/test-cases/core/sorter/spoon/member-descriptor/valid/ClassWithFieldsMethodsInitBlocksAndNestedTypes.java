package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

public abstract class ClassWithFieldsMethodsInitBlocksAndNestedTypes {

  @Deprecated
  public static final int PUBLIC_STATIC_FINAL_FIELD = 1;

  protected int protectedField;
  int packageField;

  @Deprecated
  private static void privateStaticMethod() { }

  public abstract void abstractMethod();

  public ClassWithFieldsMethodsInitBlocksAndNestedTypes() { }

  static { int staticValue = 1; }
  { int instanceValue = 2; }

  public class PublicNestedClass { }
  protected interface ProtectedNestedInterface { }
  enum NestedEnum { A }
  @interface NestedAnnotation { }
  record NestedRecord(int component) { }
}
