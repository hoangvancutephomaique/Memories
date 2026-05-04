package com.guestbook.controller;

import com.guestbook.model.GuestEntry;
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

    public GuestEntryController(GuestEntryService service) {
        this.service = service;
    }

    @GetMapping
    public List<GuestEntry> getAll() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<GuestEntry> create(@Valid @RequestBody GuestEntry entry) {
        GuestEntry saved = service.save(entry);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
