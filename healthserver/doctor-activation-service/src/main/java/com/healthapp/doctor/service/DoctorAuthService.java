package com.healthapp.doctor.service;

import com.healthapp.doctor.client.NotificationClient;
import com.healthapp.doctor.dto.request.DoctorRegisterRequest;
import com.healthapp.doctor.dto.request.EmailNotificationRequest;
import com.healthapp.doctor.dto.response.DoctorResponse;
import com.healthapp.doctor.entity.Doctor;
import com.healthapp.doctor.entity.DoctorActivationRequest;
import com.healthapp.doctor.repository.DoctorActivationRequestRepository;
import com.healthapp.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DoctorAuthService - Direct doctor registration
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DoctorAuthService {
    
    private final DoctorRepository doctorRepository;
    private final DoctorActivationRequestRepository activationRequestRepository;
    private final NotificationClient notificationClient;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Value("${notification.admin-email}")
    private String adminEmail;
    
    /**
     * Register a new doctor
     */
    public DoctorResponse registerDoctor(DoctorRegisterRequest request) {
        log.info("========================================");
        log.info("🥼 DOCTOR REGISTRATION START");
        log.info("========================================");
        log.info("📧 System Email (login): {}", request.getEmail());
        log.info("📨 Contact Email (notifications): {}", request.getContactEmail());
        log.info("========================================");
        
        // Validation initiale
        if (request.getContactEmail() == null || request.getContactEmail().trim().isEmpty()) {
            log.error("❌ CRITICAL: contactEmail is NULL or EMPTY in request!");
            throw new RuntimeException("Contact email is required for doctor registration");
        }
        
        // Check if doctor already exists
        if (doctorRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Doctor already exists with email: " + request.getEmail());
        }
        
        if (doctorRepository.existsByMedicalLicenseNumber(request.getMedicalLicenseNumber())) {
            throw new RuntimeException("Medical license number already registered");
        }
        
        try {
            // ✅ STEP 1: Create doctor profile
            log.info("📝 STEP 1: Creating doctor profile");
            Doctor doctor = createDoctorProfile(request);
            
            log.info("🔍 Doctor object BEFORE save:");
            log.info("   - email: {}", doctor.getEmail());
            log.info("   - contactEmail: {}", doctor.getContactEmail());
            log.info("   - contactEmail is null? {}", doctor.getContactEmail() == null);
            log.info("   - contactEmail is empty? {}", doctor.getContactEmail() != null && doctor.getContactEmail().isEmpty());
            
            // ✅ STEP 2: Save to MongoDB
            log.info("💾 STEP 2: Saving to MongoDB");
            Doctor savedDoctor = doctorRepository.save(doctor);
            
            log.info("✅ Doctor saved to MongoDB:");
            log.info("   - ID: {}", savedDoctor.getId());
            log.info("   - email: {}", savedDoctor.getEmail());
            log.info("   - contactEmail: {}", savedDoctor.getContactEmail());
            log.info("   - getNotificationEmail(): {}", savedDoctor.getNotificationEmail());
            log.info("   - createdAt: {}", savedDoctor.getCreatedAt());
            
            // ✅ VERIFICATION: Re-fetch from DB to confirm
            log.info("🔍 VERIFICATION: Re-fetching from database");
            Doctor verifyDoctor = doctorRepository.findByEmail(savedDoctor.getEmail())
                    .orElseThrow(() -> new RuntimeException("Doctor not found after save!"));
            
            log.info("✅ Doctor re-fetched from DB:");
            log.info("   - contactEmail in DB: {}", verifyDoctor.getContactEmail());
            log.info("   - Matches saved object? {}", 
                    savedDoctor.getContactEmail().equals(verifyDoctor.getContactEmail()));
            
            // ✅ STEP 3: Create activation request
            log.info("📋 STEP 3: Creating activation request");
            createActivationRequest(savedDoctor);
            
            // ✅ STEP 4: Send email to DOCTOR
            log.info("========================================");
            log.info("📧 STEP 4: Sending email to DOCTOR");
            log.info("========================================");
            log.info("🎯 Target email: {}", savedDoctor.getNotificationEmail());
            log.info("📝 Template: DOCTOR_REGISTRATION_PENDING");
            log.info("========================================");
            
            sendPendingValidationEmailToDoctor(savedDoctor);
            
            // ✅ STEP 5: Send email to ADMIN
            log.info("========================================");
            log.info("📧 STEP 5: Sending email to ADMIN");
            log.info("========================================");
            log.info("🎯 Admin email: {}", adminEmail);
            log.info("📝 Template: DOCTOR_REGISTRATION_ADMIN_NOTIFICATION");
            log.info("========================================");
            
            notifyAdmins(savedDoctor);
            
            log.info("========================================");
            log.info("✅ DOCTOR REGISTRATION COMPLETED");
            log.info("========================================");
            
            return mapToDoctorResponse(savedDoctor);
            
        } catch (Exception e) {
            log.error("❌ Failed to register doctor: {}", request.getEmail(), e);
            throw new RuntimeException("Failed to register doctor: " + e.getMessage(), e);
        }
    }
    /**
     * Create doctor profile with contactEmail
     */
    private Doctor createDoctorProfile(DoctorRegisterRequest request) {
        String userId = UUID.randomUUID().toString();
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        
        return Doctor.builder()
                .userId(userId)
                .email(request.getEmail())
                .contactEmail(request.getContactEmail()) // ✅ NOUVEAU
                .password(hashedPassword)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .medicalLicenseNumber(request.getMedicalLicenseNumber())
                .specialization(request.getSpecialization())
                .hospitalAffiliation(request.getHospitalAffiliation())
                .yearsOfExperience(request.getYearsOfExperience())
                .officeAddress(request.getOfficeAddress())
                .consultationHours(request.getConsultationHours())
                .isActivated(false)
                .activationStatus("PENDING")
                .activationRequestDate(LocalDateTime.now())
                .totalPatients(0)
                .totalConsultations(0)
                .averageRating(0.0)
                .build();
    }
    
    /**
     * Create activation request
     */
    private void createActivationRequest(Doctor doctor) {
        DoctorActivationRequest activationRequest = DoctorActivationRequest.builder()
                .doctorId(doctor.getId())
                .doctorEmail(doctor.getEmail())
                .doctorFullName(doctor.getFullName())
                .medicalLicenseNumber(doctor.getMedicalLicenseNumber())
                .specialization(doctor.getSpecialization())
                .hospitalAffiliation(doctor.getHospitalAffiliation())
                .yearsOfExperience(doctor.getYearsOfExperience())
                .isPending(true)
                .requestedAt(LocalDateTime.now())
                .build();
        
        activationRequestRepository.save(activationRequest);
    }
    
    /**
     * ✅ Send pending validation email to doctor's CONTACT EMAIL
     */
    private void sendPendingValidationEmailToDoctor(Doctor doctor) {
        try {
            String emailTo = doctor.getNotificationEmail();
            log.info("📧 Sending pending validation email to: {}", emailTo);
            
            EmailNotificationRequest emailRequest = EmailNotificationRequest.builder()
                    .to(emailTo) // ✅ Contact email, pas system email
                    .subject("Registration Received - Pending Validation")
                    .templateType("DOCTOR_REGISTRATION_PENDING")
                    .templateVariables(Map.of(
                        "doctorFirstName", doctor.getFirstName(),
                        "doctorLastName", doctor.getLastName(),
                        "registrationDate", doctor.getCreatedAt().toString()
                    ))
                    .build();
            
            notificationClient.sendEmail(emailRequest);
            log.info("✅ Pending validation email sent to: {}", emailTo);
            
        } catch (Exception e) {
            log.error("❌ Failed to send pending validation email", e);
        }
    }
    
    /**
     * Notify admins
     */
    private void notifyAdmins(Doctor doctor) {
        try {
            log.info("📧 Sending notification to admin: {}", adminEmail);
            
            EmailNotificationRequest emailRequest = EmailNotificationRequest.builder()
                    .to(adminEmail)
                    .subject("New Doctor Registration - Approval Required")
                    .templateType("DOCTOR_REGISTRATION_ADMIN_NOTIFICATION")
                    .templateVariables(Map.of(
                        "adminName", "Admin",
                        "doctorName", doctor.getFullName(),
                        "doctorEmail", doctor.getEmail(),
                        "doctorContactEmail", doctor.getContactEmail(), // ✅ Inclus dans notification admin
                        "medicalLicense", doctor.getMedicalLicenseNumber(),
                        "specialization", doctor.getSpecialization(),
                        "hospital", doctor.getHospitalAffiliation(),
                        "experience", doctor.getYearsOfExperience(),
                        "registrationDate", doctor.getCreatedAt().toString()
                    ))
                    .build();
            
            notificationClient.sendEmail(emailRequest);
            log.info("✅ Admin notification sent to: {}", adminEmail);
            
        } catch (Exception e) {
            log.error("❌ Failed to send admin notification", e);
        }
    }
    
    /**
     * Map Doctor to DoctorResponse
     */
    private DoctorResponse mapToDoctorResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .userId(doctor.getUserId())
                .email(doctor.getEmail())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .fullName(doctor.getFullName())
                .phoneNumber(doctor.getPhoneNumber())
                .medicalLicenseNumber(doctor.getMedicalLicenseNumber())
                .specialization(doctor.getSpecialization())
                .hospitalAffiliation(doctor.getHospitalAffiliation())
                .yearsOfExperience(doctor.getYearsOfExperience())
                .officeAddress(doctor.getOfficeAddress())
                .consultationHours(doctor.getConsultationHours())
                .isActivated(doctor.getIsActivated())
                .activationStatus(doctor.getActivationStatus())
                .activationDate(doctor.getActivationDate())
                .activationRequestDate(doctor.getActivationRequestDate())
                .totalPatients(doctor.getTotalPatients())
                .averageRating(doctor.getAverageRating())
                .totalConsultations(doctor.getTotalConsultations())
                .createdAt(doctor.getCreatedAt())
                .build();
    }
}