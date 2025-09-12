package com.snac.util.annotations;

import io.github.classgraph.ClassGraph;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Annotation to mark methods that should be invoked
 * by the Tick loader system.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Tick {

    /**
     * A unique key to group methods together for ticking.
     *
     * @return the link key
     */
    String link();

    /**
     * Loader class responsible for scanning, registering, and invoking
     * methods annotated with @Tick.
     */
    @Slf4j
    final class Loader {

        /**
         * Stores methods grouped by their link key.
         */
        private static final Map<String, List<Method>> tickMethods = new HashMap<>();

        /**
         * Scans the classpath for methods annotated with @Tick,
         * filters parameterless methods, and registers them under the given link.
         *
         * @param link the key to register methods under
         */
        public static void registerLink(String link) {
            try (var scanResult = new ClassGraph()
                    .enableMethodInfo()
                    .acceptPackages()
                    .scan()) {

                scanResult.getClassesWithMethodAnnotation(Tick.class)
                        .stream()
                        .flatMap(info -> info.getMethodInfo().stream())
                        .filter(info -> info.hasAnnotation(Tick.class))
                        .forEach(info -> {
                            try {
                                var method = info.loadClassAndGetMethod();
                                if (method.getParameterCount() < 1) {
                                    tickMethods.computeIfAbsent(link, k -> new ArrayList<>()).add(method);
                                } else {
                                    log.warn(
                                            "Couldn't link method {} in class {} to '{}' because it has parameters",
                                            method.getName(),
                                            method.getDeclaringClass().getSimpleName(),
                                            link
                                    );
                                }
                            } catch (Exception e) {
                                log.error(
                                        "Couldn't link method {} in class {} to '{}'",
                                        info.getName(),
                                        info.loadClassAndGetMethod().getDeclaringClass().getSimpleName(),
                                        link,
                                        e
                                );
                            }
                        });
            }
        }

        /**
         * Invokes all methods registered under the given link.
         * <p>
         * - Static methods are invoked with {@code null} as the instance.
         * - Non-static methods are invoked on any object provided in {@code instances}
         *   that matches the declaring class of the method.
         *
         * @param link the key under which methods were registered
         * @param instances optional objects for invoking non-static methods
         */
        public static void tick(String link, Object... instances) {
            if (!tickMethods.containsKey(link)) return;

            try {
                for (var method : tickMethods.get(link)) {
                    if (Modifier.isStatic(method.getModifiers())) {
                        method.invoke(null);
                        continue;
                    }
                    for (var inst : instances) {
                        if (method.getDeclaringClass().isInstance(inst)) {
                            method.invoke(inst);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Couldn't tick link '{}'", link, e);
            }
        }
    }
}
