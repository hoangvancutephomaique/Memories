package com.guestbook.controller;

import com.guestbook.config.DeleteSecretGuard;
import com.guestbook.dto.CreateEntryRequest;
import com.guestbook.dto.GoogleProfile;
import com.guestbook.model.GuestEntry;
import com.guestbook.service.GoogleIdentityService;
import com.guestbook.service.GuestEntryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entries")
public class GuestEntryController {

    private final GuestEntryService service;
    private final GoogleIdentityService googleIdentityService;
    private final DeleteSecretGuard deleteSecretGuard;

    public GuestEntryController(
            GuestEntryService service,
            GoogleIdentityService googleIdentityService,
            DeleteSecretGuard deleteSecretGuard) {
        this.service = service;
        this.googleIdentityService = googleIdentityService;
        this.deleteSecretGuard = deleteSecretGuard;
    }

    @GetMapping
    public List<GuestEntry> getAll() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<GuestEntry> create(@Valid @RequestBody CreateEntryRequest request) {
        GoogleProfile profile;
        try {
            profile = googleIdentityService.resolveProfile(request.googleIdToken());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        GuestEntry entry = new GuestEntry();
        String publicName = request.name() == null || request.name().isBlank()
                ? "Anonymous"
                : request.name().trim();
        entry.setName(publicName);
        entry.setMessage(request.message().trim());
        entry.setGoogleDisplayName(profile.displayName());
        entry.setGoogleAccountEmail(profile.email());

        GuestEntry saved = service.save(entry);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Delete-Secret", required = false) String deleteSecret) {
        if (deleteSecretGuard.requiresDeleteSecret() && !deleteSecretGuard.headerMatches(deleteSecret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
