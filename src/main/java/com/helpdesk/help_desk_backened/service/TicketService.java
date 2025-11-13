package com.helpdesk.help_desk_backened.service;

import com.helpdesk.help_desk_backened.entity.Ticket;
import com.helpdesk.help_desk_backened.repository.TicketRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Getter
@Setter
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

@Transactional
    public Ticket createTicket(Ticket ticket){
        ticket.setId(null);
        return ticketRepository.save(ticket);
    }

    public Ticket updateTicket(Ticket ticket){
        return ticketRepository.save(ticket);
    }

    public Ticket getTicket(Long ticketId) {
        return ticketRepository.findById(ticketId).orElse(null);

    }

    public Ticket getTicketByUserName(String username){
        return ticketRepository.findByEmail(username).orElse(null);
    }

}
