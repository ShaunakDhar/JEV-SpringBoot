package com.example.jevlab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class JevClientTest {
    final ObjectMapper mapper=new ObjectMapper();
    HttpServer server;
    String base;
    int status=200;
    String response="{\"model\":\"jev-1.14.0\",\"answers\":{\"check\":{\"type\":\"noul\",\"noul\":0.9}},\"usage\":{\"input_tokens\":10,\"output_tokens\":0}}";
    final AtomicReference<String> authorization=new AtomicReference<>(), imageHeader=new AtomicReference<>(), contextHeader=new AtomicReference<>(), body=new AtomicReference<>();
    @BeforeEach void start() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/systemone", e->{
            authorization.set(e.getRequestHeaders().getFirst("Authorization"));
            imageHeader.set(e.getRequestHeaders().getFirst("TypeSafe-Danger-Zone-Images"));
            contextHeader.set(e.getRequestHeaders().getFirst("TypeSafe-Danger-Zone-Long-Context"));
            body.set(new String(e.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] bytes=response.getBytes(StandardCharsets.UTF_8);
            e.sendResponseHeaders(status,bytes.length);e.getResponseBody().write(bytes);e.close();
        });
        server.start();base="http://127.0.0.1:"+server.getAddress().getPort();
    }
    @AfterEach void stop(){ server.stop(0); }
    JevClient client(boolean images,boolean context){return new JevClient(mapper,"test-key",base,"jev-1.14.0",images,context);}
    ObjectNode request() throws Exception {return (ObjectNode)mapper.readTree("{\"state\":\"hello\",\"questions\":{\"check\":{\"type\":\"noul\",\"instructions\":\"Is this a greeting?\"}}}");}

    @Test void sendsDocumentedWireFormatAndKeepsKeyServerSide() throws Exception {
        var result=client(false,false).evaluate(request());
        assertThat(result.path("answers").path("check").path("noul").asDouble()).isEqualTo(.9);
        assertThat(authorization.get()).isEqualTo("Bearer test-key");
        assertThat(mapper.readTree(body.get()).path("model").asText()).isEqualTo("jev-1.14.0");
        assertThat(imageHeader.get()).isNull();assertThat(contextHeader.get()).isNull();
        assertThat(result.toString()).doesNotContain("test-key");
    }
    @Test void experimentalHeadersAreExplicitlyOptedIn() throws Exception {
        var req=request();req.putObject("assets").putObject("image-id").put("type","image").put("format","png").put("data_base64","abc=");
        assertThatThrownBy(()->client(false,false).evaluate(req)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        client(true,true).evaluate(req);
        assertThat(imageHeader.get()).isEqualTo("true");assertThat(contextHeader.get()).isEqualTo("true");
    }
    @Test void rateLimitAndUpstreamFailuresDoNotReturnSuccess() throws Exception {
        status=429;
        assertThatThrownBy(()->client(false,false).evaluate(request())).isInstanceOf(ResponseStatusException.class).hasMessageContaining("429");
        status=401;response="secret upstream diagnostic";
        assertThatThrownBy(()->client(false,false).evaluate(request())).hasMessageContaining("502").hasMessageNotContaining("secret upstream");
    }
    @Test void malformedOrMissingAnswersFailClosed() throws Exception {
        for(String invalid:new String[]{"not-json","{}","{\"answers\":{}}","{\"answers\":{\"check\":{\"type\":\"noul\",\"noul\":2}}}"}) {
            response=invalid;
            assertThatThrownBy(()->client(false,false).evaluate(request())).isInstanceOf(ResponseStatusException.class).hasMessageContaining("502");
        }
    }
    @Test void missingKeyFailsBeforeCallingProvider() throws Exception {
        var noKey=new JevClient(mapper,"",base,"jev-1.14.0",false,false);
        assertThatThrownBy(()->noKey.evaluate(request())).hasMessageContaining("503");
        assertThat(body.get()).isNull();
    }
}

