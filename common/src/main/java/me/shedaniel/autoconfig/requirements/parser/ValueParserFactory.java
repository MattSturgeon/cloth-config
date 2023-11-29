package me.shedaniel.autoconfig.requirements.parser;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;

class ValueParserFactory {
    private static final ValueParser<Boolean> BOOLEAN = new BooleanParser();
    private static final ValueParser<String> STRING = new StringParser();
    private static final ValueParser<Character> CHARACTER = new CharacterParser();
    private static final ValueParser<Short> SHORT = Short::valueOf;
    private static final ValueParser<Integer> INTEGER = Integer::valueOf;
    private static final ValueParser<Long> LONG = Long::valueOf;
    private static final ValueParser<Float> FLOAT = Float::valueOf;
    private static final ValueParser<Double> DOUBLE = Double::valueOf;
    private static final ValueParser<BigInteger> BIG_INTEGER = BigInteger::new;
    private static final ValueParser<BigDecimal> BIG_DECIMAL = BigDecimal::new;
    
    private ValueParserFactory() {}
    
    @SuppressWarnings("unchecked")
    static <T> @Nullable ValueParser<T> forType(Class<T> type) {
        if (Boolean.class.equals(type)) {
            return (ValueParser<T>) BOOLEAN;
        }
        if (String.class.equals(type)) {
            return (ValueParser<T>) STRING;
        }
        if (Character.class.equals(type)) {
            return (ValueParser<T>) CHARACTER;
        }
        if (Enum.class.isAssignableFrom(type)) {
            EnumParser<?> parser = new EnumParser<>((Class<Enum<?>>) type);
            return (ValueParser<T>) parser;
        }
        if (Number.class.isAssignableFrom(type)) {
            if (Short.class.equals(type)) {
                return (ValueParser<T>) SHORT;
            }
            if (Integer.class.equals(type)) {
                return (ValueParser<T>) INTEGER;
            }
            if (Long.class.equals(type)) {
                return (ValueParser<T>) LONG;
            }
            if (Float.class.equals(type)) {
                return (ValueParser<T>) FLOAT;
            }
            if (Double.class.equals(type)) {
                return (ValueParser<T>) DOUBLE;
            }
            if (BigInteger.class.equals(type)) {
                return (ValueParser<T>) BIG_INTEGER;
            }
            if (BigDecimal.class.equals(type)) {
                return (ValueParser<T>) BIG_DECIMAL;
            }
        }
        return null;
    }
}
