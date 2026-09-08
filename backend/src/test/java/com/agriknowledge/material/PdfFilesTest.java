package com.agriknowledge.material;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PdfFilesTest {

	private static byte[] bytes(String content) {
		return content.getBytes(StandardCharsets.UTF_8);
	}

	// ----- signature ---------------------------------------------------------

	@Test
	void acceptsARealPdfHeader() {
		assertThat(PdfFiles.looksLikePdf(bytes("%PDF-1.7\nrest of the file"))).isTrue();
	}

	@Test
	@DisplayName("a renamed file is rejected however convincing its name was")
	void rejectsSomethingThatIsNotAPdf() {
		assertThat(PdfFiles.looksLikePdf(bytes("<!DOCTYPE html><html>"))).isFalse();
		assertThat(PdfFiles.looksLikePdf(bytes("PK\u0003\u0004"))).isFalse();
	}

	@Test
	void rejectsAFileTooShortToHaveASignature() {
		assertThat(PdfFiles.looksLikePdf(bytes("%PDF"))).isFalse();
		assertThat(PdfFiles.looksLikePdf(new byte[0])).isFalse();
		assertThat(PdfFiles.looksLikePdf(null)).isFalse();
	}

	@Test
	@DisplayName("the signature must be at the start, not merely present")
	void rejectsAPdfSignatureThatIsNotLeading() {
		assertThat(PdfFiles.looksLikePdf(bytes("GIF89a%PDF-1.4"))).isFalse();
	}

	// ----- filenames ---------------------------------------------------------

	@Test
	void keepsAnOrdinaryName() {
		assertThat(PdfFiles.safeFileName("ICAR JRF 2024.pdf")).isEqualTo("ICAR JRF 2024.pdf");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"../../../etc/passwd.pdf",
			"..\\..\\windows\\system32\\config.pdf",
			"/absolute/path/paper.pdf",
			"C:\\Users\\admin\\paper.pdf",
	})
	@DisplayName("no traversal survives, whichever separator was used")
	void stripsPathComponents(String hostile) {
		String safe = PdfFiles.safeFileName(hostile);
		assertThat(safe).doesNotContain("/").doesNotContain("\\").doesNotContain("..");
	}

	@Test
	@DisplayName("quotes cannot break out of the Content-Disposition header")
	void removesCharactersThatWouldEscapeTheHeader() {
		String safe = PdfFiles.safeFileName("paper\"; filename=\"evil.exe");
		assertThat(safe).doesNotContain("\"").endsWith(".pdf");
	}

	@Test
	void collapsesToAFallbackWhenNothingUsableRemains() {
		assertThat(PdfFiles.safeFileName("???.pdf")).isEqualTo("document.pdf");
		assertThat(PdfFiles.safeFileName("")).isEqualTo("document.pdf");
		assertThat(PdfFiles.safeFileName(null)).isEqualTo("document.pdf");
	}

	@Test
	void alwaysEndsWithASinglePdfExtension() {
		assertThat(PdfFiles.safeFileName("paper.pdf")).isEqualTo("paper.pdf");
		assertThat(PdfFiles.safeFileName("paper")).isEqualTo("paper.pdf");
		assertThat(PdfFiles.safeFileName("paper.PDF")).isEqualTo("paper.pdf");
	}

	@Test
	void truncatesAnAbsurdlyLongName() {
		String safe = PdfFiles.safeFileName("a".repeat(500) + ".pdf");
		assertThat(safe.length()).isLessThanOrEqualTo(125);
		assertThat(safe).endsWith(".pdf");
	}

	// ----- keys --------------------------------------------------------------

	@Test
	void keysAreNamespacedByYearAndNeverCollide() {
		String first = PdfFiles.storageKey(2024, "icar-jrf-2024");
		String second = PdfFiles.storageKey(2024, "icar-jrf-2024");

		assertThat(first).startsWith("papers/2024/icar-jrf-2024-").endsWith(".pdf");
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void keyFallsBackToTheCurrentYearWhenThePaperHasNone() {
		assertThat(PdfFiles.storageKey(null, "notes")).matches("papers/\\d{4}/notes-[0-9a-f]{8}\\.pdf");
	}

}
