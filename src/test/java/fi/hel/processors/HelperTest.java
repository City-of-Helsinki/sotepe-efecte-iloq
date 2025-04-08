package fi.hel.processors;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class HelperTest {

    @Inject
    Helper helper;

    @Test
    void testShouldCreateIdentifierWithTwoLettersWhenShortEnough() {
        String email = "matti.meikalainen@domain.com";
        String name = "Matti Juhani Meikäläinen";

        String result = helper.createIdentifier(email, name);

        assertThat(result)
                .isEqualTo("matti.meikalainen@domain.com#MAJUME")
                .hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void testShouldFallbackToOneLetterWhenTooLong() {
        String email = "matti.juhani.meikalainen.virtanen@domain.com";
        String name = "Matti Juhani Meikäläinen";

        String result = helper.createIdentifier(email, name);

        assertThat(result)
                .isEqualTo("matti.juhani.meikalainen.virtanen@domain.com#MJM")
                .hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void testShouldRemoveDomainWhenStillTooLong() {
        String email = "matti.juhani.meikalainen.virtanen.seppo@very.long.domain.com";
        String name = "Matti Juhani Meikäläinen";

        String result = helper.createIdentifier(email, name);

        assertThat(result)
                .isEqualTo("matti.juhani.meikalainen.virtanen.seppo#MJM")
                .hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void testShouldHandleShortNames() {
        String email = "li.virtanen@domain.com";
        String name = "Li Virtanen";

        String result = helper.createIdentifier(email, name);

        assertThat(result)
                .isEqualTo("li.virtanen@domain.com#LIVI")
                .hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void testShouldHandleOneLetterNames() {
        String email = "l.virtanen@domain.com";
        String name = "L Virtanen";

        String result = helper.createIdentifier(email, name);

        assertThat(result)
                .isEqualTo("l.virtanen@domain.com#LVI")
                .hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void testShouldHandleExtraSpacesInName() {
        String email = "matti.meikalainen@domain.com";
        String name = "  Matti   Juhani    Meikäläinen  ";

        String result = helper.createIdentifier(email, name);

        assertThat(result)
                .isEqualTo("matti.meikalainen@domain.com#MAJUME")
                .hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void testShouldHandleEmailWithExtraSpaces() {
        String email = "  matti.meikalainen@domain.com  ";
        String name = "Matti Meikäläinen";

        String result = helper.createIdentifier(email, name);

        assertThat(result)
                .isEqualTo("matti.meikalainen@domain.com#MAME")
                .hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void testShouldHandleEmptyOrNullPartsInName() {
        String email = "matti.meikalainen@domain.com";
        String name = "Matti  Meikäläinen";

        String result = helper.createIdentifier(email, name);

        assertThat(result)
                .isEqualTo("matti.meikalainen@domain.com#MAME")
                .hasSizeLessThanOrEqualTo(50);
    }
}