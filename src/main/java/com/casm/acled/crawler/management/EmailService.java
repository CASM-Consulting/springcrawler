package com.casm.acled.crawler.management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * Created by Andrew D. Robertson on 02/11/2020.
 *
 * Ensure you've set the required properties in application.properties:
 *
 *   spring.mail.host=smtp.gmail.com
 *   spring.mail.port=587
 *   spring.mail.username=*********
 *   spring.mail.password=*********
 *   spring.mail.properties.mail.smtp.auth=true
 *   spring.mail.properties.mail.smtp.starttls.enable=true
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private ConfigService configService;

    /**
     * Send a simple plain text email.
     */
    public void sendSimpleMessage(String to, String subject, String text){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@casmtechnology.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }

    /**
     * Send HTML-supported text as email to the address configured
     * in application.properties under: crawler.configservice.email
     *
     * If no email is configured, then no email is sent.
     */
    public void sendHtmlMessage(String subject, String html){
        if (configService.isEmailConfigured()){
            sendHtmlMessage(configService.getEmail(), subject, html);
        }
    }

    /**
     * Send HTML-supported text as email.
     */
    public void sendHtmlMessage(String to, String subject, String html)  {
        try {
            MimeMessage message = emailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, false);

            helper.setFrom("noreply@casmtechnology.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            emailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
