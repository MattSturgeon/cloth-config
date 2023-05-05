package me.shedaniel.clothconfig2.api.dependencies;

import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@FunctionalInterface
public interface Dependency {
    
    /**
     * Checks if this dependency is currently met.
     *
     * @return whether the dependency is met
     */
    boolean check();
    
    /**
     * Generates a {@link Dependency} that checks if the {@code gui}'s value is {@code true}.
     */
    static @NotNull Dependency isTrue(BooleanListEntry gui) {
        return gui::getValue;
    }
    
    /**
     * Generates a {@link Dependency} that checks if the {@code gui}'s value is {@code false}.
     */
    static @NotNull Dependency isFalse(BooleanListEntry gui) {
        return () -> !gui.getValue();
    }
    
    /**
     * Generates a {@link Dependency} that checks if the {@code gui}'s value is one of the values provided.
     */
    @SafeVarargs
    static <T> @NotNull Dependency isValue(AbstractConfigEntry<T> gui, T firstValue, T... otherValues) {
        Set<T> values = Stream.concat(Stream.of(firstValue), Arrays.stream(otherValues))
                .collect(Collectors.toUnmodifiableSet());
        
        return () -> values.contains(gui.getValue());
    }
    
    /**
     * Generates a {@link Dependency} that compares the {@code gui}'s value to the given {@code otherGui}'s value.
     */
    static @NotNull <T> Dependency matches(AbstractConfigEntry<T> gui, AbstractConfigEntry<T> otherGui) {
        return () -> gui.getValue().equals(otherGui.getValue());
    }
    
    /**
     * Generates a {@link Dependency} that depends on all of its dependencies being met.
     * <br>
     * 
     * @param dependencies the dependencies to be included in the group 
     * @return the generated group
     */
    static @NotNull Dependency all(Dependency... dependencies) {
        return () -> Arrays.stream(dependencies).allMatch(Dependency::check);
    }
    
    /**
     * Generates a {@link Dependency} that depends on none of its dependencies being met.
     * <br>
     * I.e. the group is unmet if any of its dependencies are met.
     *
     * @param dependencies the dependencies to be included in the group 
     * @return the generated group
     */
    static @NotNull Dependency none(Dependency... dependencies) {
        return () -> Arrays.stream(dependencies).noneMatch(Dependency::check);
    }
    
    /**
     * Generates a {@link Dependency} that depends on any of its dependencies being met.
     * <br>
     * I.e. the group is met if one or more of its dependencies are met. 
     *
     * @param dependencies the dependencies to be included in the group 
     * @return the generated group
     */
    static @NotNull Dependency any(Dependency... dependencies) {
        return () -> Arrays.stream(dependencies).anyMatch(Dependency::check);
    }
    
    /**
     * Generates a {@link Dependency} that depends on exactly one of its dependencies being met.
     * <br>
     * I.e. the group is met if precisely one dependency is met, however the group is unmet if more than one
     * (or less than one) are met.
     *
     * @param dependencies the dependencies to be included in the group 
     * @return the generated group
     */
    static @NotNull Dependency one(Dependency... dependencies) {
        return () -> Arrays.stream(dependencies)
                .filter(Dependency::check)
                .count() == 1;
    }
}
