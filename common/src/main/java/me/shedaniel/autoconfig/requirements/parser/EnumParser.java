package me.shedaniel.autoconfig.requirements.parser;

import java.util.Arrays;

public class EnumParser<T extends Enum<?>> implements ValueParser<T> {
    
    private final Class<T> type;
    
    EnumParser(Class<T> type) {
        this.type = type;
    }
    
    @Override
    public T parse(String string) {
        return Arrays.stream(type.getEnumConstants())
                .filter(val -> string.equals(val.toString()))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Invalid enum constant \"%s\" (not found in %s)"
                        .formatted(string, type.getCanonicalName())));
    }
}
