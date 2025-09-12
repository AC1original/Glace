package com.snac.graphics.animation;

import com.snac.util.Vector2D;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

/**
 * This class represents a single frame of an animation.
 *
 * @param <I> The type of image this frame contains.
 *            This type must be compatible with the used {@link com.snac.graphics.Renderer Renderer}
 *            (which means it also must match {@link AnimationHandler} and {@link Animation}).
 *            <br> For example, if you're using {@link com.snac.graphics.impl.SwingRenderer SwingRenderer},
 *            this type must be {@link java.awt.image.BufferedImage BufferedImage}.
 */
@Getter
@Setter
public final class AnimationFrame<I> {
    private I image;
    private int width;
    private int height;
    @Nullable
    private Vector2D location;

    /**
     * Create a new AnimationFrame.
     *
     * @param image  the image to use for this frame
     * @param width  the width of the image
     * @param height the height of the image
     */
    public AnimationFrame(I image, int width, int height) {
        this.image = image;
        this.width = width;
        this.height = height;
    }
}
