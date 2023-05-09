package me.shedaniel.clothconfig2.api;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a predicate (boolean-valued function) without arguments.
 *
 * <p>This is a <a href="{@docRoot}/java/util/function/package-summary.html">functional interface</a>
 * whose functional method is {@link #check()}.
 */
@FunctionalInterface
public interface Requirement {
    
    /**
     * Checks if this requirement is currently true.
     */
    boolean check();
    
    /**
     * Generates a {@link Requirement} that is true when {@code gui}'s value is one of the provided values.
     */
    @SafeVarargs
    static <T> @NotNull Requirement isValue(AbstractConfigEntry<T> gui, T firstValue, T... otherValues) {
        Set<T> values = Stream.concat(Stream.of(firstValue), Arrays.stream(otherValues))
                .collect(Collectors.toUnmodifiableSet());
        
        return () -> values.contains(gui.getValue());
    }
    
    /**
     * Generates a {@link Requirement} that is true when {@code firstGui}'s value equals {@code secondGui}'s value.
     */
    static @NotNull <T> Requirement matches(AbstractConfigEntry<T> firstGui, AbstractConfigEntry<T> secondGui) {
        return () -> firstGui.getValue().equals(secondGui.getValue());
    }
    
    /**
     * Generates a {@link Requirement} that is true when {@code gui}'s value is true.
     */
    static @NotNull Requirement isTrue(AbstractConfigEntry<Boolean> gui) {
        return gui::getValue;
    }
    
    /**
     * Generates a {@link Requirement} that is true when {@code gui}'s value is false.
     */
    static @NotNull Requirement isFalse(AbstractConfigEntry<Boolean> gui) {
        return () -> !gui.getValue();
    }
    
    /**
     * Generates a {@link Requirement} that is true when the given {@code requirement} is false.
     */
    static Requirement not(Requirement requirement) {
        return () -> !requirement.check();
    }
  
    /**
     * Generates a {@link Requirement} that is true when all the given requirements are true.
     */
    static @NotNull Requirement all(Requirement... requirements) {
        return () -> Arrays.stream(requirements).allMatch(Requirement::check);
    }
    
    /**
     * Generates a {@link Requirement} that is true when any of the given requirements are true.
     */
    static @NotNull Requirement any(Requirement... requirements) {
        return () -> Arrays.stream(requirements).anyMatch(Requirement::check);
    }
    
    /**
     * Generates a {@link Requirement} that is true when none of the given requirements are true, i.e. all are false.
     */
    static @NotNull Requirement none(Requirement... requirements) {
        return () -> Arrays.stream(requirements).noneMatch(Requirement::check);
    }
    
    /**
     * Generates a {@link Requirement} that is true when precisely one of the given requirements is true.
     */
    static @NotNull Requirement one(Requirement... requirements) {
        return () -> {
            // Use a for loop instead of Stream.count() so that we can return early. We only need to count past 1.
            boolean oneFound = false;
            for (Requirement requirement : requirements) {
               if (!requirement.check())
                   continue;
               if (oneFound)
                   return false;
               oneFound = true;
            }
            return oneFound;
        };
    }
}
