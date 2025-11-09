package com.snac.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
@Slf4j
@Getter
public abstract class Attachable<T extends Attachable<T>> {
    @Nullable
    protected T parent = null;
    protected final Set<T> attachments = Collections.synchronizedSet(new HashSet<>());

    public void addAttachment(T attachable) {
        if (isAttached()) {
            var parent = getParent() == null ? "null" : getParent().getClass().getSimpleName();
            log.warn("Object {} is already attached to {}. It must be detached first.",
                    attachable.getClass().getSimpleName(),
                    parent);
            return;
        }
        attachments.add(attachable);
        attachable.parent = (T) this;
        attachable.onSelfAttach((T) this);
    }

    public void removeAttachment(T attachable) {
        attachments.remove(attachable);
        attachable.parent = null;
        attachable.onSelfDetach((T) this);
    }

    protected void onSelfAttach(T attachedTo) {}
    protected void onSelfDetach(T detachedFrom) {}

    public boolean isAttached() {
        return parent != null;
    }

    protected void onParentPositionChange(Consumer<T> childAction) {
        if (attachments.isEmpty()) return;
        synchronized (attachments) {
            attachments.forEach(childAction);
        }
    }

    public final CompletableFuture<T> getRootParent() {
        if (getParent() == null) return CompletableFuture.completedFuture((T) this);
        return CompletableFuture.supplyAsync(() -> {
            var lastParent = getParent();
            while (true) {
                var parent = lastParent.getParent();

                if (parent == null) return lastParent;
                else lastParent = parent;
            }
        });
    }
}
