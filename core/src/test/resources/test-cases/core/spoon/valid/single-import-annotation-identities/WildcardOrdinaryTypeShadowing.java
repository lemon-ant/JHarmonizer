// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package annotationimportidentities;

import static annotationimportidentities.WildcardOrdinaryTypeShadowing.HiddenOwner.*;
import static annotationimportidentities.WildcardOrdinaryTypeShadowing.VisibleOwner.*;

class WildcardOrdinaryTypeShadowing {
    static class HiddenOwner {
        private static class Tag {}
    }

    static class VisibleOwner {
        @interface Tag {}
    }

    @Tag
    int value;
}
