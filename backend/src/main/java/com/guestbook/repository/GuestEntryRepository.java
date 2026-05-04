package com.guestbook.repository;

import com.guestbook.model.GuestEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestEntryRepository extends JpaRepository<GuestEntry, Long> {
    List<GuestEntry> findAllByOrderByCreatedAtDesc();
}
