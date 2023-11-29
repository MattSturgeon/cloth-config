package me.shedaniel.autoconfig.requirements.parser;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ValueParser<T> {
    
    T parse(String string);
    
    static <T> @Nullable ValueParser<T> forType(Class<T> type) {
        return ValueParserFactory.forType(type);
    }
}
