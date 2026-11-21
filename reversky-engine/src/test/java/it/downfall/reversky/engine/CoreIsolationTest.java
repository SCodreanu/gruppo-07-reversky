package it.downfall.reversky.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regola 1 di AGENTS.md e CodingRequirements §4: il package {@code core} importa solo il JDK, così gira in un
 * semplice {@code main} o in un test JUnit senza Spring.
 */
class CoreIsolationTest {

	private static final Path CORE = Path.of("src/main/java/it/downfall/reversky/engine/core");
	private static final List<String> FORBIDDEN = List.of("import org.springframework.", "import lombok.",
			"import tools.jackson.", "import com.fasterxml.", "import jakarta.");

	@Test
	@DisplayName("Nessun file del core importa Spring, Lombok, Jackson o Jakarta")
	void core_importsOnlyTheJdk() throws IOException {
		List<String> violations;
		try (Stream<Path> files = Files.walk(CORE)) {
			violations = files.filter(path -> path.toString().endsWith(".java"))
					.flatMap(CoreIsolationTest::forbiddenImports)
					.toList();
		}

		assertThat(violations).isEmpty();
	}

	private static Stream<String> forbiddenImports(Path file) {
		try {
			return Files.readAllLines(file).stream()
					.map(String::strip)
					.filter(line -> FORBIDDEN.stream().anyMatch(line::startsWith))
					.map(line -> CORE.relativize(file) + ": " + line);
		} catch (IOException e) {
			throw new IllegalStateException("Impossibile leggere " + file, e);
		}
	}
}
