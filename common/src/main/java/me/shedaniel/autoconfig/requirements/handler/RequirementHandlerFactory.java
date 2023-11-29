package me.shedaniel.autoconfig.requirements.handler;

import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.gui.registry.GuiLookupTable;
import me.shedaniel.autoconfig.requirements.definition.Reference;
import me.shedaniel.autoconfig.requirements.definition.RequirementDefinitionEntry;
import me.shedaniel.autoconfig.requirements.parser.ValueParser;
import me.shedaniel.clothconfig2.api.ValueHolder;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@ApiStatus.Internal
class RequirementHandlerFactory {
    
    static RequirementHandler fromDefinition(RequirementDefinitionEntry definition, GuiLookupTable guiLookupTable) {
        if (definition instanceof RequirementDefinitionEntry.Method m) {
            return fromDefinition(m, guiLookupTable);
        }
        if (definition instanceof RequirementDefinitionEntry.Field f) {
            return fromDefinition(f, guiLookupTable);
        }
        
        throw new IllegalStateException("Unsupported implementation of %s, \"%s\"".formatted(RequirementDefinitionEntry.class.getSimpleName(), definition.getClass().getSimpleName()));
    }
    
    private static RequirementHandler fromDefinition(RequirementDefinitionEntry.Method definition, GuiLookupTable guiLookupTable) {
        Method method = definition.target().method();
        List<Reference.Field> constParamArgs = getConstParamArgs(method);
        
        // Concat constParamArgs & refArgs, then map them as GUI args
        // TODO provide an option for handler to accept const args in a separate array?
        List<? extends ValueHolder<?>> guiArgs = Stream.concat(constParamArgs.stream(), Arrays.stream(definition.refArgs()))
                .map(ref -> guiLookupTable.getGui(ref.field(), ref.index()))
                .map(gui -> (ValueHolder<?>) gui)
                .toList();
        
        List<String> staticArgs = List.of(definition.staticArgs());
        
        // TODO consider allowing handler methods to annotate how they want to accept args;
        //     e.g. they might prefer an array of guiArgs and an array of staticArgs
        // method.getAnnotation(Handler.class) or definition.paramType()
        
        return new MethodRequirementHandler(method, guiArgs, staticArgs);
    }
    
    private static List<Reference.Field> getConstParamArgs(Method method) {
        List<Reference.Field> constParamArgs;
        ConfigEntry.Requirements.ConstParams constParamsDefinition = method.getAnnotation(ConfigEntry.Requirements.ConstParams.class);
        if (constParamsDefinition == null) {
            constParamArgs = Collections.emptyList();
        } else {
            // TODO cache constParamArgs somewhere
            Class<?> handlerClass = method.getDeclaringClass();
            constParamArgs = Arrays.stream(constParamsDefinition.value())
                    .map(ref -> Reference.parse(handlerClass, ref))
                    .peek(ref -> {
                        if (!(ref instanceof Reference.Field)) {
                            throw new IllegalArgumentException("ConstParams declared on method %s.%s() has a non-field reference (\"%s\")"
                                    .formatted(handlerClass.getCanonicalName(), method.getName(), ref.original()));
                        }
                    })
                    .map(Reference.Field.class::cast)
                    .toList();
        }
        return constParamArgs;
    }
    
    private static RequirementHandler fromDefinition(RequirementDefinitionEntry.Field definition, GuiLookupTable guiLookupTable) {
        ValueHolder<?> gui = guiLookupTable.getGui(definition.target().field(), definition.target().index());
        if (gui == null) {
            throw new IllegalStateException("Referenced ConfigEntry \"%s\" does not exist.".formatted(definition.target().original()));
        }
        return forGui(gui, definition.conditions());
    }
    
    private static <T> RequirementHandler forGui(ValueHolder<T> gui, String[] conditions) {
        // TODO consider allowing the user to specify comparison operators in numeric conditions
        //     Would need an ExpressionParser that evaluates to a ConditionChecker,
        //     a more complex RequirementHandler could take a list of checkers instead of values.
        
        Class<T> type = gui.getType();// FIXME field type vs gui type
        return Optional.ofNullable(ValueParser.forType(type))
                .map(parser -> simple(parser, type, gui, conditions))
                .orElseGet(() -> new GenericRequirementHandler<>(gui, conditions));
    }
    
    private static <T> RequirementHandler simple(ValueParser<T> parser, Class<T> type, ValueHolder<T> gui, String[] conditions) {
        List<T> values;
        
        // Check for special cases
        if (Boolean.class.equals(type) && conditions.length < 1) {
            //noinspection unchecked
            values = Collections.singletonList((T) (Object) true);
        } else {
            values = Arrays.stream(conditions)
                    .map(parser::parse)
                    .toList();
        }
        
        return new ConfigEntryRequirementHandler<>(gui, values);
    }
}
