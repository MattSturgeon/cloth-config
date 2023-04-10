package me.shedaniel.clothconfig2.impl.dependencies;

import me.shedaniel.clothconfig2.api.ConfigEntry;

/**
 * A dependency that compares a given config entry's value to that of other config entries.
 * 
 * @param <T> the type handled by the config entries 
 */
public class GenericDependency<T> extends ConfigEntryDependency<T, ConfigEntry<T>> {
    GenericDependency(ConfigEntry<T> entry) {
        super(entry);
        // TODO consider allowing multi-to-multi matching?
        // TODO consider using DependencyGroup.Condition to allow ALL/ANY/NONE etc?
    }
}
