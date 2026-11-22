package it.downfall.reversky.api.common.storage;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/**
 * Dove si salvano le immagini caricate dal backoffice e quanto possono pesare (docs/backoffice, "Illustrazioni"):
 * configurazione tecnica, non regole di gioco.
 *
 * @param imagesDir    cartella delle immagini; con {@code ./gradlew bootRun} è {@code uploads} dentro reversky-api,
 *                     nel container {@code /app/uploads} su un volume Docker (variabile IMAGES_DIR)
 * @param maxImageSize dimensione massima di un'immagine
 */
@Validated
@ConfigurationProperties(prefix = "reversky.storage")
public record ImageStorageProperties(

	@NotNull
	Path imagesDir,

	@NotNull
	DataSize maxImageSize
) {
}
