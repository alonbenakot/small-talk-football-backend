package com.smalltalk.SmallTalkFootball.system.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResponseHandler is the boundary that keeps apifootball.com failures from becoming
 * application failures. Its whole contract is that it never throws: every unusable response
 * has to come back as an empty Optional so callers degrade to an empty list.
 */
class ResponseHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private record Payload(String name, int value) {
    }

    private static ResponseEntity<String> ok(String body) {
        return ResponseEntity.ok(body);
    }

    @Test
    void returnsTheDeserialisedBodyOnSuccess() {
        Optional<Payload> result = ResponseHandler.process(
                () -> ok("{\"name\":\"Anfield\",\"value\":7}"),
                "should not be logged", Payload.class, objectMapper);

        assertThat(result).contains(new Payload("Anfield", 7));
    }

    @Test
    void deserialisesGenericTypesThroughATypeReference() {
        Optional<List<Payload>> result = ResponseHandler.process(
                () -> ok("[{\"name\":\"a\",\"value\":1},{\"name\":\"b\",\"value\":2}]"),
                "should not be logged", new TypeReference<List<Payload>>() {
                }, objectMapper);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(2);
    }

    @Test
    void returnsEmptyOnAnErrorStatus() {
        Optional<Payload> result = ResponseHandler.process(
                () -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"name\":\"x\",\"value\":1}"),
                "server error", Payload.class, objectMapper);

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyOnAnEmptyBody() {
        Optional<Payload> result = ResponseHandler.process(
                () -> ResponseEntity.ok(null),
                "no body", Payload.class, objectMapper);

        assertThat(result).isEmpty();
    }

    /*
     * apifootball sometimes answers with an HTML error page under a 200, which would otherwise
     * surface as an unhelpful Jackson parse error.
     */
    @Test
    void returnsEmptyWhenTheBodyIsAnHtmlErrorPage() {
        Optional<Payload> result = ResponseHandler.process(
                () -> ok("<!DOCTYPE html><html><body>Gateway Timeout</body></html>"),
                "html page", Payload.class, objectMapper);

        assertThat(result).isEmpty();
    }

    @Test
    void ignoresLeadingWhitespaceWhenDetectingHtml() {
        Optional<Payload> result = ResponseHandler.process(
                () -> ok("\n  <html><body>nope</body></html>"),
                "html page", Payload.class, objectMapper);

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyOnMalformedJson() {
        Optional<Payload> result = ResponseHandler.process(
                () -> ok("{not json at all"),
                "malformed", Payload.class, objectMapper);

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyWhenTheBodyDoesNotMatchTheTargetType() {
        Optional<Payload> result = ResponseHandler.process(
                () -> ok("[1, 2, 3]"),
                "wrong shape", Payload.class, objectMapper);

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyWhenTheCallItselfThrows() {
        Optional<Payload> result = ResponseHandler.process(
                () -> {
                    throw new IllegalStateException("connection refused");
                },
                "call failed", Payload.class, objectMapper);

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyWhenTheBodyIsTheJsonNullLiteral() {
        Optional<Payload> result = ResponseHandler.process(
                () -> ok("null"),
                "null literal", Payload.class, objectMapper);

        assertThat(result).isEmpty();
    }

    @Test
    void treatsAnyTwoHundredRangeStatusAsValid() {
        assertThat(ResponseHandler.isValidResponse(ResponseEntity.ok("{}"))).isTrue();
        assertThat(ResponseHandler.isValidResponse(ResponseEntity.status(HttpStatus.CREATED).body("{}"))).isTrue();
        assertThat(ResponseHandler.isValidResponse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("{}"))).isFalse();
        assertThat(ResponseHandler.isValidResponse(ResponseEntity.ok(null))).isFalse();
    }
}
