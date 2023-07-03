package me.shedaniel.clothconfig2.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
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
@ApiStatus.Experimental
public interface Requirement {
    
    /**
     * Checks if this requirement is currently true.
     */
    boolean check();
    
    /**
     * Generates a {@link Requirement} that is true when {@code dependency}'s value is one of the provided values.
     */
    @SafeVarargs
    static <T> @NotNull Requirement isValue(@NotNull ValueHolder<T> dependency, @Nullable T firstValue, @Nullable T... otherValues) {
        Set<@Nullable T> values = Stream.concat(Stream.of(firstValue), Arrays.stream(otherValues))
                .collect(Collectors.toCollection(HashSet::new));
        
        return () -> values.contains(dependency.getValue());
    }
    
    /**
     * Generates a {@link Requirement} that is true when {@code firstDependency}'s value equals {@code secondDependency}'s value.
     */
    static @NotNull <T> Requirement matches(@NotNull ValueHolder<T> firstDependency, @NotNull ValueHolder<T> secondDependency) {
        return () -> Objects.equals(firstDependency.getValue(), secondDependency.getValue());
    }
    
    /**
     * Generates a {@link Requirement} that is true when {@code dependency}'s value is true.
     */
    static @NotNull Requirement isTrue(@NotNull ValueHolder<Boolean> dependency) {
        return () -> Boolean.TRUE.equals(dependency.getValue());
    }
    
    /**
     * Generates a {@link Requirement} that is true when {@code dependency}'s value is false.
     */
    static @NotNull Requirement isFalse(@NotNull ValueHolder<Boolean> dependency) {
        return () -> Boolean.FALSE.equals(dependency.getValue());
    }
    
    /**
     * Generates a {@link Requirement} that is true when the given {@code requirement} is false.
     */
    static Requirement not(@NotNull Requirement requirement) {
        return () -> !requirement.check();
    }
  
    /**
     * Generates a {@link Requirement} that is true when all the given requirements are true.
     */
    static @NotNull Requirement all(@NotNull Requirement... requirements) {
        return () -> Arrays.stream(requirements).allMatch(Requirement::check);
    }
    
    /**
     * Generates a {@link Requirement} that is true when any of the given requirements are true.
     */
    static @NotNull Requirement any(@NotNull Requirement... requirements) {
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
