package me.shedaniel.clothconfig2.impl.dependencies;

import me.shedaniel.clothconfig2.api.dependencies.Dependency;
import me.shedaniel.clothconfig2.api.dependencies.requirements.GroupRequirement;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class AbstractDependency<C> implements Dependency {
    private final Set<C> conditions = new LinkedHashSet<>();
    private GroupRequirement requirement = GroupRequirement.ANY;
    private boolean showTooltips = true;
    private Supplier<Component> describer = null;
    
    private Function<String, Component[]> tooltipProvider = null;
    
    /**
     * Get the dependency's conditions.
     *
     * @return a {@link Set} containing the dependency's conditions
     */
    public final Set<C> getConditions() {
        return conditions;
    }
    
    /**
     * Adds one or more conditions to the dependency. If any condition matches the entry's value,
     * then the dependency is met.
     *
     * @param conditions a {@link Collection} of conditions to be added
     */
    public final void addConditions(Collection<? extends C> conditions) {
        this.conditions.addAll(conditions.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet()));
    }
    
    @Override
    public void setRequirement(GroupRequirement requirement) {
        this.requirement = requirement;
    }
    
    @Override
    public GroupRequirement getRequirement() {
        return this.requirement;
    }
    
    public void displayTooltips(boolean shouldDisplay) {
        this.showTooltips = shouldDisplay;
    }
    
    @Override
    public boolean hasTooltip() {
        return this.showTooltips && tooltipProvider != null;
    }
    
    @Override
    public void setDescriber(Supplier<Component> describer) {
        this.describer = describer;
    }
    
    @Override
    public void setTooltipProvider(Function<String, Component[]> tooltipProvider) {
        this.tooltipProvider = tooltipProvider;
    }
    
    @Override
    public @Nullable Component[] getTooltip(String effectKey) {
        return hasTooltip() && tooltipProvider != null ? tooltipProvider.apply(effectKey) : null;
    }
    
    @Override
    public @Nullable Component getShortDescription() {
        return hasTooltip() && describer != null ? describer.get() : null;
    }
}
