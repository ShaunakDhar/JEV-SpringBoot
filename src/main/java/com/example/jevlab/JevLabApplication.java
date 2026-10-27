package com.example.jevlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.WebApplicationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Map;

@SpringBootApplication
public class JevLabApplication {
    public static void main(String[] args) throws Exception {
        boolean serve = Arrays.asList(args).contains("--serve");
        var application = new SpringApplication(JevLabApplication.class);
        application.setWebApplicationType(serve ? WebApplicationType.SERVLET : WebApplicationType.NONE);
        String[] springArgs = Arrays.stream(args).filter(a -> !a.equals("--serve")).toArray(String[]::new);
        int code = 0;
        try {
            var context = application.run(springArgs);
            if (serve) return;
            try (context) {
                code = context.getBean(TerminalCommand.class).execute(args, System.in, System.out);
            }
        } catch (Exception e) {
            new ObjectMapper().writeValue(System.out, Map.of("error", "Application startup failed. Check configuration and stderr logs."));
            System.out.println();
            code = 1;
        }
        if (code != 0) System.exit(code);
    }
}
