package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.user.UserRequest;
import com.adem.attijari_compass.dto.user.UserResponse;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.RefreshTokenRepository;
import com.adem.attijari_compass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private static final long MAX_PROFILE_PHOTO_SIZE_BYTES = 2 * 1024 * 1024;
    private static final Path PROFILE_UPLOAD_DIR = Paths.get("uploads", "profile");
    private static final Set<String> ALLOWED_PROFILE_PHOTO_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    public List<UserResponse> getAllUsers() {
        return userRepository.findAllByDeletedFalse().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToResponse(user);
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapToResponse(user);
    }

    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Compte supprime.");
        }
        user.setEmail(request.getEmail());
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userRepository.save(user);
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse uploadCurrentUserPhoto(String email, MultipartFile file) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Compte supprime.");
        }
        validateProfilePhoto(file);

        try {
            Files.createDirectories(PROFILE_UPLOAD_DIR);
            deleteStoredProfilePhoto(user.getProfilePictureUrl());

            String extension = resolveExtension(file);
            String fileName = "user-" + user.getId() + "-" + UUID.randomUUID() + extension;
            Path target = PROFILE_UPLOAD_DIR.resolve(fileName).normalize();

            if (!target.startsWith(PROFILE_UPLOAD_DIR)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom de fichier invalide.");
            }

            file.transferTo(target);
            user.setProfilePictureUrl("/uploads/profile/" + fileName);
            userRepository.save(user);
            return mapToResponse(user);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible d enregistrer la photo de profil.");
        }
    }

    @Transactional
    public UserResponse deleteCurrentUserPhoto(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Compte supprime.");
        }

        deleteStoredProfilePhoto(user.getProfilePictureUrl());
        user.setProfilePictureUrl(null);
        userRepository.save(user);
        return mapToResponse(user);
    }

    @Transactional
    public void deleteCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Compte deja supprime.");
        }
        deleteStoredProfilePhoto(user.getProfilePictureUrl());
        user.setProfilePictureUrl(null);
        user.setActive(false);
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(user.getId());
        user.setDeletionReason("Suppression demandee depuis le profil utilisateur");
        refreshTokenRepository.deleteByUserId(user.getId());
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La suppression physique est desactivee. Utilisez la suppression logique du Back Office.");
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }

    private void validateProfilePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo de profil manquante.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_PROFILE_PHOTO_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seuls les fichiers image sont acceptes.");
        }

        if (file.getSize() > MAX_PROFILE_PHOTO_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La photo ne doit pas depasser 2 Mo.");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return ".jpg";
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    private void deleteStoredProfilePhoto(String profilePictureUrl) {
        if (!StringUtils.hasText(profilePictureUrl) || !profilePictureUrl.startsWith("/uploads/profile/")) {
            return;
        }

        String fileName = Path.of(profilePictureUrl).getFileName().toString();
        Path target = PROFILE_UPLOAD_DIR.resolve(fileName).normalize();

        if (!target.startsWith(PROFILE_UPLOAD_DIR)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // La suppression du fichier ne doit pas bloquer la mise a jour du profil.
        }
    }
}

