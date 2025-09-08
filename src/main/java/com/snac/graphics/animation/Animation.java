package com.snac.graphics.animation;

import com.snac.graphics.Brush;
import com.snac.graphics.Renderable;
import com.snac.util.Vector2D;
import lombok.Getter;

@Getter
public abstract class Animation<I, F> implements Renderable<I, F> {
    protected int counter = 0;
    protected int index = 0;
    protected boolean paused = false;
    protected final Vector2D location = new Vector2D(0, 0);
    protected long lastFrameChange = 0;

    public abstract AnimationFrame<I>[] getFrames();
    public abstract int getDelay();
    public abstract boolean drawAnimation();
    public abstract void onFrameChange(AnimationFrame<?> frame);

    protected void onPlay() {}
    protected void onStop() {}
    protected void onPause() {}

    public boolean checkValidation() {
        return getFrames().length > 0;
    }

    protected void updateIndex() {
        if (checkValidation() && !isPaused()) {
            var frame = getFrames()[index];
            var delay = frame.getDelay() > 0 ? frame.getDelay() : getDelay();

            if (getLastFrameChange() <= 0 || System.currentTimeMillis() - getLastFrameChange() >= delay) {
                index = index < getFrames().length ? index + 1 : 0;
                onFrameChange(getFrames()[index]);
            }
        }
    }

    public void jumpTo(int index) {
        if (index >= 0 && index < getFrames().length) {
            this.index = index;
            onFrameChange(getFrames()[index]);
        }
    }

    public void reset() {
        index = 0;
        onFrameChange(getFrames()[index]);
    }

    public Vector2D getLocation(AnimationFrame<?> frame) {
        return frame.getLocation() == null ? location : frame.getLocation();
    }

    @Override
    public void render(Brush<I, F> brush) {
        if (!drawAnimation()) return;

        var frame = getFrames()[index];
        var location = getLocation(frame);

        brush.drawImage(frame.getImage(), location.getXRound(), location.getYRound(), frame.getWidth(), frame.getHeight());
    }

    public void pause() {
        this.paused = true;
        onPause();
    }
}
