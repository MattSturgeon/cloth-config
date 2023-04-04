package me.shedaniel.autoconfig.dependencies;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Dependency.EnableIf;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Dependency.EnableIfGroup;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Dependency.ShowIf;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Dependency.ShowIfGroup;
import me.shedaniel.autoconfig.util.RelativeRefParser;
import me.shedaniel.clothconfig2.api.ConfigEntry;
import me.shedaniel.clothconfig2.api.dependencies.Dependency;
import me.shedaniel.clothconfig2.impl.dependencies.DependencyGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code DependencyManager} is used by {@link me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess GuiRegistryAccess} implementations
 * to handle generating dependencies defined in supported annotations.
 * <br><br>
 * It is necessary to register <strong>all</strong> config entry GUIs with the {@code DependencyManager} instance before building any dependencies.
 *
 * @see EnableIf @EnableIf
 * @see EnableIfGroup @EnableIfGroup
 * @see ShowIf @ShowIf
 * @see ShowIfGroup @ShowIfGroup
 * @see Dependency
 * @see me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess GuiRegistryAccess
 * @see DependencyManagerAccess
 */
public class DependencyManager {
    
    private final Map<Field, EntryRecord> registry = new LinkedHashMap<>();
    
    private Class<? extends ConfigData> configClass = null;
    
    public DependencyManager() {}
    
    /**
     * Define a prefix which {@link #register(ConfigEntry, Field) register()} will add
     * to i18n keys if they don't already start with it.
     * <br><br>
     * Should normally be set to the i18n key of the root {@link me.shedaniel.autoconfig.annotation.Config @Config}
     * class.
     *
     * @param configClass the i18n prefix
     */
    public void setConfigClass(@Nullable Class<? extends ConfigData> configClass) {
        this.configClass = configClass;
    }
    
    /**
     * Get the config entry GUI associated with the given i18n key.
     * <p>
     * Will look for an entry exactly matching {@code i18n}, otherwise will look again, prefixing {@code i18n} with
     * the prefix defined using {@link #setConfigClass(Class)}.
     *
     * @param field the i18n key
     * @return The matching config entry
     * @throws IllegalArgumentException if a matching config entry is not found
     */
    public ConfigEntry<?> getEntry(@NotNull Field field) throws IllegalArgumentException {
        return Optional.ofNullable(registry.get(field))
                .map(EntryRecord::gui)
                .orElseThrow(() -> new IllegalArgumentException("Specified config entry not found: \"%s\"".formatted(field)));
    }
    
    /**
     * Gets the config entry GUI associated with the given i18n key, so long as the config entry can handle the
     * provided type.
     *
     * @param <T>  the type the GUI must support
     * @param type a {@code Class} representing type {@code <T>}
     * @param field the field that declared the GUI
     * @return The matching config entry
     * @throws IllegalArgumentException if a matching config entry supporting type {@code <T>} is not found
     */
    public <T> ConfigEntry<T> getEntry(Class<T> type, Field field) throws IllegalArgumentException {
        ConfigEntry<?> entry = getEntry(field);
        
        // Entry's type must extend from the provided type 
        if (!type.isAssignableFrom(entry.getType()))
            throw new IllegalArgumentException("Specified config entry does not support the required type. Found %s, required %s for \"%s\"."
                    .formatted(entry.getType().getSimpleName(), type.getSimpleName(), RelativeRefParser.getReference(field)));
    
        // If type is assignable, we can safely cast to <T>
        @SuppressWarnings("unchecked") ConfigEntry<T> tEntry = (ConfigEntry<T>) entry;
        return tEntry;
    }
    
    /**
     * Register a new or transformed config entry for later use by {@link #build()}.
     *
     * @param entry the {@link me.shedaniel.autoconfig.annotation.ConfigEntry config entry} GUI to be registered
     * @param field a {@link Field} which may have dependency annotations present
     * @see #build()
     */
    public void register(ConfigEntry<?> entry, Field field) {
        Set<EnableIf> enableIfs = null;
        Set<EnableIfGroup> enableIfGroups = null;
        Set<ShowIf> showIfs = null;
        Set<ShowIfGroup> showIfGroups = null;
        
        if (field != null) {
            if (field.isAnnotationPresent(EnableIf.class))
                enableIfs = Collections.singleton(field.getAnnotation(EnableIf.class));
            if (field.isAnnotationPresent(EnableIfGroup.class))
                enableIfGroups = Collections.singleton(field.getAnnotation(EnableIfGroup.class));
            if (field.isAnnotationPresent(ShowIf.class))
                showIfs = Collections.singleton(field.getAnnotation(ShowIf.class));
            if (field.isAnnotationPresent(ShowIfGroup.class))
                showIfGroups = Collections.singleton(field.getAnnotation(ShowIfGroup.class));
        }
    
        register(entry, field, enableIfs, enableIfGroups, showIfs, showIfGroups);
    }
    
    /**
     * Register a new or transformed config entry for later use by {@link #build()}.
     * <br><br>
     * Explicitly provide one or more {@link EnableIf @EnableIf} or {@link EnableIfGroup @EnableIfGroup}
     * annotations to be applied to this entry.
     *
     * @param entry                    the config entry GUI
     * @param field                    a {@link Field} to be used as the reference key for this config entry
     * @param enableIfAnnotations      a {@link Collection} of {@link EnableIf @EnableIf} annotations
     * @param enableIfGroupAnnotations a {@link Collection} of {@link EnableIfGroup @EnableIfGroup} annotations
     * @param showIfAnnotations        a {@link Collection} of {@link ShowIf @ShowIf} annotations
     * @param showIfGroupAnnotations   a {@link Collection} of {@link ShowIfGroup @ShowIfGroup} annotations
     * @see #register(ConfigEntry, Field)
     * @see #build()
     */
    public void register(
            ConfigEntry<?> entry,
            Field field,
            @Nullable Collection<EnableIf> enableIfAnnotations,
            @Nullable Collection<EnableIfGroup> enableIfGroupAnnotations,
            @Nullable Collection<ShowIf> showIfAnnotations,
            @Nullable Collection<ShowIfGroup> showIfGroupAnnotations)
    {
        Field key = entry.getOrSetDeclaringField(field);
    
        // Get the already defined sets or create new ones
        Optional<EntryRecord> optional = Optional.ofNullable(registry.get(key));
        
        Set<DependencyDefinition> enableIfs = optional
                .map(EntryRecord::enableIfDependencies)
                .orElseGet(LinkedHashSet::new);
        
        Set<DependencyGroupDefinition> enableIfGroups = optional
                .map(EntryRecord::enableIfGroups)
                .orElseGet(LinkedHashSet::new);
        
        Set<DependencyDefinition> showIfs = optional
                .map(EntryRecord::showIfDependencies)
                .orElseGet(LinkedHashSet::new);
        
        Set<DependencyGroupDefinition> showIfGroups = optional
                .map(EntryRecord::showIfGroups)
                .orElseGet(LinkedHashSet::new);
    
        // Add the new dependencies to the appropriate sets
        enableIfs.addAll(Stream.ofNullable(enableIfAnnotations)
                .flatMap(Collection::stream)
                .map(single -> new DependencyDefinition(configClass, single))
                .collect(Collectors.toUnmodifiableSet()));
    
        enableIfGroups.addAll(Stream.ofNullable(enableIfGroupAnnotations)
                .flatMap(Collection::stream)
                .map(group -> new DependencyGroupDefinition(configClass, group))
                .collect(Collectors.toUnmodifiableSet()));
        
        showIfs.addAll(Stream.ofNullable(showIfAnnotations)
                .flatMap(Collection::stream)
                .map(single -> new DependencyDefinition(configClass, single))
                .collect(Collectors.toUnmodifiableSet()));
    
        showIfGroups.addAll(Stream.ofNullable(showIfGroupAnnotations)
                .flatMap(Collection::stream)
                .map(group -> new DependencyGroupDefinition(configClass, group))
                .collect(Collectors.toUnmodifiableSet()));
        
        registry.put(key, new EntryRecord(entry, enableIfs, enableIfGroups, showIfs, showIfGroups));
    }
    
    /**
     * Builds the dependencies for all registered config entries.
     * <br><br>
     * <strong>All config entries</strong> must be registered <strong>before</strong> running this method.
     * <br><br>
     * If a field has multiple dependencies declared, they will be combined using {@link #combineDependencies(Collection, Collection) combineDependencies()}.
     * 
     * @see #register(ConfigEntry, Field)
     */
    public void build() {
        registry.values().stream()
                .filter(EntryRecord::hasDependencies)
                .forEach(entry -> {
                    // Combine the dependencies, then add the result to the config entry
                    
                    // "enable if" dependencies:
                    Optional.ofNullable(combineDependencies(entry.enableIfDependencies(), entry.enableIfGroups()))
                            .ifPresent(entry.gui()::setEnableIfDependency);
                    
                    // "show if" dependencies:
                    Optional.ofNullable(combineDependencies(entry.showIfDependencies(), entry.showIfGroups()))
                            .ifPresent(entry.gui()::setShowIfDependency);
                });
    }
    
    /**
     * <ul>
     *     <li>If only one dependency is provided, that dependency is built and returned.</li>
     *     <li>If multiple dependencies are provided, they are combined into a {@link DependencyGroup} requiring all of them.</li>
     * </ul>
     * 
     * @param dependencies a {@link Collection} of "enable if" {@link DependencyDefinition}s
     * @param dependencyGroups a {@link Collection} of "enable if" {@link DependencyGroupDefinition}s
     * @return a single {@link Dependency} or {@link DependencyGroup} representing all dependencies provided,
     * or {@code null} if {@code dependencies} is empty
     */
    private @Nullable Dependency combineDependencies(@NotNull Collection<DependencyDefinition> dependencies, @NotNull Collection<DependencyGroupDefinition> dependencyGroups)
    {
        // Build each dependency
        List<Dependency> singles = dependencies.stream()
                .map(single -> single.build(this))
                .toList();
        List<Dependency> groups = dependencyGroups.stream()
                .map(group -> group.build(this))
                .collect(Collectors.toCollection(ArrayList::new));
    
        // Combine multiple dependencies if necessary,
        // add the result to the groups list
        if (!singles.isEmpty())
            groups.add(singles.size() == 1 ? singles.get(0) : Dependency.builder().startGroup().withChildren(singles).build());
    
        // Filter duplicates before checking quantities
        List<Dependency> children = groups.stream().distinct().toList();
        
        // Don't build a group if we only have one dependency
        if (children.isEmpty())
            return null;
        else if (children.size() == 1)
            return children.get(0);
        
        // Return a group that depends on all dependencies & groups
        // Filtered to remove any duplicates
        return Dependency.builder()
                .startGroup()
                .withChildren(children)
                .build();
    }
    
    /**
     * Return true if a supported dependency annotation is <em>present</em> on the provided field.
     *
     * @param field the field to check
     * @return whether the provided field has dependency annotations present
     */
    public static boolean hasDependencyAnnotation(Field field) {
        return field.isAnnotationPresent(EnableIf.class)
               || field.isAnnotationPresent(EnableIfGroup.class)
               || field.isAnnotationPresent(ShowIf.class)
               || field.isAnnotationPresent(ShowIfGroup.class);
    }
    
}
