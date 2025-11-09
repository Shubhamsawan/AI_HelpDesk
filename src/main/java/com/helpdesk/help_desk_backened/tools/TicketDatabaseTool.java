    package com.helpdesk.help_desk_backened.tools;

    import com.helpdesk.help_desk_backened.entity.Ticket;
    import com.helpdesk.help_desk_backened.service.TicketService;

    import org.springframework.stereotype.Component;

    @Component
    public class TicketDatabaseTool {

        private final TicketService ticketService;

        public TicketDatabaseTool(TicketService ticketService) {
            this.ticketService = ticketService;
        }



        public Ticket createTicketTool( Ticket ticket){
            return ticketService.createTicket(ticket);
        }

        public Ticket getTicketByUserName(String username) {
            return ticketService.getTicketByUserName(username);
        }

        public Ticket updateTicket( Ticket ticket){
            return ticketService.updateTicket(ticket);
        }

        public String getCurrentTime(){
            return String.valueOf(System.currentTimeMillis());
        }
    }
