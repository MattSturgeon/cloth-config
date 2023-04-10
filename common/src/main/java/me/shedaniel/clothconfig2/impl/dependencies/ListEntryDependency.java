package me.shedaniel.clothconfig2.impl.dependencies;

import me.shedaniel.clothconfig2.api.ListConfigEntry;
import me.shedaniel.clothconfig2.api.dependencies.conditions.Condition;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Represents a dependency on a {@link ListConfigEntry}
 * @param <T> 
 */
public class ListEntryDependency<T> extends AbstractElementDependency<Condition<Collection<T>>, ListConfigEntry<T>> {
    
    ListEntryDependency(ListConfigEntry<T> entry) {
        super(entry);
    }
    
    @Override
    public boolean check() {
        List<T> values = getElement().getValue();
        return this.getRequirement().matches(getConditions(), condition -> condition.check(values));
    }
    
    @Override
    public Component getShortDescription(boolean inverted) {
        // TODO
        return null;
    }
    
    @Override
    public Optional<Component[]> getTooltip(boolean inverted, String effectKey) {
        // TODO
        return Optional.empty();
    }
}
