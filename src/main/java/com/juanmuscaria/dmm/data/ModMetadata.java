package com.juanmuscaria.dmm.data;

import io.micronaut.core.annotation.ReflectiveAccess;

@ReflectiveAccess
public record ModMetadata(String name, String id, String version) {
}

