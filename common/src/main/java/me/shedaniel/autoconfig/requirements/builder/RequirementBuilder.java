package me.shedaniel.autoconfig.requirements.builder;

import me.shedaniel.clothconfig2.api.Requirement;

public interface RequirementBuilder<T> {
    T getValue();
    
    boolean conditionsMatch(T value);
    
    boolean regexConditionsMatch(T value);
    
    default Requirement build() {
        return () -> {
            T val = getValue();
            if (conditionsMatch(val)) {
                return true;
            }
            return regexConditionsMatch(val);
        };
    }
}
