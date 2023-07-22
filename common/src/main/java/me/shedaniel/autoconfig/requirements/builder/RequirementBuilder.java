package me.shedaniel.autoconfig.requirements.builder;

import me.shedaniel.clothconfig2.api.Requirement;

public interface RequirementBuilder<T> {
    Requirement build();
}
