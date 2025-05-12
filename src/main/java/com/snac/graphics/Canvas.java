package com.snac.graphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public abstract class Canvas {
    protected final List<Renderable> renderables = Collections.synchronizedList(new ArrayList<>());
    protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();

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
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(renderables).stream();
        } finally {
            rwLock.readLock().unlock();
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


    public void render(Brush<?, ?> brush) {
        rwLock.readLock().lock();
        try {
            var it = renderables.iterator();
            while (it.hasNext()) {
                var i = it.next();
                if (i.visible()) {
                    i.render(brush);
                }
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public static class DefaultCanvas extends Canvas {}
}
