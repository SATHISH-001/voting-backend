package com.voting.controller;

import com.voting.model.User;
import com.voting.repository.UserRepository;
import com.voting.security.JwtUtil;
import com.voting.service.AuthService;
import com.voting.service.FileStorageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final FileStorageService storage;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> register(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String voterId,
            @RequestParam(required = false) MultipartFile faceImage) {

        authService.register(name, email, password, phone, address, voterId, faceImage);

        Map<String,Object> res = new HashMap<>();
        res.put("message","Registration successful. Check your email for OTP.");

        return ResponseEntity.ok(res);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpReq req) {

        authService.verifyOtp(req.getEmail(), req.getOtp());

        Map<String,Object> res = new HashMap<>();
        res.put("message","Email verified. You can now login.");

        return ResponseEntity.ok(res);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> body) {

        authService.resendOtp(body.get("email"));

        Map<String,Object> res = new HashMap<>();
        res.put("message","New OTP sent.");

        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReq req) {

        String token = authService.login(req.getEmail(), req.getPassword());
        User u = userRepo.findByEmail(req.getEmail()).orElseThrow();

        Map<String,Object> res = new HashMap<>();

        res.put("token",token);
        res.put("userId",u.getId());
        res.put("name",u.getName());
        res.put("email",u.getEmail());
        res.put("role",u.getRole().name());
        res.put("hasFace",u.hasFace());
        res.put("voterId",u.getVoterId()!=null?u.getVoterId():"");
        res.put("voterIdVerified",u.getVoterIdVerified());
        res.put("voterIdRejected",u.getVoterIdRejected());
        res.put("voterIdRejectMsg",u.getVoterIdRejectMsg()!=null?u.getVoterIdRejectMsg():"");
        res.put("canVote",u.canVote());

        return ResponseEntity.ok(res);
    }

    /** GET profile */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal User currentUser) {

        User u = authService.getProfile(currentUser.getId());

        Map<String,Object> res = new HashMap<>();

        res.put("id",u.getId());
        res.put("name",u.getName());
        res.put("email",u.getEmail());
        res.put("phone",u.getPhone()!=null?u.getPhone():"");
        res.put("address",u.getAddress()!=null?u.getAddress():"");
        res.put("voterId",u.getVoterId()!=null?u.getVoterId():"");
        res.put("voterIdVerified",u.getVoterIdVerified());
        res.put("voterIdRejected",u.getVoterIdRejected());
        res.put("voterIdRejectMsg",u.getVoterIdRejectMsg()!=null?u.getVoterIdRejectMsg():"");
        res.put("hasFace",u.hasFace());
        res.put("canVote",u.canVote());
        res.put("verified",u.getVerified());

        return ResponseEntity.ok(res);
    }

    /** update voter id */
    @PutMapping("/voter-id")
    public ResponseEntity<?> updateVoterId(@RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal User currentUser) {

        User u = authService.updateVoterId(currentUser.getId(), body.get("voterId"));

        Map<String,Object> res = new HashMap<>();

        res.put("message","Voter ID submitted for verification.");
        res.put("voterId",u.getVoterId());
        res.put("voterIdVerified",u.getVoterIdVerified());
        res.put("voterIdRejected",u.getVoterIdRejected());

        return ResponseEntity.ok(res);
    }

    @Data
    static class LoginReq {
        private String email;
        private String password;
    }

    @Data
    static class OtpReq {
        private String email;
        private String otp;
    }
}