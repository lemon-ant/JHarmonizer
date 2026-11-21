// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package annotationimportidentities;

import java.util.Map.Entry;

class ImportedOrdinaryTypeShadowing {
    static class Base {
        @interface Entry {}
    }

    void useLocal() {
        class Local extends Base {
            @Entry
            int value;
        }
    }
}
