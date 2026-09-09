package com.smalltalk.SmallTalkFootball.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smalltalk.SmallTalkFootball.domain.Article;
import com.smalltalk.SmallTalkFootball.security.JwtAuthFilter;
import com.smalltalk.SmallTalkFootball.services.ArticleService;
import com.smalltalk.SmallTalkFootball.system.exceptions.ArticleException;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ArticleController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ArticleService service;

    @Test
    void listsPublishedArticlesInTheEnvelope() throws Exception {
        when(service.getPublishedArticles()).thenReturn(List.of(TestFixtures.article("1", "Live", true)));

        mockMvc.perform(get("/articles/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Live"))
                .andExpect(jsonPath("$.data[0].published").value(true));
    }

    /**
     * The published and pending listings answer 204 rather than an empty envelope.
     */
    @Test
    void answersNoContentWhenThereAreNoArticles() throws Exception {
        when(service.getPublishedArticles()).thenReturn(List.of());
        when(service.getPendingArticles()).thenReturn(List.of());

        mockMvc.perform(get("/articles/published")).andExpect(status().isNoContent());
        mockMvc.perform(get("/articles/pending")).andExpect(status().isNoContent());
    }

    @Test
    void createsAnArticle() throws Exception {
        Article submitted = TestFixtures.article(null, "New", false);
        when(service.addArticle(any())).thenReturn(TestFixtures.article("1", "New", false));

        mockMvc.perform(post("/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitted)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("1"));
    }

    @Test
    void reportsADuplicateTitleAsABadRequest() throws Exception {
        when(service.addArticle(any())).thenThrow(new ArticleException(Messages.ARTICLE_TITLE_NOT_UNIQUE));

        mockMvc.perform(post("/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestFixtures.article(null, "Taken", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.systemMessage.messageText").value(Messages.ARTICLE_TITLE_NOT_UNIQUE));
    }

    /**
     * NotFoundException extends SmallTalkException, so this only maps to 404 because Spring
     * prefers the most specific @ExceptionHandler.
     */
    @Test
    void reportsAMissingArticleAsNotFound() throws Exception {
        when(service.getArticleById("nope")).thenThrow(new NotFoundException(Messages.NO_ARTICLE_FOUND));

        mockMvc.perform(get("/articles/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.systemMessage.messageText").value(Messages.NO_ARTICLE_FOUND));
    }

    @Test
    void publishesAnArticle() throws Exception {
        when(service.publishArticle("1")).thenReturn(TestFixtures.article("1", "Live", true));

        mockMvc.perform(patch("/articles/publish/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.published").value(true));
    }

    @Test
    void unpublishesAnArticle() throws Exception {
        when(service.removeArticle("1")).thenReturn(TestFixtures.article("1", "Draft", false));

        mockMvc.perform(patch("/articles/remove/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.published").value(false));
    }

    @Test
    void deletesAnArticleWithoutABody() throws Exception {
        mockMvc.perform(delete("/articles/1")).andExpect(status().isNoContent());

        verify(service).deleteArticle("1");
    }

    @Test
    void reseedsTheArticleCollection() throws Exception {
        mockMvc.perform(post("/articles/init")).andExpect(status().isOk());

        verify(service).initArticles();
    }
}
