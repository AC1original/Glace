package com.snac.graphics;

public interface Renderable {

    boolean visible();

    Priority priority();

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