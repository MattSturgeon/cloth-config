package me.shedaniel.autoconfig.dependencies;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Dependency.EnableIf;
import me.shedaniel.autoconfig.util.RelativeRefParser;
import me.shedaniel.clothconfig2.api.ConfigEntry;
import me.shedaniel.clothconfig2.api.dependencies.conditions.ComparisonOperator;
import me.shedaniel.clothconfig2.api.dependencies.conditions.ConditionFlag;
import me.shedaniel.clothconfig2.api.dependencies.conditions.MatcherCondition;
import me.shedaniel.clothconfig2.impl.dependencies.conditions.ComparativeMatcherCondition;
import me.shedaniel.clothconfig2.impl.dependencies.conditions.GenericMatcherCondition;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.EnumSet;

record MatcherConditionDefinition(EnumSet<ConditionFlag> flags, @Nullable ComparisonOperator operator, Field referencedField) {
    
    /**
     * @see StaticConditionDefinition
     * @see EnableIf#matching() Public API documentation
     */
    static MatcherConditionDefinition fromConditionString(Class<? extends ConfigData> configClass, String condition) {
        // Parse any flags
        StaticConditionDefinition definition = StaticConditionDefinition.fromConditionString(condition)
                .map(String::strip);
    
        // Parse any operators
        ComparisonOperator operator = ComparisonOperator.startsWith(definition.condition());
        if (operator != null)
            definition = definition
                    .map(string -> string.substring(operator.toString().length()))
                    .map(String::stripLeading);
    
        // Parse the i18n reference
        Field key = RelativeRefParser.getField(configClass, definition.condition());
    
        return new MatcherConditionDefinition(definition.flags(), operator, key);
    }
    
    <T> MatcherCondition<T> toMatcher(ConfigEntry<T> gui) {
        GenericMatcherCondition<T> matcher = new GenericMatcherCondition<>(gui);
        matcher.setFlags(this.flags());
        return matcher;
    }
    
    <T extends Comparable<T>> ComparativeMatcherCondition<T> toComparativeMatcher(ConfigEntry<T> gui) {
        ComparativeMatcherCondition<T> matcher = new ComparativeMatcherCondition<>(this.operator(), gui);
        matcher.setFlags(this.flags());
        return matcher;
    }
}
