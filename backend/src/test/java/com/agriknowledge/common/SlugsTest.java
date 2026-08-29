package com.agriknowledge.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlugsTest {

	@ParameterizedTest
	@CsvSource({
			"Soil pH and nutrients, soil-ph-and-nutrients",
			"  Leading and trailing  , leading-and-trailing",
			"Multiple   spaces, multiple-spaces",
			"Punctuation! Everywhere?, punctuation-everywhere",
			"ICAR JRF 2026, icar-jrf-2026",
			"Mixed CASE Title, mixed-case-title",
	})
	void buildsReadableSlugs(String input, String expected) {
		assertThat(Slugs.from(input)).isEqualTo(expected);
	}

	@Test
	@DisplayName("accents lose their marks but keep their letters")
	void stripsAccentsWithoutLosingLetters() {
		// The naive approach drops the whole character and yields "pomolog".
		assertThat(Slugs.from("Pomología")).isEqualTo("pomologia");
		assertThat(Slugs.from("Café Crop")).isEqualTo("cafe-crop");
	}

	@Test
	void refusesNamesWithNothingUsableInThem() {
		assertThatThrownBy(() -> Slugs.from("!!!"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Slugs.from("   "))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Slugs.from(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("a taken slug gets a numeric suffix rather than colliding")
	void suffixesUntilFree() {
		Set<String> taken = Set.of("soil-ph", "soil-ph-2");

		assertThat(Slugs.uniqueFrom("Soil pH", taken::contains)).isEqualTo("soil-ph-3");
		assertThat(Slugs.uniqueFrom("Soil Moisture", taken::contains)).isEqualTo("soil-moisture");
	}

	@Test
	void trimsOverlongNamesAtAWordBoundary() {
		String slug = Slugs.from("word ".repeat(100));

		assertThat(slug.length()).isLessThanOrEqualTo(180);
		// Trimming mid-word would leave a dangling fragment.
		assertThat(slug).doesNotEndWith("-");
		assertThat(slug).endsWith("word");
	}

}
