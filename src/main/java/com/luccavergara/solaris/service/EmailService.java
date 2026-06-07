package com.luccavergara.solaris.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${application.email.resend-api-key}")
    private String resendApiKey;

    @Value("${application.email.from}")
    private String emailFrom;

    public void sendEmailVerification(String to, String verificationLink) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            System.out.println("==================================================");
            System.out.println("RESEND_API_KEY not configured.");
            System.out.println("SOLARIS EMAIL VERIFICATION LINK:");
            System.out.println(verificationLink);
            System.out.println("==================================================");
            return;
        }

        Resend resend = new Resend(resendApiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Solaris <" + emailFrom + ">")
                .to(to)
                .subject("Verify your Solaris account")
                .html(buildVerificationEmail(verificationLink))
                .build();

        try {
            resend.emails().send(params);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new IllegalStateException("Could not send verification email: " + exception.getMessage());
        }
    }

    public void sendPasswordReset(String to, String resetLink) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            System.out.println("==================================================");
            System.out.println("RESEND_API_KEY not configured.");
            System.out.println("SOLARIS PASSWORD RESET LINK:");
            System.out.println(resetLink);
            System.out.println("==================================================");
            return;
        }

        Resend resend = new Resend(resendApiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Solaris <" + emailFrom + ">")
                .to(to)
                .subject("Reset your Solaris password")
                .html(buildPasswordResetEmail(resetLink))
                .build();

        try {
            resend.emails().send(params);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new IllegalStateException("Could not send password reset email: " + exception.getMessage());
        }
    }

    private String buildVerificationEmail(String verificationLink) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 560px; margin: 0 auto; padding: 24px;">
                    <h1 style="color: #0f172a;">Welcome to Solaris ☀</h1>
                    <p style="color: #334155; font-size: 16px;">
                        Thanks for creating your Solaris account. Please verify your email address to activate your workspace.
                    </p>
                    <a href="%s"
                       style="display: inline-block; margin-top: 16px; background: #2563eb; color: white; padding: 12px 18px; border-radius: 10px; text-decoration: none; font-weight: 600;">
                        Verify email
                    </a>
                    <p style="color: #64748b; font-size: 13px; margin-top: 24px;">
                        This link expires in 24 hours.
                    </p>
                </div>
                """.formatted(verificationLink);
    }

    private String buildPasswordResetEmail(String resetLink) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 560px; margin: 0 auto; padding: 24px;">
                <h1 style="color: #0f172a;">Reset your Solaris password</h1>
                <p style="color: #334155; font-size: 16px;">
                    We received a request to reset your Solaris password. Click the button below to choose a new one.
                </p>
                <a href="%s"
                   style="display: inline-block; margin-top: 16px; background: #2563eb; color: white; padding: 12px 18px; border-radius: 10px; text-decoration: none; font-weight: 600;">
                    Reset password
                </a>
                <p style="color: #64748b; font-size: 13px; margin-top: 24px;">
                    This link expires in 1 hour. If you did not request this, you can ignore this email.
                </p>
            </div>
            """.formatted(resetLink);
    }
}