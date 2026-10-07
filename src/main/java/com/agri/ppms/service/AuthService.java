package com.agri.ppms.service;

import com.agri.ppms.config.JwtUtils;
import com.agri.ppms.dto.AuthResponse;
import com.agri.ppms.dto.LoginRequest;
import com.agri.ppms.dto.RegisterRequest;
import com.agri.ppms.entity.Farmer;
import com.agri.ppms.entity.ProcurementCentre;
import com.agri.ppms.entity.StaffAssignment;
import com.agri.ppms.entity.User;
import com.agri.ppms.repository.FarmerRepository;
import com.agri.ppms.repository.ProcurementCentreRepository;
import com.agri.ppms.repository.StaffAssignmentRepository;
import com.agri.ppms.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final FarmerRepository farmerRepository;
    private final ProcurementCentreRepository centreRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final NotificationService notificationService;

    public AuthService(UserRepository userRepository,
                       FarmerRepository farmerRepository,
                       ProcurementCentreRepository centreRepository,
                       StaffAssignmentRepository staffAssignmentRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       AuthenticationManager authenticationManager,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.farmerRepository = farmerRepository;
        this.centreRepository = centreRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.notificationService = notificationService;
    }

    @Transactional
    public AuthResponse registerFarmer(RegisterRequest request) {
        if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new IllegalArgumentException("Mobile number is already registered: " + request.getMobileNumber());
        }

        // Mask Aadhaar: only last 4 digits visible
        String cleanAadhaar = request.getAadhaarNumber().replaceAll("\\s+", "");
        String last4 = cleanAadhaar.length() >= 4 ? cleanAadhaar.substring(cleanAadhaar.length() - 4) : "0000";
        String maskedAadhaar = "XXXX XXXX " + last4;

        // Create User
        User user = new User(
                request.getMobileNumber(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                User.Role.FARMER
        );
        user = userRepository.save(user);

        // Generate unique Farmer ID, e.g. TN-FARM-XXXXX
        String farmerId = "TN-FARM-" + (10000 + new Random().nextInt(90000));

        ProcurementCentre preferredCentre = null;
        if (request.getPreferredCentreId() != null) {
            preferredCentre = centreRepository.findById(request.getPreferredCentreId()).orElse(null);
        }

        Farmer farmer = new Farmer(
                user,
                farmerId,
                maskedAadhaar,
                request.getLandAcres(),
                request.getAddress(),
                request.getVillage(),
                request.getDistrict(),
                request.getState(),
                preferredCentre
        );
        farmer = farmerRepository.save(farmer);

        // Welcome notification
        notificationService.createNotification(
                user,
                "Welcome to PPMS!",
                "Your registration is successful. Provisional paddy quota of " + farmer.getApprovedQuantity() + " bags (20 bags/acre) has been allocated.",
                "REGISTER",
                "#dashboard"
        );

        String token = jwtUtils.generateToken(user.getMobileNumber(), user.getRole().name(), user.getId());

        return new AuthResponse(
                token,
                user.getId(),
                user.getMobileNumber(),
                user.getFullName(),
                user.getRole(),
                farmer.getFarmerId(),
                preferredCentre != null ? preferredCentre.getId() : null,
                preferredCentre != null ? preferredCentre.getName() : null
        );
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getMobileNumber(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("Invalid mobile number or password. Please try again.");
        }

        User user = userRepository.findByMobileNumber(request.getMobileNumber())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isActive()) {
            throw new IllegalStateException("Your account has been deactivated. Please contact the administrator.");
        }

        String token = jwtUtils.generateToken(user.getMobileNumber(), user.getRole().name(), user.getId());

        String farmerId = null;
        Long centreId = null;
        String centreName = null;

        if (user.getRole() == User.Role.FARMER) {
            Farmer farmer = farmerRepository.findByUser(user).orElse(null);
            if (farmer != null) {
                farmerId = farmer.getFarmerId();
                if (farmer.getPreferredCentre() != null) {
                    centreId = farmer.getPreferredCentre().getId();
                    centreName = farmer.getPreferredCentre().getName();
                }
            }
        } else if (user.getRole() == User.Role.STAFF) {
            StaffAssignment assignment = staffAssignmentRepository.findByUser(user).orElse(null);
            if (assignment != null && assignment.getCentre() != null) {
                centreId = assignment.getCentre().getId();
                centreName = assignment.getCentre().getName();
            }
        }

        return new AuthResponse(
                token,
                user.getId(),
                user.getMobileNumber(),
                user.getFullName(),
                user.getRole(),
                farmerId,
                centreId,
                centreName
        );
    }
}
