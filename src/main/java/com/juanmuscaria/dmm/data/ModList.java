package com.juanmuscaria.dmm.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.micronaut.core.annotation.ReflectiveAccess;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.ConcurrentSkipListSet;

@JsonSerialize
@ToString
@ReflectiveAccess
public class ModList {
    /**
     * This will be the actual backing set that will be written to disk, it must be both thread safe and sorted
     */
    @JsonProperty("mods")
    @ReflectiveAccess
    private ConcurrentSkipListSet<ModEntry> backend;

    /**
     * Observable wrapper to be used with javafx, changes must be done on the platform thread
     */
    @JsonIgnore
    @Getter
    private ObservableSet<ModEntry> mods;

    public ModList() {
        this(new ConcurrentSkipListSet<>());
    }

    public ModList(ConcurrentSkipListSet<ModEntry> mods) {
        this.backend = mods;
        this.mods = FXCollections.observableSet(backend);
    }

    /**
     * Will be called by jackson
     */
    protected void setBackend(ConcurrentSkipListSet<ModEntry> backend) {
        this.backend = backend;
        this.mods = FXCollections.observableSet(backend);
    }
}
