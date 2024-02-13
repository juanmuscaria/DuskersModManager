package com.juanmuscaria.dmm.data;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.micronaut.core.annotation.ReflectiveAccess;

@JsonSerialize
@ReflectiveAccess
public record ModMetadata(String name, String id, String version) {
}

