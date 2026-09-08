package com.smalltalk.SmallTalkFootball.security;

import com.smalltalk.SmallTalkFootball.domain.User;
import com.smalltalk.SmallTalkFootball.services.UserService;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * There is no Spring Security here: JwtAuthFilter hard-codes which URI and method
 * combinations need a token and which need an admin, so the routing table is only as correct
 * as these cases say it is.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private UserService userService;

    private JwtUtil jwtUtil;
    private MockMvc mockMvc;
    private User member;
    private User admin;

    @RestController
    static class AnyEndpointController {
        @RequestMapping(value = "/**",
                method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH,
                        RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
        String handle() {
            return "reached the controller";
        }
    }

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationInHours", 720L);
        mockMvc = MockMvcBuilders.standaloneSetup(new AnyEndpointController())
                .addFilters(new JwtAuthFilter(jwtUtil, userService))
                .build();

        member = TestFixtures.member("member@example.com");
        admin = TestFixtures.admin("admin@example.com");
        lenient().when(userService.getUserByEmail("member@example.com")).thenReturn(member);
        lenient().when(userService.getUserByEmail("admin@example.com")).thenReturn(admin);
    }

    private String tokenFor(User user) {
        return "Bearer " + jwtUtil.generateToken(user);
    }

    private org.springframework.test.web.servlet.ResultActions call(String method, String uri) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.request(HttpMethod.valueOf(method), uri));
    }

    private org.springframework.test.web.servlet.ResultActions callAs(String method, String uri, User user)
            throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.request(HttpMethod.valueOf(method), uri)
                .header("Authorization", tokenFor(user)));
    }

    @Nested
    @DisplayName("open endpoints")
    class Open {

        @ParameterizedTest
        @CsvSource({
                "GET,/fixtures",
                "GET,/fixtures/abc",
                "GET,/one-liners/abc",
                "GET,/small-infos",
                "GET,/small-infos/categories",
                "GET,/articles/published",
                "GET,/competitions",
                "POST,/users/login",
                "POST,/users/signup"})
        void needNoToken(String method, String uri) throws Exception {
            call(method, uri).andExpect(status().isOk());
        }

        @Test
        void preflightRequestsAlwaysPassThrough() throws Exception {
            call("OPTIONS", "/teams").andExpect(status().isOk());
            call("OPTIONS", "/fixtures").andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("endpoints that require a token")
    class RequiresToken {

        @ParameterizedTest
        @CsvSource({
                "POST,/small-infos",
                "POST,/articles",
                "PATCH,/articles/publish/1",
                "POST,/fixtures",
                "GET,/teams",
                "POST,/teams",
                "PATCH,/teams/standings",
                "DELETE,/fixtures",
                "DELETE,/articles/1",
                "DELETE,/small-infos/1"})
        void areRejectedWithoutOne(String method, String uri) throws Exception {
            call(method, uri).andExpect(status().isUnauthorized());
        }

        @Test
        void areRejectedWhenTheHeaderIsNotABearerToken() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/articles")
                            .header("Authorization", "Basic dXNlcjpwYXNz"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void areRejectedWhenTheTokenIsGibberish() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/articles")
                            .header("Authorization", "Bearer not-a-real-token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void areRejectedWhenTheUserNoLongerExists() throws Exception {
            when(userService.getUserByEmail(any())).thenReturn(null);

            mockMvc.perform(MockMvcRequestBuilders.post("/articles")
                            .header("Authorization", tokenFor(member)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void explainThemselvesInTheStandardResponseEnvelope() throws Exception {
            call("POST", "/articles")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.systemMessage").exists());
        }
    }

    @Nested
    @DisplayName("endpoints restricted to admins")
    class RequiresAdmin {

        @ParameterizedTest
        @CsvSource({
                "DELETE,/fixtures",
                "DELETE,/articles/1",
                "DELETE,/small-infos/1",
                "PATCH,/articles/publish/1",
                "PATCH,/articles/remove/1",
                "GET,/teams",
                "POST,/teams",
                "PATCH,/teams/standings",
                "POST,/fixtures"})
        void rejectAMember(String method, String uri) throws Exception {
            callAs(method, uri, member).andExpect(status().isForbidden());
        }

        @ParameterizedTest
        @CsvSource({
                "DELETE,/fixtures",
                "PATCH,/articles/publish/1",
                "POST,/teams",
                "POST,/fixtures"})
        void allowAnAdmin(String method, String uri) throws Exception {
            callAs(method, uri, admin).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("endpoints a signed-in member may use")
    class MemberWritable {

        @ParameterizedTest
        @CsvSource({
                "POST,/articles",
                "POST,/small-infos"})
        void acceptAMemberToken(String method, String uri) throws Exception {
            callAs(method, uri, member).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("known gaps in the routing table")
    class KnownGaps {

        /*
         * Pins a defect. isAdminOnlyRequest compares the URI against "articles/pending" with no
         * leading slash, but getRequestURI() always returns "/articles/pending", so that branch
         * can never match. isJwtRequired only covers POST and PATCH on /articles, so the
         * endpoint listing unpublished articles is reachable with no token at all.
         *
         * When the leading slash is fixed this test will fail and should become an
         * isUnauthorized/isForbidden expectation.
         */
        @Test
        void pendingArticlesAreReadableWithoutAnyToken() throws Exception {
            call("GET", "/articles/pending").andExpect(status().isOk());
        }

        /*
         * Pins a related consequence: because DELETE is admin-only purely by method, a DELETE
         * to a path with no admin meaning is still gated, while GETs are gated by prefix only.
         */
        @Test
        void deleteIsAdminOnlyRegardlessOfPath() throws Exception {
            callAs("DELETE", "/one-liners/anything", member).andExpect(status().isForbidden());
        }
    }
}
