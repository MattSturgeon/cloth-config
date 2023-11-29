package me.shedaniel.autoconfig.requirements.handler;

import me.shedaniel.clothconfig2.api.ValueHolder;

import java.util.List;
import java.util.Objects;

class ConfigEntryRequirementHandler<T, C> implements RequirementHandler, ConditionChecker<T, C> {
    private final ValueHolder<T> gui;
    private final List<C> conditions;
    
    protected ConfigEntryRequirementHandler(ValueHolder<T> gui, C[] conditions) {
        this(gui, List.of(conditions));
    }
    
    protected ConfigEntryRequirementHandler(ValueHolder<T> gui, List<C> conditions) {
        this.gui = gui;
        this.conditions = conditions;
    }
    
    private boolean checkCondition(T value) {
        return conditions.stream()
                .anyMatch(c -> this.check(value, c));
    }
    
    @Override
    public boolean run() {
            T value = gui.getValue();
            return checkCondition(value);
    }
    
    /**
     * Default implementation (will usually be overridden)
     * {@inheritDoc}
     */
    @Override
    public boolean check(T value, C condition) {
        return Objects.equals(value, condition);
    }
}
