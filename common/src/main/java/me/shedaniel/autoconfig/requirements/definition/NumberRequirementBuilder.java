package me.shedaniel.autoconfig.requirements.definition;

import me.shedaniel.autoconfig.requirements.builder.AbstractRequirementBuilder;
import me.shedaniel.clothconfig2.api.ValueHolder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.regex.Pattern;

public class NumberRequirementBuilder<T extends Number & Comparable<T>> extends AbstractRequirementBuilder<T> {
    
    // TODO consider allowing the user to specify comparison operators in condition strings
    //      would need to store conditions in a generic record class instead of just the raw number
    //      Something like NumberCondition(T number, Operator operator)
    private final T[] conditions;
    
    public NumberRequirementBuilder(ValueHolder<T> gui, T[] conditions, Pattern[] patterns) {
        super(gui, patterns);
        this.conditions = conditions;
    }
    
    @Override
    protected T[] conditions() {
        return conditions;
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends Number> NumberRequirementBuilder<?> from(Class<T> type, ValueHolder<T> gui, String[] conditions, Pattern[] patterns) {
        if (Short.class.isAssignableFrom(type)) {
            Short[] numbers = Arrays.stream(conditions)
                    .map(Short::valueOf)
                    .toArray(Short[]::new);
            return new NumberRequirementBuilder<>((ValueHolder<Short>) gui, numbers, patterns);
        }
        if (Integer.class.isAssignableFrom(type)) {
            Integer[] numbers = Arrays.stream(conditions)
                    .map(Integer::valueOf)
                    .toArray(Integer[]::new);
            return new NumberRequirementBuilder<>((ValueHolder<Integer>) gui, numbers, patterns);
        }
        if (Long.class.isAssignableFrom(type)) {
            Long[] numbers = Arrays.stream(conditions)
                    .map(Long::valueOf)
                    .toArray(Long[]::new);
            return new NumberRequirementBuilder<>((ValueHolder<Long>) gui, numbers, patterns);
        }
        if (Float.class.isAssignableFrom(type)) {
            Float[] numbers = Arrays.stream(conditions)
                    .map(Float::valueOf)
                    .toArray(Float[]::new);
            return new NumberRequirementBuilder<>((ValueHolder<Float>) gui, numbers, patterns);
        }
        if (Double.class.isAssignableFrom(type)) {
            Double[] numbers = Arrays.stream(conditions)
                    .map(Double::valueOf)
                    .toArray(Double[]::new);
            return new NumberRequirementBuilder<>((ValueHolder<Double>) gui, numbers, patterns);
        }
        if (BigInteger.class.isAssignableFrom(type)) {
            BigInteger[] numbers = Arrays.stream(conditions)
                    .map(BigInteger::new)
                    .toArray(BigInteger[]::new);
            return new NumberRequirementBuilder<>((ValueHolder<BigInteger>) gui, numbers, patterns);
        }
        if (BigDecimal.class.isAssignableFrom(type)) {
            BigDecimal[] numbers = Arrays.stream(conditions)
                    .map(BigDecimal::new)
                    .toArray(BigDecimal[]::new);
            return new NumberRequirementBuilder<>((ValueHolder<BigDecimal>) gui, numbers, patterns);
        }
        throw new IllegalArgumentException("Unsupported number type `%s` passed to %s.from()"
                .formatted(type.getCanonicalName(), NumberRequirementBuilder.class.getSimpleName()));
    }
}
