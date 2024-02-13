package com.juanmuscaria.dmm.service;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import jakarta.inject.Inject;
import javafx.fxml.FXMLLoader;

@Factory
class FXMLLoaderFactory {
    @Inject
    protected ApplicationContext context;

    @Prototype
    public FXMLLoader getLoader() {
        var loader = new FXMLLoader();
        loader.setControllerFactory(context::getBean);
        return loader;
    }
}
