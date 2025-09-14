package com.snac.core.gameobject;

import com.snac.graphics.Brush;
import com.snac.graphics.Renderable;
import com.snac.util.HitBox;
import com.snac.util.Vector2D;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Base class for renderable and updatable game objects.
 * <p>
 * Subclasses are expected to provide concrete behavior by implementing the lifecycle methods.
 *
 * <p><strong>Note</strong></p>
 * <ul>
 *   <li>{@code internalCreate(manager)} is invoked by the framework to register the object and then call {@link #onCreate()}.</li>
 *   <li>{@code internalUpdate(deltaTime)} is invoked by the framework every tick and delegates to {@link #onUpdate(double)}.</li>
 * </ul>
 * <p>
 * To provide functionality for your game objects, you need to add them to a valid {@link GameObjectManager} instance.
 *
 * <p>
 * <b>Note:</b> By removing unused objects via {@link GameObjectManager#destroyGameObject(AbstractObjectBase)} you can save resources.
 * To implement such features you can use a cache (like {@link com.snac.data.runtime.caching.Cache Cache} with {@link com.snac.data.runtime.caching.CacheListener CacheListener} for custom functionality)
 * </p>
 *
 * @param <I> Type of the visual asset associated with this object (e.g., image or sprite handle).
 */
@Slf4j
@Getter
public abstract class AbstractObjectBase<I> implements Renderable<I>, Serializable {

    /**
     * Maximum distance from the center of the window (created by the
     * {@link com.snac.graphics.Renderer Renderer}) at which this object still gets updated.
     * <p>
     * If the distance is greater than this value, {@link #disabled} is set to {@code true}
     * and {@link #onUpdate(double)} won't be called anymore.
     * Moving the object back within range re-enables updates.
     * </p>
     * <p>
     * Default value: {@code (screen size / 2) + object width + 200}.
     * See also {@link #internalCreate(GameObjectManager)} and {@link #onPositionChange(double, double)}.
     * </p>
     * <p>
     * Setting this to {@code 0} or lower disables the check, meaning the object
     * always gets updated regardless of distance.
     * </p>
     */
    protected int tickDistance;

    /**
     * Similar to {@link #tickDistance}, but only affects rendering.
     * <p>
     * If the distance is exceeded, {@link #visible} is set to {@code false}.
     * {@link #disabled} is not affected.
     * </p>
     * <p>
     * Setting this to {@code 0} or lower disables the check, meaning the object
     * is always rendered regardless of distance.
     * </p>
     */
    protected int renderDistance;

    /**
     * Whether this object is updated or not.
     * If {@code true}, {@link #onUpdate(double)} will no longer be called.
     * <p>
     * See also {@link #tickDistance}, {@link #internalUpdate(double)} and
     * {@link #onPositionChange(double, double)}.
     * </p>
     */
    protected boolean disabled;

    /**
     * Whether this object is rendered or not.
     * If {@code false}, it won't be drawn.
     * <p>
     * See also {@link Renderable#visible()}, {@link #renderDistance} and
     * {@link #onPositionChange(double, double)}.
     * </p>
     */
    @Setter
    @Getter(AccessLevel.NONE)
    protected boolean visible;

    /**
     * Set of objects attached to this object.
     */
    @Getter(AccessLevel.NONE)
    protected final Set<AbstractObjectBase<I>> attachments;

    /**
     * Object this object is attached to, or {@code null} if not attached.
     */
    @Nullable
    protected AbstractObjectBase<I> attachesTo;

    /**
     * Position of the object relative to the window the object is rendered on
     * (Also see the {@link com.snac.graphics.Renderer Renderer} you are using
     * ({@link com.snac.graphics.impl.SwingRenderer SwingRenderer} by default if you're using the default
     * {@link com.snac.core.Glace Glace} configurations)).<br>
     * Never {@code null}; defaults to (0,0) if not provided.
     */
    protected final Vector2D position;

    /**
     * Facing or movement direction vector.
     * Never {@code null}; defaults to (1,0) if not provided.
     */
    protected final Vector2D direction;

    /**
     * Object width in pixels.
     */
    protected volatile int width;

    /**
     * Object height in pixels.
     */
    protected volatile int height;

    /**
     * Axis-aligned bounding box used for collision or spatial queries,
     * initialized from the rounded position and current size.
     */
    protected final HitBox hitBox;

    /**
     * Wall-clock timestamp (milliseconds since epoch) at which this instance was created.
     */
    private final long timeCreated;

    /**
     * Unique identifier assigned to this instance.
     */
    private final UUID uuid;

    /**
     * Optional visual asset bound to this object.
     * Marked {@code volatile} to ensure
     * visibility across threads if updated outside the main loop.
     * May be {@code null}.
     */
    @Setter
    private volatile I image;

    /**
     * Manager responsible for the object's lifecycle and orchestration.
     * May be {@code null} this object gets initialized with a {@link GameObjectManager}
     */
    @Nullable
    private GameObjectManager<I> manager;

    /**
     * Creates an object with a default position (0|0), default direction (1|0),
     * and minimum dimensions (20x20).
     * <p>
     * Delegates to {@link #AbstractObjectBase(Vector2D, Vector2D, int, int)}.
     */
    protected AbstractObjectBase() {
        this(null, null, 0, 0);
    }

    /**
     * Creates an object with the provided spatial parameters.
     * The hit box is initialized from the rounded coordinates and the resolved size,
     * and identity fields ({@code timeCreated}, {@code uuid}) are assigned.
     *
     * @param position  initial world position, or {@code null} for (0,0)
     * @param direction initial direction vector, or {@code null} for (1,0)
     * @param width     desired width in pixels; values < 1 resolve to 20
     * @param height    desired height in pixels; values < 1 resolve to 20
     */
    protected AbstractObjectBase(@Nullable Vector2D position, @Nullable Vector2D direction, int width, int height) {
        this.position = new Vector2D(position == null ? new Vector2D(0, 0) : position) {
            @Override
            public void set(double x, double y) {
                onPositionChange(x, y);
                super.set(x, y);
            }
        };
        this.direction = new Vector2D(direction == null ? new Vector2D(1, 0) : direction) {
            @Override
            public void set(double x, double y) {
                onDirectionChange(x, y);
                super.set(x, y);
            }
        };
        this.disabled = false;
        this.visible = true;
        this.attachments = Collections.synchronizedSet(Set.of());
        this.width = width < 1 ? 20 : width;
        this.height = height < 1 ? 20 : height;
        this.hitBox = new HitBox(this.position.getXRound(), this.position.getYRound(), getWidth(), getHeight());
        this.timeCreated = System.currentTimeMillis();
        this.uuid = UUID.randomUUID();
    }

    /**
     * Callback method invoked whenever the position of this object changes.<br>
     * Used to implement logic for {@link #renderDistance} and {@link #tickDistance}
     * <p>
     * Subclasses can override this method to perform custom logic or trigger
     */
    protected void onPositionChange(double newX, double newY) {
        updateAttachments(position.getX(), position.getY(), newX, newY);
        if (manager == null || manager.getRenderer().getWindowWidth() == Integer.MAX_VALUE) {
            return;
        }
        var renderer =  manager.getRenderer();
        var distance = Math.max(Math.abs(this.position.getXRound() - renderer.getWindowWidth() / 2),
                Math.abs(this.position.getYRound() - renderer.getWindowHeight() / 2));

        if (renderDistance > 0) {
            visible = distance < renderDistance;
        }
        if (tickDistance > 0) {
            disabled = distance > tickDistance;
        }
    }

    /**
     * Callback method invoked whenever the direction of this object changes.
     * <p>
     * Subclasses can override this method to perform custom logic or trigger
     */
    protected void onDirectionChange(double newX, double newY) {
    }

    /**
     * Renders this object with the given brush.
     *
     * @param brush drawing context provided by the renderer
     */
    public abstract void onRender(Brush<?> brush);

    /**
     * Update hook invoked by the framework.
     * Implementations should advance the object's state according to the elapsed time
     * and game logic.
     * Avoid blocking operations.
     *
     * @param deltaTime time elapsed since the previous update
     */
    protected abstract void onUpdate(double deltaTime);

    /**
     * One-time initialization hook invoked after the object has been added to a valid {@link GameObjectManager}.
     */
    abstract void onCreate();

    /**
     * Cleanup hook invoked when the object is being destroyed.
     */
    abstract void onDestroy();

    /**
     * Framework-internal update entry point; delegates to {@link #onUpdate(double)}.
     * Not intended to be overridden by subclasses.
     *
     * @param deltaTime time elapsed since the previous update
     */
    void internalUpdate(double deltaTime) {
        if (disabled) return;
        onUpdate(deltaTime);
    }

    /**
     * Framework-internal creation entry point; sets the manager and calls {@link #onCreate()}.
     * Not intended to be called directly by subclasses.
     *
     * @param gameObjectManager the manager responsible for this object
     */
    void internalCreate(GameObjectManager<I> gameObjectManager) {
        this.manager = gameObjectManager;
        this.tickDistance = (Toolkit.getDefaultToolkit().getScreenSize().width / 2) + getWidth() + 200;
        this.renderDistance = tickDistance;
        this.onCreate();
    }

    /**
     * Getter for the hitbox of this object.
     *
     * @return the hitbox updated with the current position and size of this object
     */
    public HitBox getHitBox() {
        hitBox.setX(position.getXRound());
        hitBox.setY(position.getYRound());
        hitBox.setWidth(getWidth());
        hitBox.setHeight(getHeight());

        return hitBox;
    }

    /**
     * Updates the positions of all attached objects based on the movement of this object.
     * <br>
     * In other words, if this object moves, all its attachments get shifted by the same offset.
     *
     * <p>
     * This method is called automatically by the framework whenever the object moves.
     * You can override it in subclasses, though there's usually no good reason to.
     * </p>
     *
     * @param oldX the old X position of this object
     * @param oldY the old Y position of this object
     * @param newX the new X position of this object
     * @param newY the new Y position of this object
     */
    public void updateAttachments(double oldX, double oldY, double newX, double newY) {
        synchronized (attachments) {
            attachments.forEach(attachment -> {
                attachment.position.set(
                        attachment.position.getX() - oldX + newX,
                        attachment.position.getY() - oldY + newY
                );
            });
        }
    }

    /**
     * Returns the attachments of this object.
     *
     * @return an unmodifiable copy of the current attachments set
     */
    public Set<AbstractObjectBase<I>> getAttachments() {
        return Set.copyOf(attachments);
    }

    /**
     * Attaches the given object to this object.
     * <p>
     * If the provided object is already attached to another object, no attachment will be made
     * and a warning will be logged.
     * </p>
     * <p>
     * <b>Note:</b> If the object is already attached to another object, you can obtain
     * that object via {@link #getAttachesTo()} and detach it using
     * {@link #detach(AbstractObjectBase)} before attempting to attach it again.
     * </p>
     *
     * @param object the object to attach; must not already be attached to another object
     */
    public void attach(AbstractObjectBase<I> object) {
        if (object.attachesTo != null) {
            log.warn("Object {} is already attached to {}. It must be detached first.",
                    object.getClass().getSimpleName(),
                    object.attachesTo.getClass().getSimpleName());
            return;
        }
        attachments.add(object);
        object.attachesTo = this;
    }

    /**
     * Detaches the specified object from this object.
     * <p>
     * This removes the object from this object's attachment set and clears
     * its {@code attachesTo} reference.
     * </p>
     *
     * @param object the object to be detached; must currently be attached to this object
     */
    public void detach(AbstractObjectBase<I> object) {
        attachments.remove(object);
        object.attachesTo = null;
    }

    @Override
    public boolean visible() {
        return visible;
    }
}