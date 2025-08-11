package com.snac.core.gameobject;

import com.snac.graphics.Brush;
import com.snac.graphics.Renderable;
import com.snac.util.HitBox;
import com.snac.util.Vector2D;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
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
 *
 * To provide functionality for your game objects, you need to add them to a valid {@link GameObjectManager} instance.
 *
 * @param <I> Type of the visual asset associated with this object (e.g., image or sprite handle).
 */
//TODO: New feature. Attach other objects to this object.
@Getter
public abstract class AbstractObjectBase<I> implements Renderable, Serializable {

    /**
     * World position of the object in continuous coordinates.
     * Never {@code null}; defaults to (0,0) if not provided.
     */
    protected final Vector2D position;

    /**
     * Facing or movement direction vector.
     * Never {@code null}; defaults to (1,0) if not provided.
     */
    protected final Vector2D direction;

    /**
     * Object width in pixels. A minimum of 20 is enforced when constructed.
     */
    protected int width;

    /**
     * Object height in pixels. A minimum of 20 is enforced when constructed.
     */
    protected int height;

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
    private GameObjectManager manager;

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
        this.position = position == null ? new Vector2D(0, 0) : position;
        this.direction = direction == null ? new Vector2D(1, 0) : direction;
        this.width = width < 1 ? 20 : width;
        this.height = height < 1 ? 20 : height;
        this.hitBox = new HitBox(getXRound(), getYRound(), getWidth(), getHeight());
        this.timeCreated = System.currentTimeMillis();
        this.uuid = UUID.randomUUID();
    }

    /**
     * Renders this object with the given brush.
     *
     * @param brush drawing context provided by the renderer
     */
    public abstract void onRender(Brush<?, ?> brush);

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
        onUpdate(deltaTime);
    }

    /**
     * Framework-internal creation entry point; sets the manager and calls {@link #onCreate()}.
     * Not intended to be called directly by subclasses.
     *
     * @param gameObjectManager the manager responsible for this object
     */
    void internalCreate(GameObjectManager gameObjectManager) {
        this.manager = gameObjectManager;
        this.onCreate();
    }

    /**
     * Returns the rounded X coordinate of {@link #position}.
     *
     * @return rounded X (integer)
     */
    public int getXRound() {
        return Math.toIntExact(Math.round(position.getX()));
    }

    /**
     * Returns the rounded Y coordinate of {@link #position}.
     *
     * @return rounded Y (integer)
     */
    public int getYRound() {
        return Math.toIntExact(Math.round(position.getY()));
    }

    /**
     * Getter for the hitbox of this object.
     * @return the hitbox updated with the current position and size of this object
     */
    public HitBox getHitBox() {
        hitBox.setX(getXRound());
        hitBox.setY(getYRound());
        hitBox.setWidth(getWidth());
        hitBox.setHeight(getHeight());

        return hitBox;
    }
}