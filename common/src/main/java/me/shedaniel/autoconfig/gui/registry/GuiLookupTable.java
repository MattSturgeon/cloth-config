package me.shedaniel.autoconfig.gui.registry;

import me.shedaniel.autoconfig.requirements.definition.Reference;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class GuiLookupTable {
    
    private final Map<String, List<AbstractConfigListEntry>> registered = new HashMap<>();
    
    public AbstractConfigListEntry getGui(Reference reference) {
        List<AbstractConfigListEntry> guis = getGuis(reference);
        
        // Negative index should be relative to end of array
        int index = reference.index() < 0
                ? guis.size() + reference.index()
                : reference.index();
        
        if (index < 0 || index >= guis.size()) {
            // out of bounds
            throw new IndexOutOfBoundsException("Index %d in reference \"%s\" is out of bounds (expected between 0 and %d)"
                    .formatted(index, reference.original(), guis.size() - 1));
        }
        
        return guis.get(index);
    }
    
    public List<AbstractConfigListEntry> getGuis(Reference reference) {
        // First try a package-relative ref
        List<AbstractConfigListEntry> guis = registered.get(relKeyOf(reference));
        if (guis != null) {
            return guis;
        }
        
        // Then try a canonical ref
        return registered.get(keyOf(reference));
    }
    
    public List<AbstractConfigListEntry> getGuis(Field field) {
        return registered.getOrDefault(keyOf(field), Collections.emptyList());
    }
    
    public void register(Field field, @Nullable AbstractConfigListEntry ...guis) {
        if (guis == null) {
            registered.remove(keyOf(field));
            return;
        }
        
        register(field, List.of(guis));
    }
    
    public void register(Field field, @Nullable List<AbstractConfigListEntry> guis) {
        if (guis == null) {
            registered.remove(keyOf(field));
            return;
        }
        
        registered.put(keyOf(field), Collections.unmodifiableList(guis));
    }
    
    /**
     * @deprecated intended for debugging purposes only
     */
    @Deprecated
    public Map<String, List<AbstractConfigListEntry>> getTable() {
        return registered;
    }
    
    // TODO move these methods to the Reference class?
    private static String keyOf(Reference reference) {
        return reference.classRef() + "#" + reference.memberRef();
    }
    
    private static String relKeyOf(Reference reference) {
        return reference.basePackage() + "." + reference.classRef() + "#" + reference.memberRef();
    }
    
    private static String keyOf(Field field) {
        return field.getDeclaringClass().getCanonicalName() + "#" + field.getName();
    }
}
