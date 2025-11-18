package com.helpdesk.help_desk_backened.tools;

import org.springframework.stereotype.Component;

@Component
public class EmailTool {

    public void sendEmailTOSupportTeam(String email, String message) {
        System.out.println("Going to send email to support team....");
        System.out.println("email id: " + email);
        System.out.println("mesage : " + message);
    }
}
