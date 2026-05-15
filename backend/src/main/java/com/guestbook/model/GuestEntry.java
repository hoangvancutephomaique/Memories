package com.guestbook.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@Entity
@Table(name = "guest_entries")
public class GuestEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be 100 characters or fewer")
    @Column(nullable = false, length = 100)
    private String name;

    /** Reserved for optional public contact email (unused). */
    @JsonIgnore
    @Size(max = 150, message = "Email must be 150 characters or fewer")
    @Column(length = 150)
    private String email;

    @NotBlank(message = "Message is required")
    @Size(max = 500000, message = "Message must be 500000 characters or fewer")
    @Column(nullable = false, length = 500000)
    private String message;

    /** Facebook profile name from Graph API; not exposed in JSON. */
    @JsonIgnore
    @Column(name = "facebook_name", length = 200)
    private String facebookName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getFacebookName() { return facebookName; }
    public void setFacebookName(String facebookName) { this.facebookName = facebookName; }
    public Instant getCreatedAt() { return createdAt; }
}
