package com.juanmuscaria.duskers.ui;

import com.juanmuscaria.duskers.Constants;
import com.juanmuscaria.duskers.DuskersHelper;
import com.juanmuscaria.duskers.ReportedException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.util.Duration;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.*;

public class DuskersLauncherController {
    private static final Logger log = Logger.getLogger(Constants.LOGGER_NAME);
    private static final int MAX_LOG_ENTRIES = 1_000_000;

    private final BlockingDeque<String> logMessages = new LinkedBlockingDeque<>(MAX_LOG_ENTRIES);

    private Process duskers;

    @FXML
    private TextArea logs;

    @FXML
    private Button launch;

    @FXML
    private Button launchUnmodded;

    @FXML
    void initialize() {
        Timeline logTransfer = new Timeline(
                new KeyFrame(
                        Duration.seconds(0.5),
                        event -> {
                            while (logMessages.size() > 0) {
                                logs.appendText(logMessages.poll());
                            }
                        }
                )
        );
        logTransfer.setCycleCount(Timeline.INDEFINITE);
        logTransfer.play();
        Logger.getLogger("").addHandler(new QueueHandler(logMessages, new SimpleFormatter()));

//        log.addHandler(new StreamHandler(new PrintStream(new TextAreaOutputStream(logMessages), true), new SimpleFormatter()));
//
//        log.setLevel(Level.ALL);
    }

    @FXML
    void launch(ActionEvent event) {
        event.consume();
        logs.clear();
        log.info("Launching Duskers with mods");
        launch(true);
    }

    @FXML
    void launchUnmodded(ActionEvent event) {
        event.consume();
        logs.clear();
        log.info("Launching Duskers without mods");
        launch(false);
    }

    private void launch(boolean modded) {
        try {
            var pb = DuskersHelper.buildDuskersLaunchProcess(modded);
            duskers = pb.start();
            var logPipe = new LogPump(duskers, logMessages);
            logPipe.start();
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
            log.severe("Unable to launch duskers");
            var sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            log.severe(sw.toString());
        } catch (ReportedException e) {
            log.severe("Unable to launch duskers: " + e.getHeader());
            var sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            log.severe(sw.toString());
        }
    }
}

class LogPump extends Thread {

    private final Process process;
    private final BlockingDeque<String> messages;

    LogPump(Process process, BlockingDeque<String> messages) {
        super("LogPump");
        this.process = process;
        this.messages = messages;
    }

    @Override
    public void run() {
        try {
            var in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String message;

            while (process.isAlive()) {
                if ((message = in.readLine()) != null) {
                    messages.offer(message + "\n");
                }
            }

        } catch (IOException e) {
            messages.offer("[Duskers Mod Loader] Unable to listen for more logs\n");
            var sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            messages.offer(sw.toString());
        }
    }
}

class QueueHandler extends Handler {

    private final BlockingDeque<String> messages;

    QueueHandler(BlockingDeque<String> messages, Formatter formatter) {
        this.messages = messages;
        this.setLevel(Level.INFO);
        this.setFormatter(Objects.requireNonNull(formatter));
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        String msg;
        try {
            msg = getFormatter().format(record);
        } catch (Exception ex) {
            reportError(null, ex, ErrorManager.FORMAT_FAILURE);
            return;
        }
        messages.offer(msg);
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}