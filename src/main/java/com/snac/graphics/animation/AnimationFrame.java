package com.snac.graphics.animation;

import com.snac.util.Vector2D;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public final class AnimationFrame<I> {
    private static final AnimationFrame<?>[] empty;
    private I image;
    private int width;
    private int height;
    private int delay = -1;
    @Nullable private Vector2D location;

    private AnimationFrame(I image, int width, int height) {
        this.image = image;
        this.width = width;
        this.height = height;
    }

    public static AnimationFrame<?>[] createEmpty() {
        return empty;
    }

    public static<I> AnimationFrame<I> create(I image, int width, int height) {
        return new AnimationFrame<>(image, width, height);
    }

    static {
        empty = new AnimationFrame[1];
    }
}
