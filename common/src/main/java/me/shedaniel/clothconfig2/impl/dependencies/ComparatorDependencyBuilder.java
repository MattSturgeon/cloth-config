package me.shedaniel.clothconfig2.impl.dependencies;

import me.shedaniel.clothconfig2.api.ConfigEntry;
import me.shedaniel.clothconfig2.api.dependencies.DependencyBuilder;
import me.shedaniel.clothconfig2.api.dependencies.conditions.ConfigEntryMatcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ComparatorDependencyBuilder<T> implements DependencyBuilder<ComparatorDependency<T>> {
    private final Set<ConfigEntryMatcher<T>> conditions = new HashSet<>();
    private final ConfigEntry<T> gui;
    
    public <E extends ConfigEntry<T>> ComparatorDependencyBuilder(E gui) {
        
        this.gui = gui;
    }
    
    @Override
    public ComparatorDependency<T> build() {
        if (conditions.isEmpty())
            throw new IllegalArgumentException("ComparatorDependency requires at least 1 condition.");
        ComparatorDependency<T> dependency = new ComparatorDependency<>(this.gui);
        dependency.addConditions(conditions);
        return dependency;
    }
    
    public <E extends ConfigEntry<T>> ComparatorDependencyBuilder<T> matching(E... gui) {
        return matching(Set.of(gui));
    }
    
    public <E extends ConfigEntry<T>> ComparatorDependencyBuilder<T> matching(Collection<E> guis) {
        conditions.addAll(guis.stream().map(gui -> new ConfigEntryMatcher<T>(gui))
                .collect(Collectors.toUnmodifiableSet()));
        
        return this;
    }
    
    public ComparatorDependencyBuilder<T> matching(ConfigEntryMatcher<T> condition) {
        conditions.add(condition);
        return this;
    }
}
