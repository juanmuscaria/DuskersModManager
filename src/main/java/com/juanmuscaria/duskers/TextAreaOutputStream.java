package com.juanmuscaria.duskers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingDeque;

public class TextAreaOutputStream extends ByteArrayOutputStream {
    private final BlockingDeque<String> logMessages;

    public TextAreaOutputStream(BlockingDeque<String> logMessages) {
        this.logMessages = logMessages;
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        String record = this.toString();
        super.reset();
        logMessages.offer(record);
    }
}
