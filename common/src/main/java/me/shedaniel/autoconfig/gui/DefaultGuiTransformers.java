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

package me.shedaniel.autoconfig.gui;

import blue.endless.jankson.Comment;
import com.google.common.collect.Lists;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Environment(EnvType.CLIENT)
public class DefaultGuiTransformers {
    
    private static final ConfigEntryBuilder ENTRY_BUILDER = ConfigEntryBuilder.create();
    
    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation>[] REQUIREMENT_ANNOTATIONS = new Class[]{
            ConfigEntry.Requirement.EnableIf.class,
            ConfigEntry.Requirement.DisplayIf.class,
            ConfigEntry.Requirement.EnableIfGroup.class,
            ConfigEntry.Requirement.DisplayIfGroup.class
    };
    
    private DefaultGuiTransformers() {
    }
    
    public static GuiRegistry apply(GuiRegistry registry) {
        
        registry.registerAnnotationTransformer(
                (guis, i18n, field, config, defaults, guiProvider) -> guis.stream()
                        .peek(gui -> {
                            if (!(gui instanceof TextListEntry)) {
                                ConfigEntry.Gui.Tooltip tooltip = field.getAnnotation(ConfigEntry.Gui.Tooltip.class);
                                if (tooltip.count() == 0) {
                                    tryRemoveTooltip(gui);
                                } else if (tooltip.count() == 1) {
                                    tryApplyTooltip(
                                            gui,
                                            new Component[]{
                                                    Component.translatable(String.format("%s.%s", i18n, "@Tooltip"))
                                            }
                                    );
                                } else {
                                    tryApplyTooltip(
                                            gui, IntStream.range(0, tooltip.count()).boxed()
                                                    .map(i -> String.format("%s.%s[%d]", i18n, "@Tooltip", i))
                                                    .map(Component::translatable)
                                                    .toArray(Component[]::new)
                                    );
                                }
                            }
                        })
                        .collect(Collectors.toList()),
                ConfigEntry.Gui.Tooltip.class
        );
        
        registry.registerAnnotationTransformer(
                (guis, i18n, field, config, defaults, guiProvider) -> guis.stream()
                        .peek(gui -> {
                            if (!(gui instanceof TextListEntry)) {
                                Comment tooltip = field.getAnnotation(Comment.class);
                                Component[] text = new Component[]{Component.literal(tooltip.value())};
                                tryApplyTooltip(gui, text);
                            }
                        })
                        .collect(Collectors.toList()),
                field -> !field.isAnnotationPresent(ConfigEntry.Gui.Tooltip.class),
                Comment.class
        );
        
        registry.registerAnnotationTransformer(
                (guis, i18n, field, config, defaults, guiProvider) -> guis.stream()
                        .peek(gui -> {
                            if (!(gui instanceof TextListEntry)) {
                                tryRemoveTooltip(gui);
                            }
                        })
                        .collect(Collectors.toList()),
                ConfigEntry.Gui.NoTooltip.class
        );
        
        registry.registerAnnotationTransformer(
                (guis, i18n, field, config, defaults, guiProvider) -> {
                    ArrayList<AbstractConfigListEntry> ret = new ArrayList<>(guis);
                    String text = String.format("%s.%s", i18n, "@PrefixText");
                    TextListEntry element = ENTRY_BUILDER.startTextDescription(Component.translatable(text)).build();
                    String s = Component.translatable(i18n).getString().toLowerCase(Locale.ROOT);
                    if (!s.isEmpty()) {
                        element.appendSearchTags(Lists.newArrayList(s.split(" ")));
                    }
                    ret.add(0, element);
                    return Collections.unmodifiableList(ret);
                },
                ConfigEntry.Gui.PrefixText.class
        );
        
        registry.registerAnnotationTransformer(
                (guis, i18n, field, config, defaults, guiProvider) -> {
                    for (AbstractConfigListEntry gui : guis) {
                        gui.setRequiresRestart(field.getAnnotation(ConfigEntry.Gui.RequiresRestart.class).value());
                    }
                    return guis;
                },
                ConfigEntry.Gui.RequiresRestart.class
        );
        
        // Register a transformer for each requirement annotation
        for (Class<? extends Annotation> annotation : REQUIREMENT_ANNOTATIONS) {
            registry.registerAnnotationTransformer(DefaultGuiTransformers::registerRequirements, annotation);
        }
        
        return registry;
    }
    
    private static List<AbstractConfigListEntry> registerRequirements(List<AbstractConfigListEntry> guis, String i18n, Field field, Object config, Object defaults, GuiRegistryAccess guiProvider) {
        for (AbstractConfigListEntry gui : guis) {
            guiProvider.getRequirementManager().registerRequirements(gui, field);
        }
        return guis;
    }
    
    private static void tryApplyTooltip(AbstractConfigListEntry gui, Component[] text) {
        if (gui instanceof TooltipListEntry) {
            TooltipListEntry tooltipGui = (TooltipListEntry) gui;
            tooltipGui.setTooltipSupplier(() -> Optional.of(text));
        }
    }
    
    private static void tryRemoveTooltip(AbstractConfigListEntry gui) {
        if (gui instanceof TooltipListEntry) {
            TooltipListEntry tooltipGui = (TooltipListEntry) gui;
            tooltipGui.setTooltipSupplier(() -> Optional.empty());
        }
    }
}
