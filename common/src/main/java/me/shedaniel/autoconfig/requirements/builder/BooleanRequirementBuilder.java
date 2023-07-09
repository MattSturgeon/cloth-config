package me.shedaniel.autoconfig.requirements.builder;

import me.shedaniel.clothconfig2.api.ValueHolder;

import java.util.Arrays;
import java.util.regex.Pattern;

public class BooleanRequirementBuilder implements StaticRequirementBuilder<Boolean> {
    
    private static final Pattern TRUTHY = Pattern.compile("^(?:t(?:rue)?|y(?:es)?|on|enabled?)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FALSEY = Pattern.compile("^(?:f(?:alse)?|no?|off|disabled?)$", Pattern.CASE_INSENSITIVE);
    
    private final ValueHolder<? extends Boolean> gui;
    private final Boolean[] conditions;
    private final Pattern[] regexConditions;
    
    public BooleanRequirementBuilder(ValueHolder<? extends Boolean> gui, String[] conditions, Pattern[] regexConditions) {
        this.gui = gui;
        this.regexConditions = regexConditions;
        
        Boolean[] definedConditions = Arrays.stream(conditions)
                .map(s -> {
                    if (TRUTHY.matcher(s).matches()) {
                        return true;
                    }
                    if (FALSEY.matcher(s).matches()) {
                        return false;
                    }
                    // Neither true nor false
                    throw new RuntimeException("Invalid boolean condition \"%s\"".formatted(s));
                })
                .toArray(Boolean[]::new);
        
        // For booleans, default to checking `true` if no conditions are defined
        this.conditions = definedConditions.length < 1 && regexConditions.length < 1
                ? new Boolean[]{true}
                : definedConditions;
    }
    
    @Override
    public ValueHolder<? extends Boolean> gui() {
        return this.gui;
    }
    
    @Override
    public Boolean[] conditions() {
        return this.conditions;
    }
    
    @Override
    public Pattern[] regexConditions() {
        return this.regexConditions;
    }
}
