package com.substring.helpdesk.help_desk_backened.entity;

import com.substring.helpdesk.help_desk_backened.enums.Priority;
import com.substring.helpdesk.help_desk_backened.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "help_desk_tickets")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    private String summary;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(unique = true)
    private String userName;

    private LocalDate createdOn;

    private LocalDate updatedOn;

    @Enumerated(EnumType.STRING)
    private Status status;

    @PrePersist
    void preSaved(){
        if(this.createdOn == null){
            this.createdOn = LocalDate.now();
        }
        this.updatedOn = LocalDate.now();
    }

    @PreUpdate
    void preUpdate(){
        this.updatedOn = LocalDate.now();
    }

}
