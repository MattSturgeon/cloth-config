package me.shedaniel.clothconfig2.api.dependencies.builders;

import me.shedaniel.clothconfig2.api.dependencies.EnumDependency;
import me.shedaniel.clothconfig2.api.dependencies.conditions.EnumCondition;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;

public class EnumDependencyBuilder<T extends Enum<?>> extends AbstractDependencyBuilder<T, EnumListEntry<T>, EnumDependency<T>, EnumCondition<T>, EnumDependencyBuilder<T>> {
    
    public EnumDependencyBuilder(EnumListEntry<T> gui) {
        super(gui);
    }
    
    @Override
    public EnumDependencyBuilder<T> withCondition(T condition) {
        return withCondition(new EnumCondition<>(condition));
    }
    
    @Override
    public EnumDependency<T> build() {
        if (conditions.isEmpty())
            throw new IllegalStateException("EnumDependency requires at least one condition");
        
        return finishBuilding(new EnumDependency<>(this.gui));
    }
}
