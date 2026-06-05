package com.kidsgame.mathapp.catalog;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/word-catalog")
@PreAuthorize("@adminAccess.isAdmin(authentication)")
public class WordCatalogAdminController {
    private final WordCatalogService service;

    public WordCatalogAdminController(WordCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<WordCatalogEntryResponse> list() {
        return service.list();
    }

    @PostMapping("/suggest")
    public WordCatalogSuggestionResponse suggest(@Valid @RequestBody WordCatalogSuggestionRequest request) {
        return service.suggest(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WordCatalogEntryResponse create(@Valid @RequestBody WordCatalogUpsertRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public WordCatalogEntryResponse update(@PathVariable Long id, @Valid @RequestBody WordCatalogUpsertRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
