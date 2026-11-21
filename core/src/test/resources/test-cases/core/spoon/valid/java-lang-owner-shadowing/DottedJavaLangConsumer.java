// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package annotationordinaryshadow;

public class DottedJavaLangConsumer {
    @Thread.Tag
    @java.lang.Thread.Tag
    @java.lang.Deprecated
    int value;

    java.lang.Thread.State state = java.lang.Thread.State.LOCAL;
}
