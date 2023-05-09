/*
 * This file is part of Cloth Config.
 * Copyright (C) 2020 - 2021 shedaniel
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package me.shedaniel.clothconfig2.impl.builders;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.Requirement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public abstract class FieldBuilder<T, A extends AbstractConfigListEntry, SELF extends FieldBuilder<T, A, SELF>> {
    @NotNull private final Component fieldNameKey;
    @NotNull private final Component resetButtonKey;
    protected boolean requireRestart = false;
    @Nullable protected Supplier<T> defaultValue = null;
    @Nullable protected Function<T, Optional<Component>> errorSupplier;
    @Nullable protected Requirement enableRequirement = null;
    @Nullable protected Requirement displayRequirement = null;
    
    protected FieldBuilder(Component resetButtonKey, Component fieldNameKey) {
        this.resetButtonKey = Objects.requireNonNull(resetButtonKey);
        this.fieldNameKey = Objects.requireNonNull(fieldNameKey);
    }
    
    @Nullable
    public final Supplier<T> getDefaultValue() {
        return defaultValue;
    }
    
    @SuppressWarnings("rawtypes")
    @Deprecated
    public final AbstractConfigListEntry buildEntry() {
        return build();
    }
    
    @NotNull
    public abstract A build();
    
    /**
     * Finishes building the given {@link AbstractConfigListEntry config entry} by applying anything defined in this abstract class.
     * <br><br>
     * Should be used by implementations of {@link #build()}.
     *
     * @param field the config entry to finish building
     * @return the finished config entry
     */
    @Contract(value = "_ -> param1", mutates = "param1")
    protected A finishBuilding(A field) {
        if (field == null)
            return null;
        if (enableRequirement != null)
            field.setEnabledDependency(enableRequirement);
        if (displayRequirement != null)
            field.setDisplayDependency(displayRequirement);
        return field;
    }
    
    @NotNull
    public final Component getFieldNameKey() {
        return fieldNameKey;
    }
    
    @NotNull
    public final Component getResetButtonKey() {
        return resetButtonKey;
    }
    
    public boolean isRequireRestart() {
        return requireRestart;
    }
    
    public void requireRestart(boolean requireRestart) {
        this.requireRestart = requireRestart;
    }
    
    /**
     * Defines a dependency that controls whether the built config entry is enabled.
     * <br><br>
     * If an "enabled" dependency is already set, it will be overwritten. The dependency will be tested
     * using the config entry's value.
     *
     * @param requirement the dependency controls whether the config entry is enabled
     * @return this instance, for chaining
     * @see Predicate 
     */
    @Contract(mutates = "this")
    @SuppressWarnings("unchecked")
    public final SELF setEnableDependency(Requirement requirement) {
        enableRequirement = requirement;
        return (SELF) this;
    }
    
    /**
     * Defines a dependency that controls whether the built config entry is displayed.
     * <br><br>
     * If a "display" dependency is already set, it will be overwritten.
     * 
     * @param requirement the dependency controls whether the config entry is displayed
     * @return this instance, for chaining
     * @see Predicate 
     */
    @Contract(mutates = "this")
    @SuppressWarnings("unchecked")
    public final SELF setDisplayDependency(Requirement requirement) {
        displayRequirement = requirement;
        return (SELF) this;
    }
}
