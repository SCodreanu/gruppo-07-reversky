package it.downfall.reversky.api.common.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * {@link PageResponse}: test unitario, senza Spring né database.
 * <p>
 * {@link PageImpl} è la pagina che restituiscono i repository di Spring Data.
 */
class PageResponseTest {

	@Test
	@DisplayName("A middle page copies its content and all the numbers")
	void from_middlePage_copiesContentAndNumbers() {
		// Arrange: seconda pagina da 2 elementi su 5 in totale
		Page<String> page = new PageImpl<>(List.of("c", "d"), PageRequest.of(1, 2), 5);

		// Act
		PageResponse<String> response = PageResponse.from(page);

		// Assert
		assertThat(response.content()).containsExactly("c", "d");
		assertThat(response.page()).isEqualTo(1);
		assertThat(response.size()).isEqualTo(2);
		assertThat(response.totalElements()).isEqualTo(5);
		assertThat(response.totalPages()).isEqualTo(3);
	}

	@Test
	@DisplayName("An empty result has no content and zero totals")
	void from_emptyResult_returnsZeroTotals() {
		// Arrange
		Page<String> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

		// Act
		PageResponse<String> response = PageResponse.from(page);

		// Assert
		assertThat(response.content()).isEmpty();
		assertThat(response.totalElements()).isZero();
		assertThat(response.totalPages()).isZero();
	}

	@Test
	@DisplayName("A page converted with map keeps the same totals")
	void from_mappedPage_keepsTotals() {
		// Arrange: è l'uso previsto nei service, page.map(XxxResponse::from)
		Page<String> page = new PageImpl<>(List.of("abc", "de"), PageRequest.of(0, 2), 3);

		// Act
		PageResponse<Integer> response = PageResponse.from(page.map(String::length));

		// Assert
		assertThat(response.content()).containsExactly(3, 2);
		assertThat(response.totalElements()).isEqualTo(3);
		assertThat(response.totalPages()).isEqualTo(2);
	}
}
