package it.downfall.reversky.api.common.storage;

import java.time.Duration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * Serve le immagini caricate su {@code GET /images/<percorso>}, senza token (SecurityConfig), così il frontend le
 * usa direttamente in un {@code <img>}. Il resource handler di Spring rifiuta i percorsi che escono dalla cartella.
 * <p>
 * Ogni caricamento ha un nome nuovo e un file non cambia mai: il browser può tenerlo in cache a lungo.
 */
@Configuration
@RequiredArgsConstructor
public class ImageResourceConfig implements WebMvcConfigurer {

	private final ImageStorage imageStorage;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler(ImageStorage.URL_PREFIX + "**")
			.addResourceLocations(imageStorage.getRoot().toUri().toString())
			.setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic());
	}
}
