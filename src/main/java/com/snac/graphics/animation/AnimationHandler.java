package com.snac.graphics.animation;

import com.snac.graphics.Canvas;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class AnimationHandler {
    private final List<Animation> animations;
    @Getter
    @Setter
    private Canvas canvas;

    public AnimationHandler(Canvas canvas) {
        this.animations = Collections.synchronizedList(new ArrayList<>());
        this.canvas = canvas;

        log.info("Initialized");
    }

    public void play(Animation animation) {
        animations.add(animation);
    }

    public List<Animation> getAnimations() {
        return List.copyOf(animations);
    }
}
