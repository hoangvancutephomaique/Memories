package com.guestbook.service;

import com.guestbook.model.GuestEntry;
import com.guestbook.repository.GuestEntryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GuestEntryService {

    private final GuestEntryRepository repository;

    public GuestEntryService(GuestEntryRepository repository) {
        this.repository = repository;
    }

    public List<GuestEntry> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public GuestEntry save(GuestEntry entry) {
        return repository.save(entry);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
