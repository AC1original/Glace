package com.snac.graphics;

public interface Renderable {

    void render(Brush<?, ?> brush);

    default boolean visible() {
        return true;
    }

    default Priority priority() {
        return Priority.DEFAULT;
    }

    default int layer() {
        return -1;
    }

    enum Priority {
        LOWEST,
        LOW,
        DEFAULT,
        HIGH,
        HIGHEST;

        public boolean isHigherThan(Priority priority) {
            return indexOf(this) > indexOf(priority);
        }

        private int indexOf(Priority priority) {
            for (int i = 0; i < Priority.values().length; i++) {
                if (Priority.values()[i].equals(priority)) {
                    return i;
                }
            }
            return 0;
        }
    }
}