package me.shedaniel.autoconfig.requirements.builder;

import me.shedaniel.clothconfig2.api.ValueHolder;

import java.util.Arrays;
import java.util.regex.Pattern;

public abstract class AbstractRequirementBuilder<T> implements RequirementBuilder<T> {
    
    protected final ValueHolder<T> gui;
    
    // FIXME this will be a lot simpler with collections...
    protected final Pattern[] regexConditions;
    
    protected AbstractRequirementBuilder(ValueHolder<T> gui, Pattern[] regexConditions) {
        this.gui = gui;
        this.regexConditions = regexConditions;
    }
    
    // FIXME this will be a lot simpler with collections...
    protected abstract T[] conditions();
    
    @Override
    public T getValue() {
        return gui.getValue();
    }
    
    @Override
    public boolean conditionsMatch(T value) {
        // TODO should we allow conditions to ignore case?
        //    Or is it ok that regex can provide this usage?
        return Arrays.asList(conditions()).contains(value);
    }
    
    @Override
    public boolean regexConditionsMatch(T value) {
        String string = value.toString();
        return Arrays.stream(regexConditions)
                .anyMatch(pattern -> pattern.matcher(string).matches());
    }
}
