package com.wks.integration.honeywell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.Test;

class ResponseSizeLimitInterceptorTest {
    @Test void streamsUpToLimitAndStopsOnNextByte() throws Exception {
        MessageImpl message = new MessageImpl(); message.setContent(InputStream.class, new ByteArrayInputStream(new byte[] {1,2,3,4}));
        new ResponseSizeLimitInterceptor(3).handleMessage(message); InputStream limited = message.getContent(InputStream.class);
        assertThat(limited.read(new byte[3])).isEqualTo(3);
        assertThatThrownBy(limited::read).isInstanceOf(ResponseSizeLimitInterceptor.ResponseTooLargeException.class);
    }
}
