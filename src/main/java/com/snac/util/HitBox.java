package com.snac.util;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.io.Serializable;
import java.util.function.Consumer;

@Getter
@Setter
public class HitBox extends Attachable<HitBox> implements Serializable {
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
        onMove(x, y, x + dx, y + dy);
        x += dx;
        y += dy;
    }

    public void setBounds(int x, int y, int width, int height) {
        onMove(this.x, this.y, width, height);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setBounds(HitBox hitBox) {
        onMove(getX(), getY(), hitBox.getX(), hitBox.getY());
        this.x = hitBox.getX();
        this.y = hitBox.getY();
        this.width = hitBox.getWidth();
        this.height = hitBox.getHeight();
    }

    public Point getLocation() {
        location.move(this.x, this.y);
        return location;
    }

    public void onMove(int oldX, int oldY, int newX, int newY) {
        childAction(child -> {
            child.move(oldX - newX, oldY - newY);
        });
    }

    @Override
    public void childAction(Consumer<HitBox> childAction) {
        if (attachments.isEmpty()) return;
        synchronized (attachments) {
            attachments.forEach(childAction);
        }
    }

    @Override
    public String toString() {
        return "HitBox [x=" + x + ", y=" + y + ", width=" + width + ", height=" + height;
    }
}
