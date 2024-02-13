package com.juanmuscaria.dmm.ui;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Context;
import com.juanmuscaria.dmm.data.ModEntry;
import com.juanmuscaria.dmm.service.ModManager;
import com.juanmuscaria.dmm.util.DialogHelper;
import com.juanmuscaria.dmm.util.DuskersHelper;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.DragEvent;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

@Singleton
@ReflectiveAccess
public class DuskersLauncherController {
    private static final Logger logger = LoggerFactory.getLogger(DuskersLauncherController.class);
    public ListView<ModEntry> modListView;
    public TextArea logs;
    public Label versionLabel;
    public Button launch;
    public Button launchUnmodded;
    private Process duskers;
    @Inject
    public ModManager modManager;
    @Inject
    public Application application;
    @Value("${dmm.version}")
    public String version;

    @FXML
    @ReflectiveAccess
    void initialize() {
        // Dirty hack to get log messages
        var rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        var appender = new LogbackListAppender(rootLogger.getLoggerContext());
        appender.start();
        rootLogger.addAppender(appender);

        versionLabel.setText(version);

        try {
            modManager.loadOrCreateFiles();
        } catch (Throwable e) {
            DialogHelper.reportAndExit(e);
        }

        modListView.setCellFactory(CheckBoxListCell.forListView(item -> modManager.makeModEnabledProperty(item), new StringConverter<ModEntry>() {
            @Override
            public String toString(ModEntry object) {
                return "%s %s [%s, %s]".formatted(object.getMetadata().name(),
                    object.getMetadata().version(), object.getMetadata().id(), Path.of(object.getModPath()).getFileName());
            }

            @Override
            public ModEntry fromString(String string) {
                return null;
            }
        }));

        modListView.getItems().addAll(modManager.getModlist().getMods());
        modManager.getModlist().getMods().addListener((SetChangeListener<ModEntry>) change -> {
            modListView.getItems().clear();
            modListView.getItems().addAll(change.getSet());
        });
    }

    @FXML
    @ReflectiveAccess
    void launch(ActionEvent event) {
        event.consume();
        logger.info("Preparing mods");
        try {
            modManager.updateInstalledMods();
        } catch (Exception e) {
            DialogHelper.reportAndWait(e, "Mod preparation failed", "Seems like something went wrong when preparing your mods, possibly a bug on the mod manager!");
            return;
        }
        logger.info("Launching Duskers with mods");
        launch(true);
    }

    @FXML
    @ReflectiveAccess
    void launchUnmodded(ActionEvent event) {
        event.consume();
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

    public void dragDroppedOnMods(DragEvent dragEvent) {
        logger.warn("STUB File was dropped but no file handling is implemented");
    }

    public void installModButtonClicked(ActionEvent event) {
        DialogHelper.infoAndWait("This button should actually open the system file picker", "Bother me if I forgot to implement");
    }

    public void openModFolderButtonClicked(ActionEvent event) {
        event.consume();
        application.getHostServices().showDocument(modManager.getModsDir().toUri().toString());
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
}