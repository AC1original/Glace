package com.snac.core.gameobject;

import com.snac.graphics.Brush;
import com.snac.graphics.Renderable;
import com.snac.util.HitBox;
import com.snac.util.Vector2D;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.UUID;

/**
 * TODO: Attach other objects
 */
@Getter
public abstract class AbstractObjectBase<I> implements Renderable, Serializable {
    protected final Vector2D position;
    protected final Vector2D direction;
    protected int width;
    protected int height;
    protected final HitBox hitBox;
    private final long timeCreated;
    private final UUID uuid;
    @Nullable private GameObjectManager manager;

    protected AbstractObjectBase() {
        this(null, null, 0, 0);
    }

    protected AbstractObjectBase(@Nullable Vector2D position, @Nullable Vector2D direction, int width, int height) {
        this.position = position == null ? new Vector2D(0, 0) : position;
        this.direction = direction == null ? new Vector2D(1, 0) : direction;
        this.width = width < 1 ? 20 : width;
        this.height = height < 1 ? 20 : height;
        this.hitBox = new HitBox(getXRound(), getYRound(), getWidth(), getHeight());
        this.timeCreated = System.currentTimeMillis();
        this.uuid = UUID.randomUUID();
    }

    public abstract I getImage();
    public abstract void setImage(I image);
    public abstract void onRender(Brush<?, ?> brush);
    protected abstract void onUpdate(double deltaTime);
    abstract void onCreate();
    abstract void onDestroy();

    void internalUpdate(double deltaTime) {
        onUpdate(deltaTime);
    }

    void internalCreate(GameObjectManager gameObjectManager) {
        this.manager = gameObjectManager;
        this.onCreate();
    }

    public int getXRound() {
        return Math.toIntExact(Math.round(position.getX()));
    }

    public int getYRound() {
        return Math.toIntExact(Math.round(position.getY()));
    }
}
