package com.juanmuscaria.dmm;

import com.juanmuscaria.dmm.event.FXEvent;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Singleton;
import javafx.application.Application;
import javafx.stage.Stage;

@Singleton
public class ModManagerApplication extends Application {
    static ApplicationContext context;

    public ModManagerApplication() {
    }

    @Override
    public void init() {
        context.registerSingleton(this);
        context.getEventPublisher(FXEvent.FXInit.class).publishEvent(new FXEvent.FXInit(this));
    }

    @Override
    public void start(Stage primaryStage) {
        context.getEventPublisher(FXEvent.FXStart.class).publishEvent(new FXEvent.FXStart(this, primaryStage));
    }
}
