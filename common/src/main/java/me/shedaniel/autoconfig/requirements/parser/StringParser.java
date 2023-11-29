package me.shedaniel.autoconfig.requirements.parser;

public class StringParser implements ValueParser<String> {
    
    @Override
    public String parse(String string) {
        return string;
    }
}
