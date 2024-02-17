package com.juanmuscaria.dmm.data;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.*;
import lombok.EqualsAndHashCode.Exclude;
import lombok.extern.jackson.Jacksonized;


@Getter
@Setter
@ToString
@Builder
@Jacksonized
@ReflectiveAccess
@AllArgsConstructor
@EqualsAndHashCode
public class ModEntry implements Comparable<ModEntry> {
    private final String modPath;
    @Exclude
    private final ModMetadata metadata;
    @Exclude // We don't want to compare state
    private boolean enabled;

    @Override
    public int compareTo(ModEntry other) {
        return this.getModPath().compareTo(other.getModPath());
    }
}
