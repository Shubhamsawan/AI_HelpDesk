package com.helpdesk.help_desk_backened.repository;

import com.helpdesk.help_desk_backened.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketRepository extends JpaRepository< Ticket,Long> {

    Optional<Ticket> findByUserName(String userName);
}
