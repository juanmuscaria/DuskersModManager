package com.juanmuscaria.dmm.data;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.*;
import lombok.EqualsAndHashCode.Exclude;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;

@JsonSerialize
@AllArgsConstructor
@Builder
@Jacksonized
@Getter
@Setter
@EqualsAndHashCode
@ToString
@ReflectiveAccess
public class ModEntry implements Comparable<ModEntry> {
    @ReflectiveAccess
    private final String modPath;
    @Exclude
    @ReflectiveAccess
    private final ModMetadata metadata;
    @Exclude // We don't want to compare state
    @ReflectiveAccess
    private boolean enabled;

    @Override
    public int compareTo(ModEntry other) {
        return this.getModPath().compareTo(other.getModPath());
    }
}
