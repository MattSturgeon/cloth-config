package me.shedaniel.autoconfig.requirements.builder;

import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.api.ValueHolder;

import java.util.Arrays;
import java.util.regex.Pattern;

public interface StaticRequirementBuilder<T> {
    
    ValueHolder<? extends T> gui();
    T[] conditions();
    Pattern[] regexConditions();
    
    default boolean conditionsMatch(T value) {
        // TODO should we allow conditions to ignore case?
        //    Or is it ok that regex can provide this usage?
        return Arrays.asList(conditions()).contains(value);
    }
    
    default boolean regexConditionsMatch(T value) {
        String string = value.toString();
        return Arrays.stream(regexConditions())
                .anyMatch(pattern -> pattern.matcher(string).matches());
    }
    
    default Requirement build() {
        return () -> {
            T val = gui().getValue();
            if (conditionsMatch(val)) {
                return true;
            }
            return regexConditionsMatch(val);
        };
    }
}
