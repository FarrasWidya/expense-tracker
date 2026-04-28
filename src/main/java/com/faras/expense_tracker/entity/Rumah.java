package com.faras.expense_tracker.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class Rumah {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String emoji;
    private String color;

    @Column(unique = true, nullable = false)
    private UUID inviteToken;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public UUID getInviteToken() { return inviteToken; }
    public void setInviteToken(UUID inviteToken) { this.inviteToken = inviteToken; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public User getAdmin() { return admin; }
    public void setAdmin(User admin) { this.admin = admin; }
}
