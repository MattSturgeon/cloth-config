package me.shedaniel.autoconfig.requirements.parser;

import java.text.ParseException;

public class CharacterParser implements ValueParser<Character> {
    @Override
    public Character parse(String string) {
        if (string.isEmpty()) {
            throw new IllegalArgumentException(new ParseException("Cannot parse an empty string into a char", 0));
        }
        if (string.length() > 1) {
            throw new IllegalArgumentException(new ParseException("Cannot parse a multi-char string into a char (\"%s\")".formatted(string), 0));
        }
        return string.charAt(0);
    }
}
