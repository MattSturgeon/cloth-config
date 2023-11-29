package me.shedaniel.autoconfig.requirements.handler;

import me.shedaniel.clothconfig2.api.ValueHolder;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
class GenericRequirementHandler<T>  extends ConfigEntryRequirementHandler<T, String> {
    
    GenericRequirementHandler(ValueHolder<T> gui, String[] conditions) {
        super(gui, conditions);
    }
    
    @Override
    public boolean check(T value, String condition) {
        return value.toString().equals(condition);
    }
}
