package com.snac.graphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * Canvas instances can be added to any {@link Renderer}.
 * Canvas can get switched/changed while the {@link Renderer} is rendering.
 * This means you can have different Canvas instances used for different things.
 * For example, one for the game itself,
 * one for the game-over screen and one for the settings menu and switch to the one currently needed.
 * <p>
 *     The {@link Renderer} will render every {@link Renderable} added to its current Canvas.
 * </p>
 * Also see {@link Renderer} and {@link Renderable} for more information.
 */
public class Canvas {
    protected final List<Renderable> renderables = Collections.synchronizedList(new ArrayList<>());
    protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * Adds a new {@link Renderable} to Canvas.
     * @param renderable The renderable you want to add
     */
    public void addRenderable(final Renderable renderable) {
        renderables.add(renderable);
        sortRenderables();
    }

    /**
     * Removes a {@link Renderable} from Canvas
     * @param renderable The renderable you want to remove
     */
    public void removeRenderable(final Renderable renderable) {
        renderables.remove(renderable);
        sortRenderables();
    }

    /**
     * For thread safety and clarity purposes, this method only returns a copy of the renderables-list.
     * @return A copy of the renderables-list from the canvas
     */
    public List<Renderable> getRenderables() {
        rwLock.readLock().lock();
        try {
            return List.copyOf(renderables);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * @return The renderables used by the Canvas as stream
     */
    public Stream<Renderable> streamRenderables() {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(renderables).stream();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Deletes every renderable from Canvas
     */
    public void clearRenderables() {
        renderables.clear();
    }

    /**
     * Sorts the renderables
     * related to their {@link com.snac.graphics.Renderable.Priority Priority} and {@link Renderable#layer() layer}.
     * Only called when a drawable is added or removed.
     * <p>IDK what exactly I did here, but it somehow works - at least I think so.</p>
     */
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

    /**
     * Renders every {@link Renderable}
     * @param brush The brush which is passed on to every renderable
     */
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
