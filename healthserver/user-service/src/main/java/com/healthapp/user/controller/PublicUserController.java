package com.healthapp.user.controller;

import com.healthapp.user.dto.response.ApiResponse;
import com.healthapp.user.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@Slf4j
public class PublicUserController {
    
    private final PasswordResetService passwordResetService;
    
    /**
     * ✅ FIXED: Forgot password - Envoie réellement l'email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @RequestBody Map<String, String> request) {
        
        String email = request.get("email");
        
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Email is required"));
        }
        
        log.info("🔐 Password reset requested for user: {}", email);
        
        try {
            // ✅ Appeler le service pour envoyer l'email
            passwordResetService.sendPasswordResetEmailForUser(email);
            
            return ResponseEntity.ok(
                ApiResponse.success("Password reset email sent successfully", null)
            );
            
        } catch (Exception e) {
            log.error("❌ Failed to send password reset email: {}", e.getMessage());
            
            // ⚠️ NE PAS révéler si l'email existe ou pas (sécurité)
            return ResponseEntity.ok(
                ApiResponse.success("If the email exists, a reset link will be sent", null)
            );
        }
    }
}