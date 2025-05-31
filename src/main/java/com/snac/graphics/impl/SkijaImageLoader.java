package com.snac.graphics.impl;

import com.snac.graphics.ImageLoader;
import io.github.humbleui.skija.Image;

public class SkijaImageLoader extends ImageLoader<Image> {

    @Override
    public Image load(String path) {
        return null;
    }

    @Override
    public Image getFallback() {
        return null;
    }
}
