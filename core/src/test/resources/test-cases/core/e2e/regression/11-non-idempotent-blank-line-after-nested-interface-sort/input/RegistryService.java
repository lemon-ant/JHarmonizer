// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import java.io.Closeable;

public interface RegistryService extends Closeable {

    /**
     * @return the token client
     */
    Object getTokenClient();

    //-------------------------------------------------------------------------------------------

    /**
     * @return the bucket reader
     */
    Object getBucketReader();

    /**
     * @return the bucket reader with options
     */
    Object getBucketReader(Object options);

    //-------------------------------------------------------------------------------------------

    /**
     * The builder.
     */
    interface Builder {

        Builder withOptions(Object options);

        RegistryService build();

        Object getOptions();

    }

}
