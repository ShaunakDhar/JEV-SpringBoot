package com.example.jevlab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TerminalCommandTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final JevClient client=mock(JevClient.class);
    private final ByteArrayOutputStream output=new ByteArrayOutputStream();
    @Test void stdinProducesOnlyProviderJson() throws Exception {
        String request="{\"state\":\"hello\",\"questions\":{\"greeting\":{\"type\":\"noul\"}}}";
        var response=mapper.readTree("{\"answers\":{\"greeting\":{\"type\":\"noul\",\"noul\":0.9}},\"model\":\"test-provider\"}");
        when(client.evaluate(any(ObjectNode.class))).thenReturn(response);
        int code=new TerminalCommand(mapper,client).execute(new String[]{"--request=-"},new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8)),output);
        assertThat(code).isZero();assertThat(mapper.readTree(output.toByteArray())).isEqualTo(response);
        verify(client).evaluate((ObjectNode)mapper.readTree(request));
    }
    @Test void invalidInputNeverCallsProviderAndReturnsJsonError() throws Exception {
        int code=new TerminalCommand(mapper,client).execute(new String[]{"--request=-"},new ByteArrayInputStream("{oops".getBytes()),output);
        assertThat(code).isEqualTo(2);assertThat(mapper.readTree(output.toByteArray()).has("error")).isTrue();verifyNoInteractions(client);
    }
    @Test void helpIsJsonAndDoesNotNeedAKey() throws Exception {
        assertThat(new TerminalCommand(mapper,client).execute(new String[]{"--help"},new ByteArrayInputStream(new byte[0]),output)).isZero();
        assertThat(mapper.readTree(output.toByteArray()).has("usage")).isTrue();verifyNoInteractions(client);
    }
}
