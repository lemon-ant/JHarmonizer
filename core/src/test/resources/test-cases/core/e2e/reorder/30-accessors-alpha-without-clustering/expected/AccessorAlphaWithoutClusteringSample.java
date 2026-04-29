// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

public class AccessorAlphaWithoutClusteringSample {
    private String clientId;
    private boolean disconnectedNodeAcknowledged;
    private String processorId;

    public String getClientId() {
        return clientId;
    }

    public String getProcessorId() {
        return processorId;
    }

    public boolean isDisconnectedNodeAcknowledged() {
        return disconnectedNodeAcknowledged;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setDisconnectedNodeAcknowledged(boolean disconnectedNodeAcknowledged) {
        this.disconnectedNodeAcknowledged = disconnectedNodeAcknowledged;
    }

    public void setProcessorId(String processorId) {
        this.processorId = processorId;
    }
}
