package me.shedaniel.autoconfig.gui.registry;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
@ApiStatus.Internal
public class GuiLookupTable {
    
    private final Map<Field, List<AbstractConfigListEntry>> registered = new HashMap<>();
    
    public List<AbstractConfigListEntry> getGuis(Field field) {
        return registered.getOrDefault(field, Collections.emptyList());
    }
    
    public @Nullable AbstractConfigListEntry getGui(Field field, int index) {
        List<AbstractConfigListEntry> guis = getGuis(field);
        
        if (guis == null || guis.isEmpty()) {
            return null;
        }
        
        // Negative index should be relative to end of array
        int i = index < 0 ? guis.size() + index : index;
        
        if (i < 0 || i >= guis.size()) {
            throw new IndexOutOfBoundsException("Index %d is out of bounds (expected between 0 and %d)"
                    .formatted(i, guis.size() - 1));
        }
        
        return guis.get(i);
    }
    
    public void register(Field field, @Nullable AbstractConfigListEntry ...guis) {
        if (guis == null) {
            registered.remove(field);
            return;
        }
        
        register(field, List.of(guis));
    }
    
    public void register(Field field, @Nullable List<AbstractConfigListEntry> guis) {
        if (guis == null) {
            registered.remove(field);
            return;
        }
        
        registered.put(field, Collections.unmodifiableList(guis));
    }
    
    /**
     * @deprecated for debugging purposes only
     */
    @Deprecated
    public Map<Field, List<AbstractConfigListEntry>> getTable() {
        return registered;
    }
}
