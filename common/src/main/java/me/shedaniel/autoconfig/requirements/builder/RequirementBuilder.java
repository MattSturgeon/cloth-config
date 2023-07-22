package me.shedaniel.autoconfig.requirements.builder;

import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.api.ValueHolder;
import org.jetbrains.annotations.ApiStatus;

import java.util.regex.Pattern;

@ApiStatus.Internal
public interface RequirementBuilder<T> {
    
    Requirement build();
    
    @SuppressWarnings("unchecked")
    static RequirementBuilder<?> builder(ValueHolder<?> gui, String[] conditions, Pattern[] patterns) {
        Class<?> type = gui.getType();
        if (Boolean.class.equals(type)) {
            return new BooleanRequirementBuilder((ValueHolder<Boolean>) gui, conditions, patterns);
        }
        if (Enum.class.isAssignableFrom(type)) {
            return new EnumRequirementBuilder<>((Class<Enum<?>>) type, (ValueHolder<Enum<?>>) gui, conditions, patterns);
        }
        if (Number.class.isAssignableFrom(type)) {
            return NumberRequirementBuilder.from((Class<Number>) type, (ValueHolder<Number>) gui, conditions, patterns);
        }
        if (String.class.equals(type)) {
            return new StringRequirementBuilder((ValueHolder<String>) gui, conditions, patterns);
        }
        return new GenericRequirementBuilder<>((ValueHolder<Object>) gui, conditions, patterns);
    }
}
