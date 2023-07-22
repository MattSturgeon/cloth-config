package me.shedaniel.autoconfig.requirements.builder;

import me.shedaniel.clothconfig2.api.ValueHolder;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.regex.Pattern;

public class EnumRequirementBuilder<T extends Enum<?>> extends AbstractRequirementBuilder<T> {
    
    public EnumRequirementBuilder(ValueHolder<T> gui, String[] conditions, Pattern[] regexConditions) {
        super(gui, conditions, regexConditions);
    }
    
    @Override
    protected T[] parseConditions(String[] conditions) {
        Class<T> type = this.gui.getType();
        T[] permitted = type.getEnumConstants();
        
        return Arrays.stream(conditions)
                .map(s -> Arrays.stream(permitted)
                        .filter(val -> s.equals(val.toString()))
                        .findAny()
                        .orElseThrow(() -> new RuntimeException("Invalid enum constant \"%s\" (not found in %s)"
                                .formatted(s, type.getCanonicalName()))))
                .toArray(length -> (T[]) Array.newInstance(type, length));
    }
}
