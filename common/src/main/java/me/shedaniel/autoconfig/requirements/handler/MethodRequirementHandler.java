package me.shedaniel.autoconfig.requirements.handler;

import joptsimple.internal.Strings;
import me.shedaniel.autoconfig.requirements.parser.ValueParser;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.clothconfig2.api.ValueHolder;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ApiStatus.Internal
class MethodRequirementHandler implements RequirementHandler {
    private final Method method;
    
    private final List<ValueHolder<?>> argSuppliers;
    private final @Nullable List<ValueHolder<?>> varArgSuppliers;
    private final boolean hasVarArg;
    private final int normalArgCount;
    
    public MethodRequirementHandler(Method method, List<? extends ValueHolder<?>> guiArgs, List<String> staticArgs) {
        this.method = method;
       this.method.setAccessible(true);
        validateMethodSignature();
        
        this.hasVarArg = method.isVarArgs();
        this.normalArgCount = hasVarArg ?
                this.method.getParameterCount() - 1 : this.method.getParameterCount();
        
        int guiArgCount = guiArgs.size();
        int staticArgCount = staticArgs.size();
        int argCount = guiArgCount + staticArgCount;
        validateArgCount(argCount);
        
        // Handle generic types
        List<Parameter> parameters = List.of(method.getParameters());
        Map<TypeVariable<?>, Class<?>> generics = new HashMap<>(normalArgCount);
        
        // Populate resolvedGenerics with GUI types associated with generic parameters
        IntStream.range(0, Math.min(guiArgCount, normalArgCount))
                .forEach(i -> {
                    Parameter parameter = parameters.get(i);
                    if (parameter.getParameterizedType() instanceof TypeVariable<?> typeVariable) {
                        Class<?> type = guiArgs.get(i).getType();
                        generics.compute(typeVariable, (key, prev) -> {
                            Class<?> typeBoxed = Utils.box(type);
                            
                            // Validate that type is within bounds
                            Arrays.stream(typeVariable.getBounds())
                                    .map(bound -> {
                                        if (!(bound instanceof Class<?>)) {
                                            throw new IllegalStateException("Unknown bound type " + bound.getClass().getSimpleName());
                                        }
                                        return (Class<?>) bound;
                                    })
                                    .filter(bound -> !bound.isAssignableFrom(typeBoxed))
                                    .forEach(bound -> {
                                        throw new IllegalArgumentException("Incorrect type used for <%s>. %s is not assignable from %s."
                                                .formatted(typeVariable.getName(), type.getSimpleName(), bound.getSimpleName()));
                                    });

                            if (prev == null) {
                                return type;
                            }
                            
                            Class<?> prevBoxed = Utils.box(prev);
                            if (Objects.equals(typeBoxed, prevBoxed)) {
                                return prev;
                            }
                            if (prevBoxed.isAssignableFrom(typeBoxed)) {
                                return prev;
                            }
                            if (typeBoxed.isAssignableFrom(prevBoxed)) {
                                return type;
                            }
                            throw new IllegalArgumentException("Incompatible types used for %s (%s & %s)"
                                    .formatted(typeVariable.getName(), prev.getSimpleName(), type.getSimpleName()));
                        });
                    }
                });
        
        // A hot mess of spaghetti that attempts to evaluate the varArgs type TODO cleanup/refactor
        Class<?> varArg = null;
        if (hasVarArg) {
            TypeVariable<?> unresolved = null;
            
            // Get the varArg param
            Parameter param = getVarArgType(method);
            if (param == null) {
                throw new IllegalStateException("Method is varArg, but varArg is null!");
            }
            
            // Check for a generic type and try to resolve it
            if (param.getParameterizedType() instanceof GenericArrayType arrayType) {
                if (!(arrayType.getGenericComponentType() instanceof TypeVariable<?> generic)) {
                    throw new IllegalStateException("GenericArrayType doesn't have a TypeVariable!");
                }
                Class<?> resolved = generics.get(generic);
                if (resolved == null) {
                    // varArg is generic, but we were unable to resolve a match
                    unresolved = generic;
                } else {
                    // Found a match!
                    varArg = resolved;
                }
            }
            
            // If we haven't found the type yet
            if (varArg == null) {
                Class<?> bound = param.getType().componentType();
                varArg = bound; // Fallback
                
                // If we have gui entries in the varArgs, we can use their type
                if (normalArgCount < guiArgCount) {
                    varArg = guiArgs.subList(normalArgCount, guiArgCount)
                            .stream()
                            .map(ValueHolder::getType)
                            .reduce((prev, type) -> {
                                Class<?> prevBoxed = Utils.box(prev);
                                Class<?> typeBoxed = Utils.box(type);
                                
                                // Check type is within bounds
                                if (!bound.isAssignableFrom(typeBoxed)) {
                                    throw new IllegalArgumentException("Incorrect type used in varargs. %s is not assignable from %s."
                                            .formatted(type.getSimpleName(), bound.getSimpleName()));
                                }
                                
                                // Reduce to common superclass
                                if (Objects.equals(typeBoxed, prevBoxed)) {
                                    return prev;
                                }
                                if (prevBoxed.isAssignableFrom(typeBoxed)) {
                                    return prev;
                                }
                                if (typeBoxed.isAssignableFrom(prevBoxed)) {
                                    return type;
                                }
                                throw new IllegalArgumentException("Incompatible types used in varargs (%s & %s)"
                                        .formatted(prev.getSimpleName(), type.getSimpleName()));
                            })
                            .orElseThrow(() -> new IllegalStateException("Impossible state - gui varArgs resolved to nothing!"));
                }
            }
            
            // If varArgs is generic and unresolved, register it as resolved
            if (unresolved != null) {
                if (varArg == null) {
                    LogManager.getLogger().warn("Unable to resolve varArg generic <{}>", unresolved.getName());
                } else {
                    generics.put(unresolved, varArg);
                }
            }
        }
        
        List<Class<?>> paramClasses = this.getParamClasses(argCount, generics, varArg);
        List<? extends ValueHolder<?>> parsedStaticArgs = parseStaticArgs(staticArgs, paramClasses.subList(guiArgCount, paramClasses.size()));
        List<ValueHolder<?>> args = Stream.concat(guiArgs.stream(), parsedStaticArgs.stream()).toList();
        
        this.argSuppliers = args.subList(0, normalArgCount);
        this.varArgSuppliers = hasVarArg ? args.subList(normalArgCount, argCount) : null;
        this.validateTypes(args, paramClasses);
    }
    
    private static Parameter getVarArgType(Method method) {
        int paramCount = method.getParameterCount();
        Parameter[] parameters = method.getParameters();
        if (paramCount > 0) {
            Parameter param = parameters[paramCount - 1];
            if (param.isVarArgs()) {
                return param;
            }
        }
        return null;
    }
    
    private List<? extends ValueHolder<?>> parseStaticArgs(List<String> staticArgs, List<Class<?>> paramClasses) {
        // Sanity check
        if (staticArgs.size() != paramClasses.size()) {
            this.throwError("Internal math error: staticArgs and paramClasses are different sizes!");
        }
        
        return IntStream.range(0, staticArgs.size())
                .mapToObj(i -> {
                    String arg = staticArgs.get(i);
                    Class<?> type = paramClasses.get(i);
                    return Optional.ofNullable(ValueParser.forType(type))
                            .map(parser -> parser.parse(arg))
                            .map(ValueHolder::of)
                            .orElseThrow(() -> new IllegalArgumentException("[param %d]: Cannot parse unsupported type %s on %s.%s()"
                                    .formatted(i, type.getCanonicalName(), method.getDeclaringClass().getSimpleName(), method.getName())));
                })
                .toList();
    }
    
    /**
     * Runs the handler method, passing it the current value of each target
     */
    @Override
    public boolean run() {
        Object[] args = Stream.concat(
                // Stream of normal args
                argSuppliers.stream().map(ValueHolder::getValue),
                // Append optional varargs array
                Stream.ofNullable(varArgSuppliers)
                        .map(suppliers -> suppliers.stream().map(ValueHolder::getValue).toArray())
        ).toArray();
        
        try {
            return (Boolean) method.invoke(null, args);
        } catch (Throwable t) {
            throw new RuntimeException("Exception thrown while handling requirement using %s#%s"
                    .formatted(method.getDeclaringClass().getCanonicalName(), method.getName()), t);
        }
    }
    
    private void throwError(String error) {
        throwErrors(Collections.singletonList(error));
    }
    
    private void throwErrors(List<String> errors) {
        String message = Stream.concat(
                        Stream.of("Invalid Config Entry or handler %s#%s: (%d errors)"
                                .formatted(method.getDeclaringClass().getCanonicalName(), method.getName(), errors.size())),
                        errors.stream()
                                .map(error -> "    " + error))
                .collect(Collectors.joining("\n"));
        
        // TODO custom exception class
        throw new RuntimeException(message);
    }
    
    /**
     * @throws RuntimeException if the method cannot be used as a handler function because of its method signature.
     */
    private void validateMethodSignature() {
        List<String> errors = new ArrayList<>(5);
        
        Class<?> returnType = method.getReturnType();
        if (!Utils.typesCompatible(returnType, Boolean.TYPE)) {
            errors.add("Unexpected return type on %s#%s: expected `boolean` but found %s\n"
                    .formatted(method.getDeclaringClass().getCanonicalName(), method.getName(), returnType));
        }
        
        // TODO can we handle instance methods too? How would we find/store the object instance?
        if (!Modifier.isStatic(method.getModifiers())) {
            errors.add("Unexpected instance method %s#%s: Only static methods are supported\n"
                    .formatted(method.getDeclaringClass().getCanonicalName(), method.getName()));
        }
        
        // Ensure no exceptions are declared on the handler
        List<String> exceptions = Arrays.stream(method.getExceptionTypes())
                .map(Class::getCanonicalName)
                .toList();
        if (!exceptions.isEmpty()) {
            errors.add("Unexpected exceptions declared on %s#%s\n"
                                               .formatted(method.getDeclaringClass().getCanonicalName(), method.getName())
                                       + Strings.join(exceptions, "\n"));
        }
        
        if (!errors.isEmpty()) {
            throwErrors(errors);
        }
    }
    
    private void validateArgCount(int argCount) {
        if (hasVarArg) {
            if (argCount < normalArgCount) {
                throwError("Insufficient argument count: expected at least %d, found %d"
                        .formatted(normalArgCount, argCount));
            }
        } else {
            if (argCount != normalArgCount) {
                throwError("Incorrect argument count: expected %d, found %d"
                        .formatted(normalArgCount, argCount));
            }
        }
    }
    
    private List<Class<?>> getParamClasses(int argCount, Map<TypeVariable<?>, Class<?>> generics, Class<?> varArgType) {
        Stream<Class<?>> normalParamTypes = List.of(this.method.getParameters())
                .subList(0, normalArgCount)
                .stream()
                .map(parameter -> {
                    if (parameter.getParameterizedType() instanceof TypeVariable<?> typeVariable) {
                        Class<?> generic = generics.get(typeVariable);
                        if (generic == null) {
                            LogManager.getLogger().warn("Unable to resolve generic <{}>", typeVariable.getName());
                        } else {
                            return generic;
                        }
                    }
                    return parameter.getType();
                });
        
        Stream<Class<?>> varArgTypes = IntStream.range(normalArgCount, argCount).mapToObj(i -> varArgType);
                
        return Stream.concat(normalParamTypes, varArgTypes).toList();
    }
    
    private List<Class<?>> getArgClasses(List<ValueHolder<?>> args) {
        return args.stream()
                .map(ValueHolder::getType)
                .collect(Collectors.toList());
    }
    
    private void validateTypes(List<ValueHolder<?>> args, List<Class<?>> paramClasses) {
        int argCount = args.size();
        List<Class<?>> argClasses = getArgClasses(args);
        
        List<String> errors = IntStream.range(0, argCount)
                .mapToObj(i -> validateType(i, paramClasses.get(i), argClasses.get(i)))
                .filter(Objects::nonNull)
                .toList();
        
        if (!errors.isEmpty()) {
            throwErrors(errors);
        }
    }
    
    private static @Nullable String validateType(int i, Class<?> param, Class<?> arg) {
        return Utils.typesCompatible(arg, param) ? null : "[%d]: Expected `%s` found `%s`".formatted(i, param.getSimpleName(), arg.getSimpleName());
    }
    
    private record VarArg(Class<?> paramClass, Type type) {}
}
