package me.shedaniel.autoconfig.requirements.builder;

import me.shedaniel.clothconfig2.api.ValueHolder;

import java.util.Arrays;
import java.util.regex.Pattern;

public class BooleanRequirementBuilder extends AbstractRequirementBuilder<Boolean, Boolean> {
    
    private static final Pattern TRUTHY = Pattern.compile("^(?:t(?:rue)?|y(?:es)?|on|enabled?)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FALSEY = Pattern.compile("^(?:f(?:alse)?|no?|off|disabled?)$", Pattern.CASE_INSENSITIVE);
    
    public BooleanRequirementBuilder(ValueHolder<Boolean> gui, String[] conditions, Pattern[] regexConditions) {
        super(gui, parseConditions(conditions, regexConditions.length), regexConditions);
    }
    
    private static Boolean[] parseConditions(String[] conditions, int otherConditions) {
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
        return definedConditions.length < 1 && otherConditions < 1
                ? new Boolean[]{true}
                : definedConditions;
        
    }
}
