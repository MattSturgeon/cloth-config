package me.shedaniel.autoconfig.requirements.builder;

import me.shedaniel.clothconfig2.api.ValueHolder;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class GenericRequirementBuilder<T>  implements RequirementBuilder<T> {
    
    private final ValueHolder<T> gui;
    private final String[] conditions;
    private final Pattern[] regexConditions;
    
    public GenericRequirementBuilder(ValueHolder<T> gui, String[] conditions, Pattern[] regexConditions) {
        this.gui = gui;
        this.conditions = conditions;
        this.regexConditions = regexConditions;
    }
    
    @Override
    public T getValue() {
        return gui.getValue();
    }
    
    @Override
    public boolean conditionsMet(T value) {
        String string = value.toString();
        return List.of(conditions).contains(string);
    }
    
    @Override
    public boolean regexConditionsMet(T value) {
        String string = value.toString();
        return Arrays.stream(regexConditions)
                .anyMatch(pattern -> pattern.matcher(string).matches());
    }
}
