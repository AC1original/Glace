package com.snac.graphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public abstract class Canvas {
    protected final List<Renderable> renderables = Collections.synchronizedList(new ArrayList<>());

    public void addRenderable(final Renderable renderable) {
        renderables.add(renderable);
        sortRenderables();
    }

    public void removeRenderable(final Renderable renderable) {
        renderables.remove(renderable);
        sortRenderables();
    }

    public List<Renderable> getRenderables() {
        return List.copyOf(renderables);
    }

    public Stream<Renderable> streamRenderables() {
        synchronized (renderables) {
            return new ArrayList<>(renderables).stream();
        }
    }

    public void clearRenderables() {
        renderables.clear();
    }

    public void sortRenderables() {
        synchronized (renderables) {
            renderables.sort((a, b) -> {
                boolean aHasLayer = a.layer() >= 0;
                boolean bHasLayer = b.layer() >= 0;

                if (aHasLayer && bHasLayer) {
                    return Integer.compare(a.layer(), b.layer());
                } else if (!aHasLayer && !bHasLayer) {
                    return a.priority().compareTo(b.priority());
                } else {
                    return aHasLayer ? -1 : 1;
                }
            });
        }
    }


    public synchronized void render(Brush<?> brush) {
        renderables.forEach(renderable -> renderable.render(brush));
    }

    public static class DefaultCanvas extends Canvas {}
}
