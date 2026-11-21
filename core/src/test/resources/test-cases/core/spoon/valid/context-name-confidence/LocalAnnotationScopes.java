// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package annotationcontextconfidence;

public class LocalAnnotationScopes {
    @Tag int root;

    Object anonymousOwner = new LocalAnnotationBase() {
        @Tag int anonymous;
    };

    void execute() {
        class Unknown extends LocalAnnotationBase {
            @Tag int unavailable;
        }

        class Known extends LocalAnnotationScopeBase {
            @Tag int available;
        }
    }
}

@interface Tag {}

class LocalAnnotationScopeBase {
    @interface Tag {}
}
