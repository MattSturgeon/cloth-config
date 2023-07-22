package me.shedaniel.autoconfig.requirements.builder;

import me.shedaniel.clothconfig2.api.ValueHolder;
import org.jetbrains.annotations.ApiStatus;

import java.util.regex.Pattern;

@ApiStatus.Internal
class GenericRequirementBuilder<T>  extends AbstractRequirementBuilder<T, String> {
    
    GenericRequirementBuilder(ValueHolder<T> gui, String[] conditions, Pattern[] regexConditions) {
        super(gui, conditions, regexConditions);
    }
    
    @Override
    public boolean check(T value, String condition) {
        //FIXME kinda inefficient to transform the value on every condition...
        return value.toString().equals(condition);
    }
}
