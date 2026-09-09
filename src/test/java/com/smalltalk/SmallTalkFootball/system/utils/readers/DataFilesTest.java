package com.smalltalk.SmallTalkFootball.system.utils.readers;

import com.smalltalk.SmallTalkFootball.domain.Article;
import com.smalltalk.SmallTalkFootball.domain.SmallInfo;
import com.smalltalk.SmallTalkFootball.models.InfoText;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The seeded content under src/main/resources/data is loaded by POST /articles/init and
 * POST /small-infos/init, both of which delete the whole collection before reloading. A
 * malformed file therefore empties a collection in production, so it is worth catching here.
 * <p>
 * The readers resolve relative paths, which works because Surefire runs with the project
 * directory as its working directory.
 */
class DataFilesTest {

    @Test
    void everyShippedArticleParses() {
        List<Article> articles = ArticleReader.read();

        assertThat(articles).isNotEmpty();
        assertThat(articles).allSatisfy(article -> {
            assertThat(article.getTitle()).isNotBlank();
            assertThat(article.getAuthor()).isNotBlank();
            assertThat(article.getText()).isNotBlank();
        });
    }

    @Test
    void articleTitlesAreUniqueBecauseTheyAreUniquelyIndexed() {
        List<Article> articles = ArticleReader.read();

        assertThat(articles).extracting(Article::getTitle).doesNotHaveDuplicates();
    }

    @Test
    void everyShippedInfoParses() {
        List<SmallInfo> infos = SmallInfosReader.read();

        assertThat(infos).isNotEmpty();
        assertThat(infos).allSatisfy(info -> {
            assertThat(info.getTitle()).isNotBlank();
            assertThat(info.getInfoCategory())
                    .withFailMessage("Info '%s' has no category, so it would break the sort in getAllInfos()",
                            info.getTitle())
                    .isNotNull();
            assertThat(info.getInfoTexts()).isNotEmpty();
        });
    }

    @Test
    void everyInfoTextCarriesItsLanguage() {
        List<SmallInfo> infos = SmallInfosReader.read();

        assertThat(infos).allSatisfy(info ->
                assertThat(info.getInfoTexts()).allSatisfy(infoText -> {
                    assertThat(infoText.getText()).isNotBlank();
                    assertThat(infoText.getLang())
                            .withFailMessage("An info text of '%s' has no language", info.getTitle())
                            .isNotNull();
                }));
    }

    @Test
    void infoTitlesAreUniqueBecauseTheyAreUniquelyIndexed() {
        List<SmallInfo> infos = SmallInfosReader.read();

        assertThat(infos).extracting(SmallInfo::getTitle).doesNotHaveDuplicates();
    }

    @Test
    void eachInfoHasAtMostOneTextPerLanguage() {
        List<SmallInfo> infos = SmallInfosReader.read();

        assertThat(infos).allSatisfy(info ->
                assertThat(info.getInfoTexts()).extracting(InfoText::getLang)
                        .withFailMessage("Info '%s' has more than one text for the same language", info.getTitle())
                        .doesNotHaveDuplicates());
    }
}
