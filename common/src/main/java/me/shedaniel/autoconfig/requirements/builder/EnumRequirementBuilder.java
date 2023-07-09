package me.shedaniel.autoconfig.requirements.builder;

import me.shedaniel.clothconfig2.api.ValueHolder;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.regex.Pattern;

public class EnumRequirementBuilder<T extends Enum<?>>  implements StaticRequirementBuilder<T> {
    
    private final ValueHolder<? extends T> gui;
    private final T[] conditions;
    private final Pattern[] regexConditions;
    
    public EnumRequirementBuilder(ValueHolder<T> gui, String[] conditions, Pattern[] regexConditions) {
        this.gui = gui;
        this.regexConditions = regexConditions;
        
        Class<T> type = gui.getType();
        T[] permitted = type.getEnumConstants();
        
        this.conditions = Arrays.stream(conditions)
                .map(s -> Arrays.stream(permitted)
                            .filter(val -> s.equals(val.toString()))
                            .findAny()
                            .orElseThrow(() -> new RuntimeException("Invalid enum constant \"%s\" (not found in %s)"
                                    .formatted(s, type.getCanonicalName()))))
                .toArray(length -> (T[]) Array.newInstance(type, length));
    }
    
    @Override
    public ValueHolder<? extends T> gui() {
        return this.gui;
    }
    
    @Override
    public T[] conditions() {
        return this.conditions;
    }
    
    @Override
    public Pattern[] regexConditions() {
        return this.regexConditions;
    }
}
