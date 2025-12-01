package com.unimagdalena.conectaCiudad.services.user;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserMapper;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.entities.Role;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.enums.ErrorCode;
import com.unimagdalena.conectaCiudad.enums.UserActionType;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.DuplicateResourceException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.RoleRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.events.UserCreatedEvent;
import com.unimagdalena.conectaCiudad.events.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE = "CITIZEN";
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final ApplicationEventPublisher eventPublisher;


    @Override
    public UserDto findByEmail(String email) {
        log.debug("Buscando usuario por email: {}", email);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User", "email", email);
        }

        return userMapper.toDto(user);
    }

    @Override
    public UserDto findByNationalId(String nationalId) {
        log.debug("Buscando usuario por cédula: {}", nationalId);

        User user = userRepository.findByNationalId(nationalId);
        if (user == null) {
            throw new ResourceNotFoundException("User", "nationalId", nationalId);
        }

        return userMapper.toDto(user);
    }

    @Override
    public UserDto findById(Long id) {
        log.debug("Buscando usuario por ID: {}", id);

        User user = findUserByIdOrThrow(id);
        return userMapper.toDto(user);
    }

    @Override
    public UserDto findByEmailOrNationalId(String email, String nationalId) {
        log.debug("Buscando usuario por email o cédula");

        return userRepository.findByEmailOrNationalId(email, nationalId)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User",
                        "email or nationalId",
                        String.format("%s or %s", email, nationalId)
                ));
    }


    @Override
    @Transactional
    public UserDto registerUser(UserSaveDto userDto) {
        log.info("Iniciando registro de nuevo usuario: {}", userDto.email());

        validateCitizenRoleOnly(userDto.roles());

        validateUniqueFields(userDto.email(), userDto.nationalId());

        User user = buildUserFromDto(userDto);
        user.setActive(true);

        Role citizenRole = findRoleByNameOrThrow(DEFAULT_ROLE);
        user.setRoles(List.of(citizenRole));

        User savedUser = userRepository.save(user);

        log.info("Usuario {} registrado exitosamente con ID: {}",
                savedUser.getEmail(), savedUser.getId());

        eventPublisher.publishEvent(new UserCreatedEvent(this, savedUser, null)); 

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateOwnProfile(Long userId, UserSaveDto updateDto) {
        log.info("Usuario {} actualizando su propio perfil", userId);

        User user = findUserByIdOrThrow(userId);

        updateUserBasicFields(user, updateDto);

        User savedUser = userRepository.save(user);

        log.info("Usuario {} actualizó su perfil exitosamente", user.getEmail());

        eventPublisher.publishEvent(new UserUpdatedEvent(this, savedUser, savedUser));

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("Usuario {} cambiando su contraseña", userId);

        User user = findUserByIdOrThrow(userId);

        validateCurrentPassword(user, oldPassword);

        validateNewPassword(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        User savedUser = userRepository.save(user);

        log.info("Usuario {} cambió su contraseña exitosamente", user.getEmail());

        eventPublisher.publishEvent(new UserUpdatedEvent(this, savedUser, savedUser));

        return userMapper.toDto(user);
    }


    @Override
    public void validateUniqueFields(String email, String nationalId) {
        if (email == null && nationalId == null) {
            return;
        }

        List<User> existingUsers = findUsersByEmailOrNationalId(email, nationalId);

        if (existingUsers.isEmpty()) {
            return;
        }

        checkForDuplicates(email, nationalId, existingUsers);
    }



    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }


    private Role findRoleByNameOrThrow(String roleName) {
        Role role = roleRepository.findByNameContainingIgnoreCase(roleName);
        if (role == null) {
            throw new ResourceNotFoundException("Role", "name", roleName);
        }
        return role;
    }

    private List<User> findUsersByEmailOrNationalId(String email, String nationalId) {
        return userRepository.findByEmailInOrNationalIdIn(
                email != null ? List.of(email) : Collections.emptyList(),
                nationalId != null ? List.of(nationalId) : Collections.emptyList()
        );
    }


    private void validateCitizenRoleOnly(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }

        boolean hasNonCitizen = roles.stream()
                .anyMatch(role -> !DEFAULT_ROLE.equalsIgnoreCase(role));

        if (hasNonCitizen) {
            throw new BadRequestException(ErrorCode.ROLE_CITIZEN_ONLY);
        }
    }


    private void checkForDuplicates(String email, String nationalId, List<User> existingUsers) {
        boolean emailExists = email != null && existingUsers.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));

        boolean nationalIdExists = nationalId != null && existingUsers.stream()
                .anyMatch(u -> nationalId.equalsIgnoreCase(u.getNationalId()));

        if (emailExists && nationalIdExists) {
            throw new BadRequestException(
                    ErrorCode.EMAIL_AND_NATIONAL_ID_EXIST,
                    Map.of(
                            "email", email,
                            "nationalId", nationalId
                    )
            );
        }

        if (emailExists) {
            throw new DuplicateResourceException("User", "email", email);
        }

        if (nationalIdExists) {
            throw new DuplicateResourceException("User", "nationalId", nationalId);
        }
    }


    private void validateCurrentPassword(User user, String oldPassword) {
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new BadRequestException(ErrorCode.PASSWORD_REQUIRED);
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadRequestException(ErrorCode.PASSWORD_INCORRECT);
        }
    }


    private void validateNewPassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new BadRequestException(ErrorCode.PASSWORD_EMPTY);
        }

        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BadRequestException(
                    ErrorCode.PASSWORD_TOO_SHORT,
                    Map.of("minLength", MIN_PASSWORD_LENGTH)
            );
        }

        if (newPassword.equals(newPassword.toLowerCase())) {
            log.warn("Nueva contraseña no tiene mayúsculas (recomendación de seguridad)");
        }
    }

    private User buildUserFromDto(UserSaveDto dto) {
        User user = userMapper.toUserSaveDtoToEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.password()));
        return user;
    }


    private void updateUserBasicFields(User user, UserSaveDto updateDto) {

        if (updateDto.name() != null && !updateDto.name().isBlank()) {
            user.setName(updateDto.name().trim());
        }

        if (updateDto.phone() != null && !updateDto.phone().isBlank()) {
            user.setPhone(updateDto.phone().trim());
        }

    }
}