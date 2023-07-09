package me.shedaniel.autoconfig.requirements.builder;

import me.shedaniel.autoconfig.annotation.RequirementHandler;
import me.shedaniel.clothconfig2.api.Requirement;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Method;

@ApiStatus.Internal
@FunctionalInterface
public interface HandlerBuilder {
    Requirement build(Method method, RequirementHandler definition);
}
