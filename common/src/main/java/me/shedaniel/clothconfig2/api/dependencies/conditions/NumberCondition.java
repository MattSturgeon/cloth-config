package me.shedaniel.clothconfig2.api.dependencies.conditions;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Optional;
import java.util.regex.Pattern;

public class NumberCondition<T extends Number & Comparable<T>> extends StaticCondition<T> {

    private static final Pattern WHITESPACE = Pattern.compile("\\s");
    
    private final ComparisonOperator operator;
    private final boolean integer;
    
    private int formatPrecision = -1;
    
    public NumberCondition(T value) {
        this(ComparisonOperator.EQUAL, value);
    }

    public NumberCondition(ComparisonOperator operator, T value) {
        super(value);
        this.operator = operator;
        
        Class<? extends Number> type = value.getClass();
        this.integer = type.isAssignableFrom(Long.class) || type.isAssignableFrom(Integer.class) || type.isAssignableFrom(Short.class);
    }
    
    /**
     * Build a {@code NumberCondition} represented by the given {@code condition} string.
     * <br><br>
     * The {@code type} parameter defines which number class is being dealt with. Currently, only the following
     * types are supported:
     * 
     * <ul>
     *     <li>{@link Long}</li>
     *     <li>{@link Integer}</li>
     *     <li>{@link Short}</li>
     *     <li>{@link Double}</li>
     *     <li>{@link Float}</li>
     * </ul>
     * 
     * The condition string can optionally begin with a mathematical {@link ComparisonOperator comparison operator}.
     * For example, the condition <em>"{@code >10}"</em> is true when the depended-on entry's value is greater than 10.
     * 
     * <br><br>
     * 
     * Supported operators include:
     * <ul>
     *     <li>'{@code ==}' <em>equal to</em></li>
     *     <li>'{@code !=}' <em>not equal to</em></li>
     *     <li>'{@code >}' <em>greater than</em></li>
     *     <li>'{@code >=}' <em>greater than or equal to</em></li>
     *     <li>'{@code <}' <em>less than</em></li>
     *     <li>'{@code <=}' <em>less than or equal to</em></li>
     * </ul>
     * 
     * Any whitespace in the condition string will be completely ignored.
     * 
     * @param type a class defining what type of number the {@code NumberCondition} will handle
     * @param condition a string that can be parsed into type {@code T}, optionally prefixed with a {@link ComparisonOperator comparison operator} 
     * @param <T> the type of {@link Number} the {@code NumberCondition} will deal with, must be {@link Comparable} with
     *           other instances of type {@code T}
     * @return a {@code NumberCondition} representing the given {@code condition}
     * @throws NumberFormatException if the {@code condition} string cannot be parsed into type {@code T}
     * @throws IllegalArgumentException if the given {@code type} ({@code T}) is not supported
     * @see ComparisonOperator
     */
    public static <T extends Number & Comparable<T>> @NotNull NumberCondition<T> fromString(@NotNull Class<T> type, @NotNull String condition) throws IllegalArgumentException {
        String stripped = WHITESPACE.matcher(condition).replaceAll("");
        Optional<ComparisonOperator> optional = Optional.ofNullable(ComparisonOperator.startsWith(stripped));
        
        ComparisonOperator operator;
        String numberPart;
        if (optional.isPresent()) {
            operator = optional.get();
            numberPart = stripped.substring(operator.toString().length());
        } else {
            operator = ComparisonOperator.EQUAL;
            numberPart = stripped;
        }
        
        T number;
        if (type.isAssignableFrom(Long.class))
            number = type.cast(Long.parseLong(numberPart));
        else if (type.isAssignableFrom(Integer.class))
            number = type.cast(Integer.parseInt(numberPart));
        else if (type.isAssignableFrom(Short.class))
            number = type.cast(Short.parseShort(numberPart));
        else if (type.isAssignableFrom(Double.class))
            number = type.cast(Double.parseDouble(numberPart));
        else if (type.isAssignableFrom(Float.class))
            number = type.cast(Float.parseFloat(numberPart));
        else
            throw new IllegalArgumentException("Unsupported Number type \"%s\"".formatted(type.getSimpleName()));

        return new NumberCondition<>(operator, number);
    }
    
    @Override
    protected boolean matches(T value) {
        return operator.compare(value, getValue());
    }
    
    public void setFormatPrecision(int places) {
        formatPrecision = places;
    }
    
    private int formatPrecision() {
        // if -1, calculate an appropriate precision for the scale of the number
        if (formatPrecision < 0) {
            long l = Math.abs(getValue().longValue());
            if (l == 0) {
                double d = Math.abs(getValue().doubleValue());
                if (d == 0)
                    return 0;
                if (d < 0.01)
                    return 4;
                if (d < 0.1)
                    return 3;
                return 2;
            }
            if (l > 99)
                return 0;
            if (l > 9)
                return 1;
            return 2;
        }
        return formatPrecision;
    }
    
    @Override
    protected Component getTextInternal() {
        MutableComponent condition = Component.translatable(
                "text.cloth-config.dependencies.conditions.%s".formatted(this.operator.name().toLowerCase()),
                integer ? getValue().toString() : formatDouble(getValue().doubleValue(), formatPrecision()));
        if (inverted())
            condition = Component.translatable("not", condition);
        return condition;
    }
    
    private static String formatDouble(double value, int places) {
        BigDecimal bigDecimal = new BigDecimal(value)
                .setScale(places, RoundingMode.HALF_UP);
        return NumberFormat.getInstance().format(bigDecimal);
    }
}
