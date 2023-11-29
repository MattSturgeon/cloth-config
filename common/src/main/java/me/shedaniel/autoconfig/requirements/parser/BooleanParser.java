package me.shedaniel.autoconfig.requirements.parser;

import java.util.regex.Pattern;

public class BooleanParser implements ValueParser<Boolean> {
    private static final Pattern TRUTHY = Pattern.compile("^(?:1|t(?:rue)?|y(?:es)?|on|enabled?)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FALSEY = Pattern.compile("^(?:0|f(?:alse)?|no?|off|disabled?)$", Pattern.CASE_INSENSITIVE);
    
    BooleanParser() {}
    
    @Override
    public Boolean parse(String s) {
        if (TRUTHY.matcher(s).matches()) {
            return true;
        }
        if (FALSEY.matcher(s).matches()) {
            return false;
        }
        // Neither true nor false
        throw new RuntimeException("Invalid boolean condition \"%s\"".formatted(s));
    }
}
