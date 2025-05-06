package com.snac.main;

import com.snac.graphics.impl.DefaultRenderer;

public class Glace {
    public static void main(String[] args) {
        new DefaultRenderer();
    }

    private static Glace instance;

    private Glace() {}

    public static Glace setup() {
        if (instance == null) {
            instance = new Glace();
        }
        return instance;
    }
}
