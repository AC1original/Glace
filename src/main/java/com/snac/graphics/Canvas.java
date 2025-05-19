package com.snac.graphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class Canvas {
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

    protected void sortRenderables() {
        rwLock.writeLock().lock();
        try {
            var updated = new ArrayList<>(renderables);
            updated.sort(Comparator.comparing(Renderable::priority));
            updated.removeIf(r -> r.layer() >= 0);

            renderables.sort(Comparator.comparingInt(Renderable::layer));
            for (var r : renderables) {
                if (r.layer() >= 0) {
                    if (updated.size() > r.layer()) {
                        updated.add(r.layer(), r);
                    } else {
                        updated.add(r);
                    }
                }
            }
            renderables.clear();
            renderables.addAll(updated);
        } finally {
            rwLock.writeLock().unlock();
        }
    }


    public void render(Brush<?, ?> brush) {
        rwLock.readLock().lock();
        try {
            renderables.stream()
                    .filter(Renderable::visible)
                    .forEach(r -> r.render(brush));
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
