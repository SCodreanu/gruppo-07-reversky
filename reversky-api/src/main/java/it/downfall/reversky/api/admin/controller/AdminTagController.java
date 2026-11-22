package it.downfall.reversky.api.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.api.catalog.dto.TagRequest;
import it.downfall.reversky.api.catalog.dto.TagResponse;
import it.downfall.reversky.api.catalog.service.TagService;
import it.downfall.reversky.api.common.config.OpenApiConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Tag del catalogo dal backoffice (docs/backoffice, API-07): solo ADMIN.
 */
@Tag(name = "Admin: tags", description = "Backoffice management of the tags used by the pack filters")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin/tags")
@RequiredArgsConstructor
public class AdminTagController {

	private final TagService tagService;

	@Operation(summary = "List the tags", description = "Returns every tag, sorted by name.")
	@ApiResponse(responseCode = "200", description = "Tags returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@GetMapping
	public ResponseEntity<List<TagResponse>> findAll() {
		return ResponseEntity.ok(tagService.findAll());
	}

	@Operation(summary = "Create a tag", description = "The name must be unique and contain only uppercase letters, "
		+ "digits and underscores (for example CHRISTMAS_2026). It is not trimmed or uppercased.")
	@ApiResponse(responseCode = "201", description = "Tag created")
	@ApiResponse(responseCode = "400", description = "Missing name, name too long or not in the format")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "409", description = "Name already used by another tag (TAG_NAME_TAKEN)")
	@PostMapping
	public ResponseEntity<TagResponse> create(@Valid @RequestBody TagRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(request));
	}

	@Operation(summary = "Rename a tag", description = "Cards, factions and packs with the tag keep it. The name must "
		+ "stay unique and in the same format.")
	@ApiResponse(responseCode = "200", description = "Tag renamed")
	@ApiResponse(responseCode = "400", description = "Missing name, name too long or not in the format")
	@ApiResponse(responseCode = "401", description = "Missing or invalid access token")
	@ApiResponse(responseCode = "403", description = "Not an admin")
	@ApiResponse(responseCode = "404", description = "Tag not found")
	@ApiResponse(responseCode = "409", description = "Name already used by another tag (TAG_NAME_TAKEN)")
	@PutMapping("/{id}")
	public ResponseEntity<TagResponse> update(@PathVariable Integer id, @Valid @RequestBody TagRequest request) {
		return ResponseEntity.ok(tagService.update(id, request));
	}
}
