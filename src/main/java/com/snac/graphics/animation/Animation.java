package com.snac.graphics.animation;

import com.snac.graphics.Brush;
import com.snac.graphics.Renderable;
import com.snac.util.Vector2D;
import lombok.Getter;

/**
 * Base class for creating animations.<br>
 * <p>
 * Animation instances must be registered in an {@link AnimationHandler} to be updated and rendered.
 * </p>
 *
 * @param <I> The image type of the animation frames. This must match the used {@link AnimationHandler} (and {@link com.snac.graphics.Renderer renderer}).
 */
@Getter
public abstract class Animation<I> implements Renderable<I> {
    protected int counter = 0;
    protected int index = 0;
    protected boolean paused = false;
    protected final Vector2D location = new Vector2D(0, 0);
    protected long lastFrameChange = 0;

    /**
     * Returns all frames of this animation.
     * <p>
     * <b>Important:</b> Do not create a new array on every call, as this would be inefficient.
     * Instead, initialize the array once (e.g. in the constructor) and return the same reference.
     * </p>
     *
     * @return the animation frames
     */
    public abstract AnimationFrame<I>[] getFrames();

    /**
     * Returns the default delay between frames in milliseconds.
     * <p>
     * A frame can override this value with its own delay.
     * </p>
     *
     * @return the frame delay in milliseconds
     */
    public abstract int getDelay();

    /**
     * Determines whether this animation should directly draw its frames.
     * <p>
     * If {@code false}, you can still react to frame changes via {@link #onFrameChange(AnimationFrame)}
     * and perform custom rendering logic instead.
     * </p>
     *
     * @return {@code true} if frames should be drawn, otherwise {@code false}
     */
    public abstract boolean drawAnimation();

    /**
     * Called whenever the animation switches to a new frame.
     *
     * @param frame the new frame
     */
    protected void onFrameChange(AnimationFrame<?> frame) {
    }

    /**
     * Called when the animation is started or resumed.
     */
    protected void onPlay() {
    }

    /**
     * Called when the animation is stopped.
     */
    protected void onStop() {
    }

    /**
     * Called when the animation is paused.
     */
    protected void onPause() {
    }

    /**
     * Checks if the animation is valid and can be updated.
     * <p>
     * If this method returns {@code false}, the animation will not update.
     * </p>
     *
     * @return {@code true} if the animation is valid, otherwise {@code false}
     */
    public boolean checkValidation() {
        return true;
    }

    /**
     * Advances the animation index if enough time has passed and
     * calls {@link #onFrameChange(AnimationFrame)} when a new frame is reached.
     */
    protected void updateIndex() {
        if (checkValidation() && !isPaused()) {
            if (getLastFrameChange() <= 0 || System.currentTimeMillis() - getLastFrameChange() >= getDelay()) {
                index = index < getFrames().length - 1 ? index + 1 : 0;
                onFrameChange(getFrames()[index]);
            }
        }
    }

    /**
     * Jumps to a specific frame by index.
     *
     * @param index the frame index to jump to
     */
    public void jumpTo(int index) {
        if (index >= 0 && index < getFrames().length) {
            this.index = index;
            onFrameChange(getFrames()[index]);
        }
    }

    /**
     * Resets the animation to the first frame.
     */
    public void reset() {
        index = 0;
        onFrameChange(getFrames()[index]);
    }

    /**
     * Returns the location of a frame.
     * <p>
     * If the frame has no location set, the default animation location is used.
     * </p>
     *
     * @param frame the frame
     * @return the location of the frame
     */
    public Vector2D getLocation(AnimationFrame<?> frame) {
        return frame.getLocation() == null ? location : frame.getLocation();
    }

    /**
     * Renders the current frame of this animation.
     *
     * @param brush the brush used for rendering
     */
    @Override
    public void render(Brush<I> brush) {
        if (!drawAnimation()) return;

        var frame = getFrames()[index];
        var location = getLocation(frame);

        brush.drawImage(frame.getImage(), location.getXRound(), location.getYRound(),
                frame.getWidth(), frame.getHeight());
    }

    /**
     * Pauses the animation.
     */
    public void pause() {
        this.paused = true;
        onPause();
    }

    /**
     * Resumes the animation.
     */
    public void resume() {
        this.paused = false;
        onPlay();
    }
}