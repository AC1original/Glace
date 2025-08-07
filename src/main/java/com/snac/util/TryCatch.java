package com.snac.util;

import java.util.function.Consumer;

public class TryCatch {

    public static void try_(Runnable try_) {
        tryCatchFinally(try_, (e) -> {}, () -> {});
    }

    public static void tryCatch(Runnable try_, Consumer<Exception> catch_) {
        tryCatchFinally(try_, catch_, () -> {});
    }

    public static void tryFinally(Runnable try_, Runnable finally_) {
        tryCatchFinally(try_, (e) -> {}, finally_);
    }

    public static void tryThrowFinally(Runnable try_, Runnable finally_) {
        try {
            try_.run();
        } finally {
            finally_.run();
        }
    }

    public static void tryCatchFinally(Runnable try_, Consumer<Exception> catch_, Runnable finally_) {
        try {
            try_.run();
        } catch (Exception e) {
            catch_.accept(e);
        } finally {
            finally_.run();
        }
    }
}
