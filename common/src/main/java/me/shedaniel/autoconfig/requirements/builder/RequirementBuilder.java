package me.shedaniel.autoconfig.requirements.builder;

import me.shedaniel.clothconfig2.api.Requirement;

public interface RequirementBuilder<T> {
    T getValue();
    
    boolean conditionsMet(T value);
    
    boolean regexConditionsMet(T value);
    
    default Requirement build() {
        return () -> {
            T val = getValue();
            return conditionsMet(val) || regexConditionsMet(val);
        };
    }
}
