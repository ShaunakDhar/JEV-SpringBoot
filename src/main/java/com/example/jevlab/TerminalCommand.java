package com.example.jevlab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

@Component
public class TerminalCommand {
    private final ObjectMapper mapper;
    private final JevClient client;
    public TerminalCommand(ObjectMapper mapper, JevClient client) { this.mapper=mapper; this.client=client; }

    public int execute(String[] args, InputStream input, OutputStream output) throws java.io.IOException {
        int exit=0;
        Object result;
        try {
            if (Arrays.asList(args).contains("--help")) {
                result=Map.of("usage", "java -jar jev-learning-lab.jar --request=requests/support-routing.json", "stdin", "java -jar jev-learning-lab.jar --request=-", "server", "java -jar jev-learning-lab.jar --serve", "configuration", "Edit .env in the current working directory. Environment variables override .env values.");
            } else {
                String file=Arrays.stream(args).filter(a->a.startsWith("--request=")).map(a->a.substring(10)).findFirst()
                        .orElseThrow(()->new IllegalArgumentException("Supply --request=<JSON file> or --request=- for stdin. Use --help for usage."));
                byte[] bytes;
                if (file.equals("-")) bytes=input.readNBytes(2_000_001);
                else try(var stream=Files.newInputStream(Path.of(file))) { bytes=stream.readNBytes(2_000_001); }
                if (bytes.length>2_000_000) throw new IllegalArgumentException("Request must not exceed two million bytes.");
                var node=mapper.readTree(bytes);
                if (!(node instanceof ObjectNode request)) throw new IllegalArgumentException("Request must be a JSON object.");
                LabController.validate(request);
                result=client.evaluate(request);
            }
        } catch (ResponseStatusException e) {
            result=Map.of("error",e.getReason()==null?"Request failed":e.getReason(),"status",e.getStatusCode().value()); exit=1;
        } catch (Exception e) {
            result=Map.of("error", "Invalid request or unreadable file. " + (e instanceof IllegalArgumentException ? e.getMessage() : "Check JSON syntax and the file path.")); exit=2;
        }
        // Write exactly one JSON object. Spring logs go to stderr via logback-spring.xml.
        output.write(mapper.writeValueAsBytes(result)); output.write('\n'); output.flush();
        return exit;
    }
}
