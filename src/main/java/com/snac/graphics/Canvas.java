package com.snac.graphics;

import java.util.ArrayList;
import java.util.List;

public abstract class Canvas {
    private final List<Renderable> drawables = new ArrayList<>();

    public void addDrawable(Renderable drawable) {
        drawables.add(drawable);
    }

    public void removeDrawable(Renderable drawable) {
        drawables.remove(drawable);
    }

    public void clearDrawables() {
        drawables.clear();
    }

    public List<Renderable> getDrawables() {
        return List.copyOf(drawables);
    }

    protected void render() {

    }
}
