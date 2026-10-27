package com.example.jevlab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"jev.api-key=", "spring.config.import="})
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Test void missingKeyReturnsJsonErrorWithNoFallback() throws Exception {
        mvc.perform(post("/api/decisions").contentType("application/json").content("{\"state\":\"hello\",\"questions\":{\"q\":{\"type\":\"noul\"}}}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error").exists());
    }
    @Test void invalidRequestsAreRejected() throws Exception {
        mvc.perform(post("/api/decisions").contentType("application/json").content("{oops")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/decisions").contentType("application/json").content("{}" )).andExpect(status().isBadRequest());
    }
    @Test void noFrontendOrFixtureEndpointsExist() throws Exception {
        mvc.perform(get("/")).andExpect(status().isNotFound());
        mvc.perform(get("/api/examples")).andExpect(status().isNotFound());
        mvc.perform(get("/index.html")).andExpect(status().isNotFound());
    }
}
