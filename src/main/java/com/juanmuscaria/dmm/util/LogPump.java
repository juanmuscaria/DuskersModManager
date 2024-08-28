package com.juanmuscaria.dmm.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LogPump extends Thread {
    private final Logger logger;
    private final Process process;

    public LogPump(Process process, String processName) {
        super("LogPump@" + processName);
        this.process = process;
        this.logger = LoggerFactory.getLogger(processName);
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
