package com.wks.integration.honeywell;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/** Counts bytes as CXF consumes this client's inbound stream; it never pre-buffers the response. */
final class ResponseSizeLimitInterceptor extends AbstractPhaseInterceptor<Message> {
    static final class ResponseTooLargeException extends IOException {
        ResponseTooLargeException(long limit) { super("Honeywell response exceeded configured " + limit + " byte limit"); }
    }
    private final long limit;
    ResponseSizeLimitInterceptor(long limit) { super(Phase.RECEIVE); this.limit = limit; }
    @Override public void handleMessage(Message message) throws Fault {
        InputStream input = message.getContent(InputStream.class);
        if (input != null) message.setContent(InputStream.class, new LimitedInputStream(input, limit));
    }
    private static final class LimitedInputStream extends FilterInputStream {
        private final long limit; private long count;
        LimitedInputStream(InputStream input, long limit) { super(input); this.limit = limit; }
        @Override public int read() throws IOException { int value = super.read(); if (value >= 0) add(1); return value; }
        @Override public int read(byte[] bytes, int off, int len) throws IOException {
            int read = super.read(bytes, off, len); if (read > 0) add(read); return read;
        }
        private void add(int read) throws IOException { count += read; if (count > limit) throw new ResponseTooLargeException(limit); }
    }
}
