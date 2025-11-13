package com.helpdesk.help_desk_backened.entity;

import com.helpdesk.help_desk_backened.enums.Priority;
import com.helpdesk.help_desk_backened.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "help_desk_tickets")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    private String summary;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private String category;

    @Column(nullable = false)
    private String email;

    @Column(unique = true)
    private String userName;

    private LocalDate createdOn;

    private LocalDate updatedOn;

    @Enumerated(EnumType.STRING)
    private Status status;

    @PrePersist
    void preSaved() {
        if (this.createdOn == null) {
            this.createdOn = LocalDate.now();
        }
        this.updatedOn = LocalDate.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedOn = LocalDate.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDate getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDate createdOn) {
        this.createdOn = createdOn;
    }

    public LocalDate getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(LocalDate updatedOn) {
        this.updatedOn = updatedOn;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
