package com.snac.core.gameobject;

import com.snac.util.Vector2D;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public abstract class PhysicalObject<I> extends AbstractObjectBase<I> {
    protected final Vector2D velocity;

    protected PhysicalObject() {
        this(null, Direction.RIGHT.getAngle(), 0, 0);
    }

    protected PhysicalObject(@Nullable Vector2D position, float direction, int width, int height) {
        super(position, direction, width, height);

        this.velocity = new Vector2D(0, 0);
    }

    public void onCollide(List<AbstractObjectBase<I>> collidedObjects) {}

    @Override
    void internalUpdate(double deltaTime) {
        super.internalUpdate(deltaTime);

        checkCollisions();
    }

    protected void checkCollisions() {
        var manager = getManager();
        if (manager == null || !manager.collides(this)) {
            return;
        }

        var collisions = manager.getCollisions(this);

        onCollide(collisions);
    }
}
