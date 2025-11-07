package com.unimagdalena.conectaCiudad.services.user;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;
import com.unimagdalena.conectaCiudad.Dto.action.ActionSaveDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserMapper;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.UserActionType;
import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.entities.Role;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.DuplicateResourceException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.services.access.AccessService;
import com.unimagdalena.conectaCiudad.services.action.ActionService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

import com.unimagdalena.conectaCiudad.repositories.RoleRepository;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;



@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final ProjectRepository projectRepository;
    private static final Set<String> ALLOWED_ROLE_NAMES = Set.of("ADMIN", "CIUDADANO", "CURATOR", "LIDER_COMUNITARIO");
    private final AccessService accessService; 
    private final ActionService actionService;
    private final HttpServletRequest request; 


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
            .map(user -> {
                UserDto dto = userMapper.toDto(user);
                LocalDateTime lastAction = actionService.getLastActionDateByUserId(user.getId());
                return new UserDto(
                    dto.id(),
                    dto.name(),
                    dto.nationalId(),
                    dto.email(),
                    dto.phone(),
                    dto.createdAt(),
                    dto.roles(),
                    dto.active(),
                    lastAction
                );
            })
            .toList();
}

    @Override
    public UserDto updateUser(Long id, UserSaveDto user) {
        UserDto userDto = userRepository.findById(id).map(existingUser -> {
            existingUser.setName(user.name());
            existingUser.setEmail(user.email());
            existingUser.setNationalId(user.nationalId());
            existingUser.setPhone(user.phone());
            return userRepository.save(existingUser);
        }).map(userMapper::toDto)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        logAction(UserActionType.USER_UPDATED, "Usuario actualizado " + userDto.id(), userDto.id());
        return userDto;
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        long projects = projectRepository.countByCreatorId(id);
        if (projects > 0) {
            throw new BadRequestException("No se puede eliminar el usuario: tiene " + projects + " proyecto(s) asociados");
        }
        UserDto userDto = userMapper.toDto(user);
        logAction(UserActionType.USER_DELETED, "Usuario eliminado " + userDto.id(), id);
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
        
        String oldRole = user.getRoles().isEmpty() ? "ninguno" : user.getRoles().iterator().next().getName();
        
        user.setRoles(Collections.singletonList(role));
        user = userRepository.save(user);
        
        UserDto userDto = userMapper.toDto(user);
        logAction(UserActionType.USER_ROLE_ADDED, 
                 String.format("Rol del usuario %d cambiado de %s a %s", 
                             userDto.id(), oldRole, role.getName()), 
                 userId);
        return userDto;
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
        UserDto userDto = userMapper.toDto(userRepository.save(user));
        logAction(UserActionType.USER_ROLE_REMOVED, "Rol " + roleName + " removido del usuario " + userDto.id(), userId);
        return userDto;
    }

    @Override
    public UserDto saveUser(UserSaveDto user) {    

        if (user.roles() == null || user.roles().isEmpty()) {
            throw new BadRequestException("Se debe proporcionar al menos un rol para el usuario");
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
        
        
        List<Role> roles = user.roles().stream()
        .map(roleName -> roleRepository.findByNameContainingIgnoreCase(roleName))
        .filter(Objects::nonNull)
        .toList();

        if (roles.isEmpty()) {
            throw new BadRequestException("Los roles proporcionados no existen en la base de datos");
        }

        userToSave.setActive(true);
       
        userToSave.setRoles(roles);
        UserDto userDto = userMapper.toDto(userRepository.save(userToSave));
       
        logAction(UserActionType.USER_CREATED, "Usuario " + userDto.id() + " creado con rol: " + roles.stream().map(Role::getName).collect(Collectors.joining(", ")), userDto.id());
        return userDto;
    }

    @Override
public UserDto saveUserDefault(UserSaveDto user) {    

    if (user.roles() != null && !user.roles().isEmpty()) {
        boolean hasNonCitizen = user.roles().stream()
            .anyMatch(r -> !r.equalsIgnoreCase("CIUDADANO"));
        if (hasNonCitizen) {
            throw new BadRequestException("Solo se permite el rol 'CIUDADANO'");
        }
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

    userToSave.setActive(true);

    Role defaultRole = roleRepository.findByNameContainingIgnoreCase("CIUDADANO");
    if (defaultRole == null) {
        throw new BadRequestException("El rol por defecto 'CIUDADANO' no existe en la base de datos");
    }

    userToSave.setRoles(List.of(defaultRole));

 
    UserDto userDto = userMapper.toDto(userRepository.save(userToSave));

   
    logAction(
        UserActionType.USER_CREATED,
        "Usuario " + userDto.id() + " creado con rol: " + defaultRole.getName(),
        userDto.id()
    );

    return userDto;
}




@Override
public UserDto toggleUserStatus(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + userId));
    
    boolean newStatus = !user.getActive();
    user.setActive(newStatus);

    System.out.println("El estado del usuario ha sido cambiado a " + (newStatus ? "activo" : "inactivo"));
    
    logActionForAdmin(
        newStatus ? UserActionType.USER_ACTIVATED : UserActionType.USER_DEACTIVATED,
        "El estado del usuario " + user.getEmail() + " ha sido cambiado a " + (newStatus ? "activo" : "inactivo")
    );
    
    return userMapper.toDto(userRepository.save(user));
}

@Override
public Page<UserDto> findAllExceptCurrent(Long currentUserId, int page, int size, String sortBy, String sortDirection) {
    
    if (page < 0) page = 0;
    if (size <= 0) size = 10;
    if (size > 100) size = 100; 
    
    
    Sort.Direction direction = sortDirection != null && sortDirection.equalsIgnoreCase("desc") 
        ? Sort.Direction.DESC 
        : Sort.Direction.ASC;
    
    String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "name";
    Sort sort = Sort.by(direction, sortField);
    
    
    Pageable pageable = PageRequest.of(page, size, sort);
    
    
    Page<User> userPage;
    if (currentUserId != null) {
        userPage = userRepository.findAllExceptUser(currentUserId, pageable);
    } else {
        userPage = userRepository.findAll(pageable);
    }
    
    
    return userPage.map(user -> {
        UserDto dto = userMapper.toDto(user);
        LocalDateTime lastAction = actionService.getLastActionDateByUserId(user.getId());
        return new UserDto(
            dto.id(),
            dto.name(),
            dto.nationalId(),
            dto.email(),
            dto.phone(),
            dto.createdAt(),
            dto.roles(),
            dto.active(),
            lastAction
        );
    });
}

@Override
public Page<UserDto> findByNameWithPagination(String name, Long currentUserId, int page, int size, String sortBy, String sortDirection) {
    if (page < 0) page = 0;
    if (size <= 0) size = 10;
    if (size > 100) size = 100;
    
   
    Sort.Direction direction = sortDirection != null && sortDirection.equalsIgnoreCase("desc") 
        ? Sort.Direction.DESC 
        : Sort.Direction.ASC;
    
    String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "name";
    Sort sort = Sort.by(direction, sortField);
    
 
    Pageable pageable = PageRequest.of(page, size, sort);
    
    Page<User> userPage;
    if (currentUserId != null) {
        userPage = userRepository.findByNameContainingIgnoreCaseAndIdNot(name, currentUserId, pageable);
    } else {
        userPage = userRepository.findByNameContainingIgnoreCase(name, pageable);
    }
    

    return userPage.map(user -> {
        UserDto dto = userMapper.toDto(user);
        LocalDateTime lastAction = actionService.getLastActionDateByUserId(user.getId());
        return new UserDto(
            dto.id(),
            dto.name(),
            dto.nationalId(),
            dto.email(),
            dto.phone(),
            dto.createdAt(),
            dto.roles(),
            dto.active(),
            lastAction
        );
    });
}


    private ActionDto logAction(UserActionType actionType, String description, Long userId) {
        if (userId == null) return null;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;
        Long accessId = (Long) request.getAttribute("currentAccessId");
        if (accessId == null) return null;
        Access access = accessService.findById(accessId);
        if (access == null) return null;

        return actionService.save(new ActionSaveDto(actionType.name(), description, user, access));
    }

    private ActionDto logActionForAdmin(UserActionType actionType, String description) {
        Long accessId = (Long) request.getAttribute("currentAccessId");
        if (accessId == null) return null;
        
        Access access = accessService.findById(accessId);
        if (access == null) return null;
        
        User admin = access.getUser();
        if (admin == null) return null;
    
        return actionService.save(new ActionSaveDto(actionType.name(), description, admin, access));
    }
}


