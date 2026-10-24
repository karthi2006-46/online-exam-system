// =====================================================================
// WELCOME EMAIL ON SIGN UP  (Spring Boot example - adapt to your project)
//
// Why on the server? An email can only be sent safely from the backend.
// Putting SMTP details in JavaScript would expose them to everyone.
//
// 1) pom.xml - add:
//      <dependency>
//          <groupId>org.springframework.boot</groupId>
//          <artifactId>spring-boot-starter-mail</artifactId>
//      </dependency>
//
// 2) application.properties (use environment variables, never commit real passwords):
//      spring.mail.host=smtp.gmail.com
//      spring.mail.port=587
//      spring.mail.username=${MAIL_USERNAME}
//      spring.mail.password=${MAIL_APP_PASSWORD}
//      spring.mail.properties.mail.smtp.auth=true
//      spring.mail.properties.mail.smtp.starttls.enable=true
//      app.mail.from=${MAIL_USERNAME}
//      app.base-url=http://localhost:8080
//    (Gmail needs an "App password". For production use a mail provider
//     such as SendGrid, Amazon SES or Brevo.)
//
// 3) Add @EnableAsync to your main application class (or any @Configuration)
//    so a mail problem never slows down or breaks the signup request.
//
// 4) Call it after the new user is saved, in your signup service:
//      emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFirstName());
//
// Spring Boot 2.x: change the jakarta.mail imports to javax.mail.
// Java 8/11: replace the text block with normal string concatenation.
// Never put the user's password in the email.
// =====================================================================

package com.lms.service;   // <- change to your package

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.base-url}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String firstName) {
        try {
            String name = HtmlUtils.htmlEscape(firstName == null ? "there" : firstName);
            String loginUrl = baseUrl + "/login.html";

            String plain = "Hi " + firstName + ",\n\n"
                    + "Welcome to the Online Exam System! Your account has been created successfully.\n"
                    + "You can log in here: " + loginUrl + "\n\n"
                    + "If you did not create this account, please ignore this email.\n";

            String html = """
                <div style="font-family:Segoe UI,Arial,sans-serif;background:#f3f6fc;padding:32px 16px;">
                  <div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:18px;
                              padding:36px 32px;border:1px solid #e3e9f4;">
                    <div style="width:44px;height:44px;border-radius:13px;background:#2f62e6;color:#fff;
                                font-size:22px;font-weight:800;text-align:center;line-height:44px;">L</div>
                    <h2 style="color:#0f1b3d;margin:22px 0 8px;font-size:22px;">Welcome, %s!</h2>
                    <p style="color:#62708f;font-size:15px;line-height:1.7;margin:0 0 24px;">
                      Your Online Exam System account has been created successfully.
                      You can now log in to access your courses, exams and coding challenges.
                    </p>
                    <a href="%s" style="display:inline-block;background:#2f62e6;color:#ffffff;text-decoration:none;
                                        font-weight:700;padding:13px 26px;border-radius:12px;">Login to your account</a>
                    <p style="color:#8b97b1;font-size:12.5px;line-height:1.6;margin:28px 0 0;">
                      If you did not create this account, you can safely ignore this email.
                    </p>
                  </div>
                </div>
                """.formatted(name, loginUrl);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to the Online Exam System");
            helper.setText(plain, html);   // plain-text + HTML versions

            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);

        } catch (MessagingException | RuntimeException e) {
            // Do not fail the signup because of an email problem
            log.error("Could not send welcome email to {}", toEmail, e);
        }
    }
}