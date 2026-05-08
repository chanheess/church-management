package com.saeanyang.management.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk());
    }

    @Test
    void loginIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk());
    }

    @Test
    void signupIsPublicFromAllowedIp() throws Exception {
        mockMvc.perform(get("/signup").with(request -> {
                request.setRemoteAddr("127.0.0.1");
                return request;
            }))
            .andExpect(status().isOk());
    }

    @Test
    void signupIsForbiddenOutsideAllowedIp() throws Exception {
        mockMvc.perform(get("/signup").header("X-Forwarded-For", "203.0.113.10"))
            .andExpect(status().isForbidden());
    }

    @Test
    void bulletinRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/bulletin"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }
}
