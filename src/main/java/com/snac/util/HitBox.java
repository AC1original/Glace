package com.snac.util;

import com.snac.graphics.Brush;
import com.snac.graphics.Renderable;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;

@Getter
@Setter
public class HitBox implements Renderable {
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean visible = false;
    private final Point location = new Point();

    public HitBox(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean intersects(HitBox hitBox) {
        return intersects(hitBox.getX(), hitBox.getY(), hitBox.getWidth(), hitBox.getHeight());
    }

    public boolean intersects(int x, int y, int width, int height) {
        return this.x <= x + width &&
                this.x + this.width >= x &&
                this.y <= y + height &&
                this.y + this.height >= y;
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setBounds(HitBox hitBox) {
        this.x = hitBox.x;
        this.y = hitBox.y;
        this.width = hitBox.width;
        this.height = hitBox.height;
    }

    public Point getLocation() {
        location.move(this.x, this.y);
        return location;
    }

    @Override
    public String toString() {
        return "HitBox [x=" + x + ", y=" + y + ", width=" + width + ", height=" + height;
    }

    @Override
    public void render(Brush<?, ?> brush) {
        brush.drawRectangle(x, y, width, height, false);
    }

    @Override
    public boolean visible() {
        return visible;
    }
}
