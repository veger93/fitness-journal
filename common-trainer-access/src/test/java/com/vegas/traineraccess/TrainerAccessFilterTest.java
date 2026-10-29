package com.vegas.traineraccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerAccessFilterTest {

    @Mock
    private TrainerLinkRepository repository;

    private TrainerAccessFilter filter;
    private final UUID trainerId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        filter = new TrainerAccessFilter(repository, new ObjectMapper().registerModule(new JavaTimeModule()));
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS512").subject(trainerId.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void withoutHeader_requestStaysAsTrainerHimself() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(currentJwt().getSubject()).isEqualTo(trainerId.toString());
        assertThat(TrainerAccessFilter.actorId(currentJwt())).isNull();
    }

    @Test
    void myClient_requestRunsAsClient_trainerIsActor() throws Exception {
        when(repository.existsByClientIdAndTrainerId(clientId, trainerId)).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TrainerAccessFilter.HEADER, clientId.toString());
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(currentJwt().getSubject()).isEqualTo(clientId.toString());
        assertThat(TrainerAccessFilter.actorId(currentJwt())).isEqualTo(trainerId);
    }

    @Test
    void notMyClient_forbidden_chainNotCalled() throws Exception {
        when(repository.existsByClientIdAndTrainerId(clientId, trainerId)).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TrainerAccessFilter.HEADER, clientId.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Нет доступа к этому клиенту");
        assertThat(chain.getRequest()).isNull();
        assertThat(currentJwt().getSubject()).isEqualTo(trainerId.toString());
    }

    @Test
    void brokenHeader_badRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TrainerAccessFilter.HEADER, "не-uuid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
    }

    private static Jwt currentJwt() {
        return ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken();
    }
}
