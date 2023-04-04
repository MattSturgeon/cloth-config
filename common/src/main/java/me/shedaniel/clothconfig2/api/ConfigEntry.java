package me.shedaniel.clothconfig2.api;

import me.shedaniel.clothconfig2.api.dependencies.Dependency;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public interface ConfigEntry<T> {
    
    T getValue();
    
    /**
     * Sets the entry's "enable if" dependency. Whenever the dependency is unmet, the entry will be disabled.
     * <br>
     * Passing in a {@code null} value will remove the entry's dependency.
     *
     * @param dependency the new dependency. 
     */
    void setEnableIfDependency(@Nullable Dependency dependency);
    
    /**
     * Sets the entry's "show if" dependency. Whenever the dependency is unmet, the entry will be hidden from menus.
     * <br>
     * Passing in a {@code null} value will remove the entry's dependency.
     *
     * @param dependency the new dependency. 
     */
    void setShowIfDependency(@Nullable Dependency dependency);
    
    Component getFieldName();
    
    @ApiStatus.Internal
    Field getOrSetDeclaringField(Field field);
    
    @Contract(pure = true)
    default Class<T> getType() {
        //noinspection unchecked
        return (Class<T>) getValue().getClass();
    }
}
