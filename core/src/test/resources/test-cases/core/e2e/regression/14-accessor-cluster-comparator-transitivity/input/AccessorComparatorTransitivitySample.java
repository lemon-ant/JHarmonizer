// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

@SuppressWarnings("unused")
public class AccessorComparatorTransitivitySample {

    private String apple;
    private String bravo;
    private String charlie;
    private String xray;
    private String yankee;
    private String zebra;

    public String getZebra() {
        return zebra;
    }

    public void setApple(String apple) {
        this.apple = apple;
    }

    public String getYankee() {
        return yankee;
    }

    public void middle() {
        // non-accessor method whose alphaKey ("middle():void") falls between the
        // accessors' alphaKeys; this is the third element of the inconsistent
        // comparator triple together with any get* / set* unpaired accessor.
    }

    public void setBravo(String bravo) {
        this.bravo = bravo;
    }

    public String getXray() {
        return xray;
    }

    public void setCharlie(String charlie) {
        this.charlie = charlie;
    }
}
