package com.carrental.client;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemoRentalFlowTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void realDemoLoginCatalogReservationAndReturn() throws Exception {
        mvc.perform(get("/voitures")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Loc Auto")));

        MvcResult login = mvc.perform(post("/login").with(csrf())
                .param("login", "sophie.martin").param("passwd", "password123"))
                .andExpect(redirectedUrl("/")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/voitures").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Renault")));

        MvcResult reservation = mvc.perform(post("/locations/new/AA-123-BB").with(csrf()).session(session)
                .param("dteDeb", LocalDate.now().plusDays(1).toString())
                .param("duree", "2").param("cheque", "DEMO-TEST-ONLY"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String detailPath = reservation.getResponse().getRedirectedUrl();
        assertThat(detailPath).startsWith("/locations/");
        assertThat(jdbc.queryForObject("SELECT disp FROM voiture WHERE mat = 'AA-123-BB'", String.class))
                .isEqualTo("LOUEE");
        mvc.perform(get(detailPath).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("AA-123-BB")));

        mvc.perform(post(detailPath + "/terminer").with(csrf()).session(session))
                .andExpect(redirectedUrl("/locations"));
        assertThat(jdbc.queryForObject("SELECT disp FROM voiture WHERE mat = 'AA-123-BB'", String.class))
                .isEqualTo("DISPONIBLE");
        assertThat(jdbc.queryForObject("SELECT etat FROM location WHERE cheque = 'DEMO-TEST-ONLY'", String.class))
                .isEqualTo("TERMINEE");
    }
}
