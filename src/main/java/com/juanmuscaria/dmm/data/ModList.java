package com.juanmuscaria.dmm.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.ReflectiveAccess;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.ConcurrentSkipListSet;

@ToString
@ReflectiveAccess
public class ModList {
    /**
     * This will be the actual backing set that will be written to disk;
     * it must be both sorted and thread safe as the save operation is done outside the main thread.
     */
    @JsonProperty("mods")
    private ConcurrentSkipListSet<ModEntry> backend;

    /**
     * Observable wrapper to be used with javafx, changes must be done on the platform thread,
     * all observables touches thread unsafe code paths.
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
     * Will be called by jackson; we need a way to create the javafx observable view of such set.
     * This method cannot be made public
     * because javafx does not offer a way
     * to transfer registered observers to a new object without gutting its internals with reflection.
     */
    @SuppressWarnings("unused")
    protected void setBackend(ConcurrentSkipListSet<ModEntry> backend) {
        this.backend = backend;
        this.mods = FXCollections.observableSet(backend);
    }
}
