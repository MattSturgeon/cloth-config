package me.shedaniel.autoconfig.requirements.builder;

import me.shedaniel.clothconfig2.api.ValueHolder;

import java.util.regex.Pattern;

public class StringRequirementBuilder extends AbstractRequirementBuilder<String, String> {
    
    public StringRequirementBuilder(ValueHolder<String> gui, String[] conditions, Pattern[] regexConditions) {
        super(gui, conditions, regexConditions);
    }
}
