package com.juanmuscaria.dmm.event;

import io.micronaut.context.event.ApplicationEvent;
import javafx.stage.Stage;
import lombok.Getter;

public class FXEvent extends ApplicationEvent {
    public FXEvent(Object source) {
        super(source);
    }

    public static class FXInit extends FXEvent {
        public FXInit(Object source) {
            super(source);
        }
    }

    @Getter
    public static class FXStart extends FXEvent {
        private final Stage primaryStage;

        public FXStart(Object source, Stage primaryStage) {
            super(source);
            this.primaryStage = primaryStage;
        }
    }
}
