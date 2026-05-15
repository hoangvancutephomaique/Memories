package com.guestbook.controller;

import com.guestbook.config.DeleteSecretGuard;
import com.guestbook.dto.CreateEntryRequest;
import com.guestbook.model.GuestEntry;
import com.guestbook.service.FacebookIdentityService;
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
    private final FacebookIdentityService facebookIdentityService;
    private final DeleteSecretGuard deleteSecretGuard;

    public GuestEntryController(
            GuestEntryService service,
            FacebookIdentityService facebookIdentityService,
            DeleteSecretGuard deleteSecretGuard) {
        this.service = service;
        this.facebookIdentityService = facebookIdentityService;
        this.deleteSecretGuard = deleteSecretGuard;
    }

    @GetMapping
    public List<GuestEntry> getAll() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<GuestEntry> create(@Valid @RequestBody CreateEntryRequest request) {
        String facebookName;
        try {
            facebookName = facebookIdentityService.resolveName(request.facebookAccessToken());
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
        entry.setFacebookName(facebookName);

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
