package me.shedaniel.autoconfig.requirements.builder;

import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.api.ValueHolder;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

public abstract class AbstractRequirementBuilder<T, C> implements RequirementBuilder<T>, ConditionChecker<T, C> {
    private final ValueHolder<T> gui;
    private final C[] conditions;
    private final Pattern[] patterns;
    
    public AbstractRequirementBuilder(
            ValueHolder<T> gui,
            C[] conditions,
            Pattern[] patterns
    ) {
        this.gui = gui;
        this.conditions = conditions;
        this.patterns = patterns;
    }
    
    private boolean checkCondition(T value) {
        return Arrays.stream(conditions)
                .anyMatch(c -> this.check(value, c));
    }
    
    private boolean checkRegexCondition(T value) {
        String string = value.toString();
        return Arrays.stream(patterns)
                .anyMatch(regex -> regex.matcher(string).matches());
    }
    
    @Override
    public Requirement build() {
        return () -> {
            T value = gui.getValue();
            return checkCondition(value) || checkRegexCondition(value);
        };
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
