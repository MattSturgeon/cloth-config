package me.shedaniel.clothconfig2.api.dependencies;

import me.shedaniel.clothconfig2.api.dependencies.builders.BooleanDependencyBuilder;
import me.shedaniel.clothconfig2.api.dependencies.builders.DependencyGroupBuilder;
import me.shedaniel.clothconfig2.api.dependencies.builders.EnumDependencyBuilder;
import me.shedaniel.clothconfig2.api.dependencies.builders.NumberDependencyBuilder;
import me.shedaniel.clothconfig2.api.entries.NumberConfigEntry;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

import static me.shedaniel.clothconfig2.api.dependencies.DependencyGroup.Condition.*;

public interface Dependency {
    
    static DependencyGroupBuilder groupBuilder() {
        return new DependencyGroupBuilder();
    }
    
    static BooleanDependencyBuilder builder(BooleanListEntry gui) {
        return new BooleanDependencyBuilder(gui);
    }
    
    static <T extends Enum<?>> EnumDependencyBuilder<T> builder(EnumListEntry<T> gui) {
        return new EnumDependencyBuilder<>(gui);
    }
    
    static <T extends Number & Comparable<T>> NumberDependencyBuilder<T> builder(NumberConfigEntry<T> gui) {
        return new NumberDependencyBuilder<>(gui);
    }
    
    static DependencyGroupBuilder groupBuilder(DependencyGroup.Condition condition) {
        return groupBuilder().withCondition(condition);
    }
    
    /**
     * Generates a {@link DependencyGroup} that depends on all of its dependencies being met.
     * <br>
     * 
     * @param dependencies the dependencies to be included in the group 
     * @return the generated group
     */
    static @NotNull DependencyGroup all(Dependency... dependencies) {
        return groupBuilder(ALL).withChildren(dependencies).build();
    }
    /**
     * Generates a {@link DependencyGroup} that depends on all of its dependencies being met.
     * <br>
     * 
     * @param dependencies a {@link Collection} of dependencies to be included in the group 
     * @return the generated group
     */
    static @NotNull DependencyGroup all(Collection<Dependency> dependencies) {
        return groupBuilder(ALL).withChildren(dependencies).build();
    }
    
    /**
     * Generates a {@link DependencyGroup} that depends on none of its dependencies being met.
     * <br>
     * I.e. the group is unmet if any of its dependencies are met.
     *
     * @param dependencies the dependencies to be included in the group 
     * @return the generated group
     */
    static @NotNull DependencyGroup none(Dependency... dependencies) {
        return groupBuilder(NONE).withChildren(dependencies).build();
    }
    /**
     * Generates a {@link DependencyGroup} that depends on none of its dependencies being met.
     * <br>
     * I.e. the group is unmet if any of its dependencies are met.
     *
     * @param dependencies a {@link Collection} of dependencies to be included in the group 
     * @return the generated group
     */
    static @NotNull DependencyGroup none(Collection<Dependency> dependencies) {
        return groupBuilder(NONE).withChildren(dependencies).build();
    }
    
    /**
     * Generates a {@link DependencyGroup} that depends on any of its dependencies being met.
     * <br>
     * I.e. the group is met if one or more of its dependencies are met. 
     *
     * @param dependencies the dependencies to be included in the group 
     * @return the generated group
     */
    static @NotNull DependencyGroup any(Dependency... dependencies) {
        return groupBuilder(ANY).withChildren(dependencies).build();
    }
    /**
     * Generates a {@link DependencyGroup} that depends on any of its dependencies being met.
     * <br>
     * I.e. the group is met if one or more of its dependencies are met. 
     *
     * @param dependencies a {@link Collection} of dependencies to be included in the group 
     * @return the generated group
     */
    static @NotNull DependencyGroup any(Collection<Dependency> dependencies) {
        return groupBuilder(ANY).withChildren(dependencies).build();
    }
    
    /**
     * Generates a {@link DependencyGroup} that depends on exactly one of its dependencies being met.
     * <br>
     * I.e. the group is met if precisely one dependency is met, however the group is unmet if more than one
     * (or less than one) are met.
     *
     * @param dependencies the dependencies to be included in the group 
     * @return the generated group
     */
    static @NotNull DependencyGroup one(Dependency... dependencies) {
        return groupBuilder(ONE).withChildren(dependencies).build();
    }
    /**
     * Generates a {@link DependencyGroup} that depends on exactly one of its dependencies being met.
     * <br>
     * I.e. the group is met if precisely one dependency is met, however the group is unmet if more than one
     * (or less than one) are met.
     *
     * @param dependencies a {@link Collection} of dependencies to be included in the group 
     * @return the generated group
     */
    static @NotNull DependencyGroup one(Collection<Dependency> dependencies) {
        return groupBuilder(ONE).withChildren(dependencies).build();
    }
    
    /**
     * Checks if this dependency is currently met.
     *
     * @return whether the dependency is met
     */
    boolean check();
    
    /**
     * Checks if this dependency is currently unmet and an entry with this dependency should be hidden.
     *
     * @return whether dependent entries should be hidden
     */
    default boolean hidden() {
        return !check() && hiddenWhenNotMet();
    }
    
    /**
     * @return whether entries with this dependency should hide when this dependency is unmet, instead of simply being disabled.
     */
    boolean hiddenWhenNotMet();
    
    /**
     * Sets whether entries with this dependency should hide when this dependency is unmet, instead of simply being disabled.
     * 
     * @param shouldHide whether dependant entries should hide
     */
    void hiddenWhenNotMet(boolean shouldHide);
    
    /**
     * Get a short description of this dependency. For use by GUIs, e.g. {@link DependencyGroup} tooltips.
     * 
     * @return a {@link Component} containing the description
     */
    Component getShortDescription();
    
    /**
     * Generates a tooltip for this dependency.
     * 
     * @return an {@link Optional} containing the tooltip, otherwise {@code Optional.empty()}.
     */
    Optional<Component[]> getTooltip();
}
