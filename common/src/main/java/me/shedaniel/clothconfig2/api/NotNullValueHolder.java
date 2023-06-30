package me.shedaniel.clothconfig2.api;

import org.jetbrains.annotations.NotNull;

public interface NotNullValueHolder<T> {
    @NotNull T getValue();
}
