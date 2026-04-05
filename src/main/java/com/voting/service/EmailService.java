package com.voting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mail;

    private void send(String to, String subject, String body) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setTo(to); m.setSubject(subject); m.setText(body);
        mail.send(m);
    }

    public void sendOtp(String email, String otp) {
        send(email, "SecureVote – Email Verification OTP",
            "Your OTP is: " + otp + "\n\nExpires in 10 minutes. Do not share it.\n\nSecureVote Team");
    }

    public void sendVoteConfirmation(String email, String name, String pollTitle, String option) {
        send(email, "SecureVote – Vote Recorded ✅",
            "Dear " + name + ",\n\n" +
            "Your vote has been successfully recorded!\n\n" +
            "Poll  : " + pollTitle + "\n" +
            "Choice: " + option + "\n\n" +
            "Your vote is encrypted and cannot be changed.\n\nSecureVote Team");
    }

    public void sendVerificationReminder(String email, String name) {
        send(email, "SecureVote – Please Verify Your Account",
            "Dear " + name + ",\n\nYour account is pending verification.\n" +
            "Please login and complete OTP verification.\n\nSecureVote Team");
    }

    public void sendVoterIdRejected(String email, String name, String reason) {
        send(email, "SecureVote – Voter ID Requires Update",
            "Dear " + name + ",\n\n" +
            "Your Voter ID could not be verified. Reason:\n\n  " + reason + "\n\n" +
            "Please login to your account and update your Voter ID.\n\nSecureVote Team");
    }

    public void sendVoterIdVerified(String email, String name) {
        send(email, "SecureVote – Voter ID Verified ✅",
            "Dear " + name + ",\n\n" +
            "Your Voter ID has been verified by the administrator.\n" +
            "You can now participate in active polls.\n\nSecureVote Team");
    }

    public void sendCustomEmail(String to, String subject, String body) {
        send(to, subject, body);
    }
}
