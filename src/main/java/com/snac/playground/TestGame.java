package com.snac.playground;

import com.snac.graphics.Brush;
import com.snac.graphics.Renderable;
import com.snac.graphics.impl.SkijaRenderer;

import java.awt.*;

public class TestGame {
    public static void main(String[] args) {
        var renderer = new SkijaRenderer();

        renderer.createWindow(800, 600, "Test Game");
        renderer.getCanvas().addRenderable(new Renderable() {
            @Override
            public void render(Brush<?, ?> brush) {
                brush.setColor(Color.BLUE);
                brush.drawRectangle(0, 0, 20, 20, true);
            }

            @Override
            public boolean visible() {
                return false;
            }

            @Override
            public Priority priority() {
                return null;
            }
        });
    }
}
