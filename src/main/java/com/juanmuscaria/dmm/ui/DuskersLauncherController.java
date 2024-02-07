package com.juanmuscaria.dmm.ui;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Context;
import com.juanmuscaria.dmm.util.DialogHelper;
import com.juanmuscaria.dmm.util.DuskersHelper;
import io.micronaut.core.annotation.ReflectiveAccess;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@ReflectiveAccess
public class DuskersLauncherController {
    private static final Logger logger = LoggerFactory.getLogger(DuskersLauncherController.class);
    private Process duskers;

    @FXML
    @ReflectiveAccess
    private TextArea logs;

    @FXML
    @ReflectiveAccess
    private Button launch;

    @FXML
    @ReflectiveAccess
    private Button launchUnmodded;

    @FXML
    @ReflectiveAccess
    void initialize() {
        // Dirty hack to get log messages
        var rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        var appender = new LogbackListAppender(rootLogger.getLoggerContext());
        appender.start();
        rootLogger.addAppender(appender);
    }

    @FXML
    @ReflectiveAccess
    void launch(ActionEvent event) {
        event.consume();
        logs.clear();
        logger.info("Launching Duskers with mods");
        launch(true);
    }

    @FXML
    @ReflectiveAccess
    void launchUnmodded(ActionEvent event) {
        event.consume();
        logs.clear();
        logger.info("Launching Duskers without mods");
        launch(false);
    }

    private void launch(boolean modded) {
        try {
            var pb = DuskersHelper.buildDuskersLaunchProcess(modded);
            duskers = pb.start();
            var logPump = new LogPump(duskers);
            logPump.start();
            launch.setDisable(true);
            launchUnmodded.setDisable(true);
            new Thread("TerminationHandler") {
                @Override
                public void run() {
                    try {
                        duskers.waitFor();
                    } catch (InterruptedException ignored) {
                    } finally {
                        launch.setDisable(false);
                        launchUnmodded.setDisable(false);
                    }
                }
            }.start();
        } catch (IOException e) {
            logger.error("Unable to launch duskers", e);
        } catch (DialogHelper.ReportedException e) {
            logger.error("Unable to launch duskers: {}", e.getHeader(), e);
        }
    }

    private class LogbackListAppender extends AppenderBase<ILoggingEvent> {
        private final PatternLayout layout = new PatternLayout();

        LogbackListAppender(Context ctx) {
            layout.setContext(ctx);
            layout.setPattern("%-5level %logger{36} - %msg%n");
            layout.start();
            this.setContext(ctx);
        }

        @Override
        protected void append(ILoggingEvent event) {
            var logMessage = layout.doLayout(event);
            Platform.runLater(() -> logs.appendText(logMessage));
        }
    }

    private static class LogPump extends Thread {
        private static final Logger logger = LoggerFactory.getLogger("Duskers");
        private final Process process;

        LogPump(Process process) {
            super("LogPump");
            this.process = process;
        }

        @Override
        public void run() {
            try {
                var in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String message;

                while (process.isAlive()) {
                    if ((message = in.readLine()) != null) {
                        logger.info(message);
                    }
                }
            } catch (IOException e) {
                logger.error("Unable to listen for more logs", e);
            }
        }
    }
}