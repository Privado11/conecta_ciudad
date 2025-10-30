package com.unimagdalena.conectaCiudad.services.user;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserMapper;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.entities.Role;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.DuplicateResourceException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.repositories.RoleRepository;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;



@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final ProjectRepository projectRepository;
    private static final Set<String> ALLOWED_ROLE_NAMES = Set.of("ADMIN", "CIUDADANO", "CURATOR", "LIDER_COMUNITARIO");

    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder, RoleRepository roleRepository, ProjectRepository projectRepository) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.projectRepository = projectRepository;
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
        
        Role defaultRole = roleRepository.findByNameContainingIgnoreCase("CIUDADANO");
        if (defaultRole == null) {
            throw new BadRequestException("Default role CIUDADANO not found");
        }
        List<Role> roles = new ArrayList<>();
        roles.add(defaultRole);
        userToSave.setRoles(roles);
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
        long projects = projectRepository.countByCreatorId(id);
        if (projects > 0) {
            throw new BadRequestException("No se puede eliminar el usuario: tiene " + projects + " proyecto(s) asociados");
        }
        userRepository.delete(user);
    }

    @Override
    public UserDto addRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        String normalized = roleName == null ? "" : roleName.trim().toUpperCase();
        if (!ALLOWED_ROLE_NAMES.contains(normalized)) {
            throw new BadRequestException("Role not allowed: " + roleName);
        }
        Role role = roleRepository.findByNameContainingIgnoreCase(normalized);
        if (role == null) {
            throw new ResourceNotFoundException("Role", "name", roleName);
        }
        List<Role> roles = new ArrayList<>(user.getRoles());
        boolean exists = roles.stream().anyMatch(r -> r.getName().equalsIgnoreCase(role.getName()));
        if (!exists) {
            roles.add(role);
            user.setRoles(roles);
            user = userRepository.save(user);
        }
        return userMapper.toDto(user);
    }

    @Override
    public UserDto removeRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        String normalized = roleName == null ? "" : roleName.trim().toUpperCase();
        if (!ALLOWED_ROLE_NAMES.contains(normalized)) {
            throw new BadRequestException("Role not allowed: " + roleName);
        }
        List<Role> roles = new ArrayList<>(user.getRoles());
        roles.removeIf(r -> r.getName().equalsIgnoreCase(normalized));
        if (roles.isEmpty()) {
            Role defaultRole = roleRepository.findByNameContainingIgnoreCase("CIUDADANO");
            if (defaultRole == null) {
                throw new BadRequestException("Default role CIUDADANO not found");
            }
            roles.add(defaultRole);
        }
        user.setRoles(roles);
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Override
    public UserDto createUserWithRoles(UserSaveDto user, List<String> roleNames) {
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

        List<Role> rolesToAssign = new ArrayList<>();
        if (roleNames == null || roleNames.isEmpty()) {
            Role defaultRole = roleRepository.findByNameContainingIgnoreCase("CIUDADANO");
            if (defaultRole == null) {
                throw new BadRequestException("Default role CIUDADANO not found");
            }
            rolesToAssign.add(defaultRole);
        } else {
            for (String roleName : roleNames) {
                String normalized = roleName == null ? "" : roleName.trim().toUpperCase();
                if (!ALLOWED_ROLE_NAMES.contains(normalized)) {
                    throw new BadRequestException("Role not allowed: " + roleName);
                }
                Role role = roleRepository.findByNameContainingIgnoreCase(normalized);
                if (role == null) {
                    throw new ResourceNotFoundException("Role", "name", roleName);
                }
                boolean exists = rolesToAssign.stream().anyMatch(r -> r.getName().equalsIgnoreCase(role.getName()));
                if (!exists) {
                    rolesToAssign.add(role);
                }
            }
        }

        userToSave.setRoles(rolesToAssign);
        return userMapper.toDto(userRepository.save(userToSave));
    }
}


