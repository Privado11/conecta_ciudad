package com.unimagdalena.conectaCiudad.services.user;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserMapper;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.DuplicateResourceException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;



@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDto findByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (Objects.isNull(user)) {
            throw new ResourceNotFoundException("User", "email", email);
        }
        return userMapper.toDto(user);
    }

    @Override
    public UserDto findByNationalId(String nationalId) {
        User user = userRepository.findByNationalId(nationalId);
        if (Objects.isNull(user)) {
            throw new ResourceNotFoundException("User", "nationalId", nationalId);
        }
        return userMapper.toDto(user);
    }

    @Override
    public List<UserDto> findByNameContainingIgnoreCase(String name) {
        List<User> users = userRepository.findByNameContainingIgnoreCase(name);
        return users.stream()
            .map(userMapper::toDto)
            .toList();
    }

    @Override
    public UserDto saveUser(UserSaveDto user) {
        // Validaciones de campos requeridos
        if (Objects.isNull(user)) {
            throw new BadRequestException("User data cannot be null");
        }
        if (user.email() == null || user.email().trim().isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        if (user.nationalId() == null || user.nationalId().trim().isEmpty()) {
            throw new BadRequestException("National ID is required");
        }
        if (user.name() == null || user.name().trim().isEmpty()) {
            throw new BadRequestException("Name is required");
        }
        if (user.password() == null || user.password().trim().isEmpty()) {
            throw new BadRequestException("Password is required");
        }
        

        Optional<User> existingUser = userRepository.findByEmailOrNationalId(user.email(), user.nationalId());
        existingUser.ifPresent(u -> {
            if (u.getEmail().equals(user.email())) {
                throw new DuplicateResourceException("User", "email", user.email());
            }
            if (u.getNationalId().equals(user.nationalId())) {
                throw new DuplicateResourceException("User", "nationalId", user.nationalId());
            }
        });

        User userToSave = userMapper.toUserSaveDtoToEntity(user);
        userToSave.setPassword(passwordEncoder.encode(userToSave.getPassword()));
        return userMapper.toDto(userRepository.save(userToSave));
    }

    @Override
public UserDto findById(Long id) {
    User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    
    return userMapper.toDto(user);
}

    @Override
    public UserDto findByEmailOrNationalId(String email, String nationalId) {
        return userRepository.findByEmailOrNationalId(email, nationalId)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email or nationalId", email + " or " + nationalId));
    }

    @Override
    public List<UserDto> findAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    public UserDto updateUser(Long id, UserSaveDto user) {
        return userRepository.findById(id).map(existingUser -> {
            existingUser.setName(user.name());
            existingUser.setEmail(user.email());
            existingUser.setNationalId(user.nationalId());
            existingUser.setPhone(user.phone());
            return userRepository.save(existingUser);
        }).map(userMapper::toDto)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        userRepository.delete(user);
    }
}


