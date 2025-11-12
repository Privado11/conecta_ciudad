package com.unimagdalena.conectaCiudad.services.user;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.page.Statistics;
import com.unimagdalena.conectaCiudad.Dto.user.BulkUserImportResult;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserImportError;
import com.unimagdalena.conectaCiudad.Dto.user.UserMapper;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Role;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.enums.UserActionType;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.DuplicateResourceException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.RoleRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.services.action.ActionService;
import com.unimagdalena.conectaCiudad.services.action.AuditHelper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStreamReader;

@Slf4j
@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_PAGE_NUMBER = 0;
    private static final String DEFAULT_SORT_FIELD = "name";
    private static final String DEFAULT_ROLE = "CIUDADANO";
    private static final Set<String> ALLOWED_ROLE_NAMES = Set.of("ADMIN", "CIUDADANO", "CURATOR", "LIDER_COMUNITARIO");
    private static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final String PHONE_REGEX = "^(\\+?\\d{10,15})$";
    private static final int MIN_PASSWORD_LENGTH = 6;
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final ProjectRepository projectRepository;
    private final ActionService actionService;
    private final AuditHelper auditHelper;

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
                .map(this::enrichWithLastAction)
                .toList();
    }

    @Override
    public UserDto updateUser(Long id, UserSaveDto user) {
        try {
            User existingUser = findUserById(id);
            
            String oldName = existingUser.getName();
            String oldEmail = existingUser.getEmail();
            String oldRole = existingUser.getRoles().isEmpty() ? "ninguno" : existingUser.getRoles().get(0).getName();
            
            existingUser.setName(user.name());
            existingUser.setEmail(user.email());
            existingUser.setNationalId(user.nationalId());
            existingUser.setPhone(user.phone());

            if (user.active() != null) {
                existingUser.setActive(user.active());
            }

            String newRole = oldRole;
            if (user.roles() != null && !user.roles().isEmpty()) {
                String normalizedRole = validateAndNormalizeRole(user.roles().get(0));
                Role role = findRoleByName(normalizedRole);
                existingUser.getRoles().clear();
                existingUser.getRoles().add(role);
                newRole = role.getName();
            }
            
            User savedUser = userRepository.save(existingUser);
            UserDto userDto = userMapper.toDto(savedUser);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("oldName", oldName);
            metadata.put("newName", savedUser.getName());
            metadata.put("oldEmail", oldEmail);
            metadata.put("newEmail", savedUser.getEmail());
            metadata.put("oldRole", oldRole);
            metadata.put("newRole", newRole);
            
            auditHelper.logComplete(
                UserActionType.USER_UPDATED.name(),
                "Usuario '" + oldName + "' actualizado",
                EntityType.USER,
                id,
                ActionResult.SUCCESS,
                metadata
            );
            
            return userDto;
            
        } catch (Exception e) {
            auditHelper.logFailure(
                UserActionType.USER_UPDATED.name(),
                "Error al actualizar usuario " + id,
                e.getMessage()
            );
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        try {
            log.info("Iniciando eliminación del usuario con ID: {}", id);
            
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
            
            long activeProjects = projectRepository.countByCreatorIdAndStatus(
                id, 
                ProjectStatus.PUBLICADO
            );
            
            if (activeProjects > 0) {
                throw new BadRequestException(
                    "No se puede eliminar el usuario porque tiene " + activeProjects + 
                    " proyecto(s) activo(s). Finalice o reasigne los proyectos primero."
                );
            }
            
            List<Project> inactiveProjects = projectRepository
                .findByCreatorIdAndStatusNot(id, ProjectStatus.PUBLICADO);
            
            if (!inactiveProjects.isEmpty()) {
                User systemUser = userRepository.findById(1L)
                    .orElseThrow(() -> new IllegalStateException(
                        "Usuario sistema no encontrado. Cree un usuario con ID=1 para reasignaciones."
                    ));
                
                log.info("Reasignando {} proyectos inactivos al usuario sistema", 
                         inactiveProjects.size());
                
                inactiveProjects.forEach(project -> project.setCreator(systemUser));
                projectRepository.saveAll(inactiveProjects);
            }
            
            String userName = user.getName();
            String userEmail = user.getEmail();
            
            userRepository.delete(user);
            
            log.info("Usuario {} ({}) eliminado exitosamente", userName, userEmail);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userName", userName);
            metadata.put("userEmail", userEmail);
            metadata.put("inactiveProjectsReassigned", inactiveProjects.size());
            
            auditHelper.logComplete(
                UserActionType.USER_DELETED.name(),
                "Usuario '" + userName + "' (" + userEmail + ") eliminado",
                EntityType.USER,
                id,
                ActionResult.SUCCESS,
                metadata
            );
            
        } catch (Exception e) {
            auditHelper.logFailure(
                UserActionType.USER_DELETED.name(),
                "Error al eliminar usuario " + id,
                e.getMessage()
            );
            throw e;
        }
    }

    @Override
    public UserDto addRole(Long userId, String roleName) {
        try {
            User user = findUserById(userId);
            String normalizedRole = validateAndNormalizeRole(roleName);
            Role role = findRoleByName(normalizedRole);
            
            String oldRole = user.getRoles().isEmpty() ? "ninguno" : user.getRoles().iterator().next().getName();
            
            user.getRoles().clear();
            user.getRoles().add(role);
            
            user = userRepository.save(user);
            UserDto userDto = userMapper.toDto(user);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userName", user.getName());
            metadata.put("oldRole", oldRole);
            metadata.put("newRole", role.getName());
            metadata.put("userId", userId);
            metadata.put("userEmail", user.getEmail());
            
            
            auditHelper.logComplete(
                UserActionType.USER_ROLE_ADDED.name(),
                String.format("Rol del usuario '%s' cambiado de %s a %s", 
                            user.getName(), oldRole, role.getName()),
                EntityType.USER,
                userId,
                ActionResult.SUCCESS,
                metadata
            );
            
            return userDto;
            
        } catch (Exception e) {
            auditHelper.logFailure(
                UserActionType.USER_ROLE_ADDED.name(),
                "Error al agregar rol al usuario " + userId,
                e.getMessage()
            );
            throw e;
        }
    }

    @Override
    public UserDto removeRole(Long userId, String roleName) {
        try {
            User user = findUserById(userId);
            String normalizedRole = validateAndNormalizeRole(roleName);
            Role role = findRoleByName(normalizedRole);
            
            List<Role> roles = new ArrayList<>(user.getRoles());
            roles.removeIf(r -> r.getName().equalsIgnoreCase(normalizedRole));
            
            if (roles.isEmpty()) {
                Role defaultRole = findRoleByName(DEFAULT_ROLE);
                roles.add(defaultRole);
            }
            
            user.setRoles(roles);
            User savedUser = userRepository.save(user);
            UserDto userDto = userMapper.toDto(savedUser);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userName", user.getName());
            metadata.put("removedRole", normalizedRole);
            metadata.put("assignedDefaultRole", roles.size() == 1 && roles.get(0).getName().equals(DEFAULT_ROLE));
            metadata.put("userId", userId);
            metadata.put("userEmail", user.getEmail());


            auditHelper.logComplete(
                UserActionType.USER_ROLE_REMOVED.name(),
                "Rol '" + normalizedRole + "' removido del usuario '" + user.getName() + "'",
                EntityType.USER,
                userId,
                ActionResult.SUCCESS,
                metadata
            );
            
            return userDto;
            
        } catch (Exception e) {
            auditHelper.logFailure(
                UserActionType.USER_ROLE_REMOVED.name(),
                "Error al remover rol del usuario " + userId,
                e.getMessage()
            );
            throw e;
        }
    }

    @Override
    public UserDto saveUser(UserSaveDto user) {
        try {
            validateRolesProvided(user.roles());
            validateUniqueFieldsForNewUser(user.email(), user.nationalId());
            
            User userToSave = createUserFromDto(user);
            List<Role> roles = validateAndFetchRoles(user.roles());
            
            userToSave.setActive(true);
            userToSave.setRoles(roles);
            
            User savedUser = userRepository.save(userToSave);
            UserDto userDto = userMapper.toDto(savedUser);
            
            String roleNames = roles.stream().map(Role::getName).collect(Collectors.joining(", "));
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userName", savedUser.getName());
            metadata.put("userEmail", savedUser.getEmail());
            metadata.put("roles", roleNames);
            
            auditHelper.logComplete(
                UserActionType.USER_CREATED.name(),
                "Usuario '" + savedUser.getName() + "' creado con rol: " + roleNames,
                EntityType.USER,
                savedUser.getId(),
                ActionResult.SUCCESS,
                metadata
            );
            
            return userDto;
            
        } catch (Exception e) {
            auditHelper.logFailure(
                UserActionType.USER_CREATED.name(),
                "Error al crear usuario",
                e.getMessage()
            );
            throw e;
        }
    }

    @Override
    public UserDto saveUserDefault(UserSaveDto user) {
        try {
            validateOnlyCitizenRole(user.roles());
            validateUniqueFieldsForNewUser(user.email(), user.nationalId());
            
            User userToSave = createUserFromDto(user);
            userToSave.setActive(true);
            
            Role defaultRole = findRoleByName(DEFAULT_ROLE);
            userToSave.setRoles(List.of(defaultRole));
            
            User savedUser = userRepository.save(userToSave);
            UserDto userDto = userMapper.toDto(savedUser);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userName", savedUser.getName());
            metadata.put("userEmail", savedUser.getEmail());
            metadata.put("role", defaultRole.getName());
            
            auditHelper.logComplete(
                UserActionType.USER_CREATED.name(),
                "Usuario '" + savedUser.getName() + "' creado con rol por defecto: " + defaultRole.getName(),
                EntityType.USER,
                savedUser.getId(),
                ActionResult.SUCCESS,
                metadata
            );
            
            return userDto;
            
        } catch (Exception e) {
            auditHelper.logFailure(
                UserActionType.USER_CREATED.name(),
                "Error al crear usuario con rol por defecto",
                e.getMessage()
            );
            throw e;
        }
    }

    @Override
    public UserDto toggleUserStatus(Long userId) {
        try {
            User user = findUserById(userId);
            
            boolean oldStatus = user.getActive();
            boolean newStatus = !oldStatus;
            user.setActive(newStatus);
            
            User savedUser = userRepository.save(user);
            
            log.info("Estado de usuario {} cambiado a {}", user.getEmail(), newStatus ? "activo" : "inactivo");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userName", user.getName());
            metadata.put("userEmail", user.getEmail());
            metadata.put("oldStatus", oldStatus);
            metadata.put("newStatus", newStatus);
            
            auditHelper.logComplete(
                newStatus ? UserActionType.USER_ACTIVATED.name() : UserActionType.USER_DEACTIVATED.name(),
                "Usuario '" + user.getEmail() + "' " + (newStatus ? "activado" : "desactivado"),
                EntityType.USER,
                userId,
                ActionResult.SUCCESS,
                metadata
            );
            
            return userMapper.toDto(savedUser);
            
        } catch (Exception e) {
            auditHelper.logFailure(
                UserActionType.USER_ACTIVATED.name(),
                "Error al cambiar estado del usuario " + userId,
                e.getMessage()
            );
            throw e;
        }
    }

    @Override
    public PagedResponse<UserDto> findAllExceptCurrent(Long currentUserId, int page, int size, String sortBy, String sortDirection) {
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        
        Page<User> userPage = (currentUserId != null) 
            ? userRepository.findAllExceptUser(currentUserId, pageable)
            : userRepository.findAll(pageable);
        
        Page<UserDto> users = userPage.map(this::enrichWithLastAction);
    
        long total = currentUserId != null 
            ? userRepository.countExcludingUser(currentUserId) 
            : userRepository.count();
        
        long active = currentUserId != null
            ? userRepository.countByActiveExcludingUser(true, currentUserId)
            : userRepository.countByActive(true);
        
        long inactive = total - active;
        
        Statistics<UserDto> stats = new Statistics<>(
        total,
        Map.of(
            "active", active,
            "inactive", inactive
        )
    );
        
        return new PagedResponse<>(users, stats);
    }

    @Override
    public PagedResponse<UserDto> findByNameWithPagination(String name, Long currentUserId, int page, int size, String sortBy, String sortDirection) {
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
    
        Page<User> userPage = (currentUserId != null)
        ? userRepository.findByNameContainingIgnoreCaseAndIdNot(name, currentUserId, pageable)
        : userRepository.findByNameContainingIgnoreCase(name, pageable);
    
        Page<UserDto> users = userPage.map(this::enrichWithLastAction);

        long total = userRepository.countByNameAndCurrentUser(name, currentUserId);
        long active = userRepository.countByNameAndActiveAndCurrentUser(name, true, currentUserId);
        long inactive = userRepository.countByNameAndActiveAndCurrentUser(name, false, currentUserId);
    
        Statistics<UserDto> stats = new Statistics<>(
        total,
        Map.of(
            "active", active,
            "inactive", inactive
        )
    );
    
        return new PagedResponse<>(users, stats);
    }

    @Override
    public PagedResponse<UserDto> findByFilters(String roleName, Boolean active, Long currentUserId,
                                    int page, int size, String sortBy, String sortDirection) {
        String normalizedRole = normalizeRoleForFilter(roleName);
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
    
        Page<User> userPage = userRepository.findByRoleAndActiveStatus(
        normalizedRole, 
        active, 
        currentUserId, 
        pageable
        );
    
        Page<UserDto> users = userPage.map(this::enrichWithLastAction);

        long total = userRepository.countByRoleAndCurrentUser(normalizedRole, currentUserId);
        long totalActive = userRepository.countByRoleAndActiveAndCurrentUser(normalizedRole, true, currentUserId);
        long totalInactive = userRepository.countByRoleAndActiveAndCurrentUser(normalizedRole, false, currentUserId);
    
        Statistics<UserDto> stats = new Statistics<>(
        total,
        Map.of(
            "active", totalActive,
            "inactive", totalInactive
        )
    );
    
        return new PagedResponse<>(users, stats);
    }

    @Override
    public PagedResponse<UserDto> findByNameAndFilters(String name, String roleName, Boolean active, 
                                           Long currentUserId, int page, int size, 
                                           String sortBy, String sortDirection) {
        String normalizedRole = normalizeRoleForFilter(roleName);
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
    
        Page<User> userPage = userRepository.findByNameAndRoleAndActiveStatus(
        name,
        normalizedRole,
        active,
        currentUserId,
        pageable
        );
    
        Page<UserDto> users = userPage.map(this::enrichWithLastAction);

        long total = userRepository.countByNameAndRoleAndCurrentUser(name, normalizedRole, currentUserId);
        long totalActive = userRepository.countByNameAndRoleAndActiveAndCurrentUser(name, normalizedRole, true, currentUserId);
        long totalInactive = userRepository.countByNameAndRoleAndActiveAndCurrentUser(name, normalizedRole, false, currentUserId);
    
        Statistics<UserDto> stats = new Statistics<>(
        total,
        Map.of(
            "active", totalActive,
            "inactive", totalInactive
        )
    );
    
        return new PagedResponse<>(users, stats);
    }

    @Override
    public void validateUniqueFields(String email, String nationalId) {
        if (email == null && nationalId == null) {
            return;
        }
    
        List<User> existingUsers = userRepository.findByEmailInOrNationalIdIn(
            email != null ? List.of(email) : List.of(),
            nationalId != null ? List.of(nationalId) : List.of()
        );
        
        if (existingUsers.isEmpty()) {
            return;
        }
        
        boolean emailExists = email != null && existingUsers.stream()
            .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        
        boolean nationalIdExists = nationalId != null && existingUsers.stream()
            .anyMatch(u -> u.getNationalId().equalsIgnoreCase(nationalId));
        
        if (emailExists && nationalIdExists) {
            throw new BadRequestException(
                "El email '" + email + "' y la cédula '" + nationalId + "' ya están registrados"
            );
        } else if (emailExists) {
            throw new DuplicateResourceException("User", "email", email);
        } else if (nationalIdExists) {
            throw new DuplicateResourceException("User", "nationalId", nationalId);
        }
    }

    @Override
    @Transactional
    public BulkUserImportResult saveBulkUsers(List<UserSaveDto> users) {
        try {
            log.info("Iniciando importación masiva de {} usuarios", users.size());
            
            List<UserDto> importedUsers = new ArrayList<>();
            List<UserImportError> errors = new ArrayList<>();
            
            Map<String, Integer> emailMap = new HashMap<>();
            Map<String, Integer> nationalIdMap = new HashMap<>();
            Set<Integer> rowsWithErrors = new HashSet<>();
            
            // Validación de duplicados en CSV y campos requeridos
            for (int i = 0; i < users.size(); i++) {
                UserSaveDto user = users.get(i);
                int rowNumber = i + 2; 
                String role = (user.roles() != null && !user.roles().isEmpty()) 
                    ? user.roles().get(0) 
                    : DEFAULT_ROLE;
                
                if (emailMap.containsKey(user.email())) {
                    errors.add(new UserImportError(
                        rowNumber, user.email(), user.nationalId(), role,
                        "Email duplicado en el CSV (también en fila " + emailMap.get(user.email()) + ")"
                    ));
                    rowsWithErrors.add(rowNumber);
                    continue;
                }
            
                if (nationalIdMap.containsKey(user.nationalId())) {
                    errors.add(new UserImportError(
                        rowNumber, user.email(), user.nationalId(), role,
                        "Cédula duplicada en el CSV (también en fila " + nationalIdMap.get(user.nationalId()) + ")"
                    ));
                    rowsWithErrors.add(rowNumber);
                    continue;
                }
                
                if (user.name() == null || user.name().isBlank()) {
                    errors.add(new UserImportError(rowNumber, user.email(), user.nationalId(), role, 
                        "El nombre es obligatorio"));
                    rowsWithErrors.add(rowNumber);
                    continue;
                }
                
                if (user.email() == null || user.email().isBlank()) {
                    errors.add(new UserImportError(rowNumber, user.email(), user.nationalId(), role, 
                        "El email es obligatorio"));
                    rowsWithErrors.add(rowNumber);
                    continue;
                }
                
                if (!user.email().matches(EMAIL_REGEX)) {
                    errors.add(new UserImportError(rowNumber, user.email(), user.nationalId(), role, 
                        "Formato de email inválido. Debe tener formato usuario@dominio.com"));
                    rowsWithErrors.add(rowNumber);
                    continue;
                }
                
                if (user.nationalId() == null || user.nationalId().isBlank()) {
                    errors.add(new UserImportError(rowNumber, user.email(), user.nationalId(), role, 
                        "La cédula es obligatoria"));
                    rowsWithErrors.add(rowNumber);
                    continue;
                }
                
                if (user.phone() == null || user.phone().isBlank()) {
                    errors.add(new UserImportError(rowNumber, user.email(), user.nationalId(), role, 
                        "El teléfono es obligatorio"));
                    rowsWithErrors.add(rowNumber);
                    continue;
                }
                
                if (!user.phone().matches(PHONE_REGEX)) {
                    errors.add(new UserImportError(rowNumber, user.email(), user.nationalId(), role, 
                        "Formato de teléfono inválido. Debe ser solo números o formato internacional con + (ej: +573001234567 o 3001234567)"));
                    rowsWithErrors.add(rowNumber);
                    continue;
                }
                
                if (user.password() == null || user.password().isBlank()) {
                    errors.add(new UserImportError(rowNumber, user.email(), user.nationalId(), role, 
                        "La contraseña es obligatoria"));
                    rowsWithErrors.add(rowNumber);
                    continue;
                }
                
                if (user.password().length() < MIN_PASSWORD_LENGTH) {
                    errors.add(new UserImportError(rowNumber, user.email(), user.nationalId(), role, 
                        "La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres"));
                    rowsWithErrors.add(rowNumber);
                    continue;
                }

                emailMap.put(user.email(), rowNumber);
                nationalIdMap.put(user.nationalId(), rowNumber);
            }
            
            Set<String> emails = users.stream()
                .map(UserSaveDto::email)
                .filter(email -> email != null && !email.isBlank())
                .collect(Collectors.toSet());
            
            Set<String> nationalIds = users.stream()
                .map(UserSaveDto::nationalId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
            
            List<User> existingUsers = userRepository.findByEmailInOrNationalIdIn(
                new ArrayList<>(emails), 
                new ArrayList<>(nationalIds)
            );
            
            Map<String, User> existingEmailMap = existingUsers.stream()
                .collect(Collectors.toMap(User::getEmail, u -> u, (u1, u2) -> u1));
            
            Map<String, User> existingNationalIdMap = existingUsers.stream()
                .collect(Collectors.toMap(User::getNationalId, u -> u, (u1, u2) -> u1));
            
            Map<String, Role> roleMap = new HashMap<>();
            for (String roleName : ALLOWED_ROLE_NAMES) {
                Role role = roleRepository.findByNameContainingIgnoreCase(roleName);
                if (role != null) {
                    roleMap.put(roleName, role);
                }
            }
            
            Role defaultRole = roleMap.get(DEFAULT_ROLE);
            if (defaultRole == null) {
                throw new BadRequestException("El rol por defecto 'CIUDADANO' no existe en la base de datos");
            }
            
            List<User> usersToSave = new ArrayList<>();
            
            for (int i = 0; i < users.size(); i++) {
                UserSaveDto userDto = users.get(i);
                int rowNumber = i + 2;
                
                if (rowsWithErrors.contains(rowNumber)) {
                    continue;
                }
                
                try {
                    String roleName = (userDto.roles() != null && !userDto.roles().isEmpty()) 
                        ? userDto.roles().get(0).trim().toUpperCase() 
                        : DEFAULT_ROLE;
                    
                    if (existingEmailMap.containsKey(userDto.email())) {
                        errors.add(new UserImportError(
                            rowNumber,
                            userDto.email(),
                            userDto.nationalId(),
                            roleName,
                            "El email ya existe en la base de datos"
                        ));
                        continue;
                    }
                    
                    if (existingNationalIdMap.containsKey(userDto.nationalId())) {
                        errors.add(new UserImportError(
                            rowNumber,
                            userDto.email(),
                            userDto.nationalId(),
                            roleName,
                            "La cédula ya existe en la base de datos"
                        ));
                        continue;
                    }
                    
                    if (!ALLOWED_ROLE_NAMES.contains(roleName)) {
                        errors.add(new UserImportError(
                            rowNumber,
                            userDto.email(),
                            userDto.nationalId(),
                            roleName,
                            "Rol no válido. Roles permitidos: " + String.join(", ", ALLOWED_ROLE_NAMES)
                        ));
                        continue;
                    }
                    
                    Role role = roleMap.get(roleName);
                    if (role == null) {
                        errors.add(new UserImportError(
                            rowNumber,
                            userDto.email(),
                            userDto.nationalId(),
                            roleName,
                            "El rol '" + roleName + "' no existe en la base de datos"
                        ));
                        continue;
                    }
                    
                    User user = userMapper.toUserSaveDtoToEntity(userDto);
                    user.setPassword(passwordEncoder.encode(userDto.password()));
                    user.setActive(true);
                    user.setRoles(List.of(role));
                    
                    usersToSave.add(user);
                    
                } catch (Exception e) {
                    log.error("Error procesando usuario en fila {}: {}", rowNumber, e.getMessage(), e);
                    String roleName = (userDto.roles() != null && !userDto.roles().isEmpty()) 
                        ? userDto.roles().get(0) 
                        : DEFAULT_ROLE;
                    errors.add(new UserImportError(
                        rowNumber,
                        userDto.email(),
                        userDto.nationalId(),
                        roleName,
                        "Error: " + e.getMessage()
                    ));
                }
            }
            
            int successCount = 0;
            if (!usersToSave.isEmpty()) {
                try {
                    List<User> savedUsers = userRepository.saveAll(usersToSave);
                    successCount = savedUsers.size();
                    
                    importedUsers = savedUsers.stream()
                        .map(userMapper::toDto)
                        .toList();
                    
                    log.info("Se guardaron exitosamente {} usuarios", successCount);
                    
                } catch (Exception e) {
                    log.error("Error guardando usuarios en lote: {}", e.getMessage(), e);
                    throw new BadRequestException("Error al guardar usuarios: " + e.getMessage());
                }
            }
            
            int failCount = errors.size();
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("totalRecords", users.size());
            metadata.put("successCount", successCount);
            metadata.put("failCount", failCount);
            metadata.put("errorSample", errors.stream().limit(5).map(UserImportError::errorMessage).collect(Collectors.toList()));
            
            auditHelper.logComplete(
                UserActionType.USER_BULK_IMPORT.name(),
                String.format("Importación masiva completada: %d exitosos, %d fallidos de %d totales", 
                            successCount, failCount, users.size()),
                EntityType.USER,
                null,
                ActionResult.SUCCESS,
                metadata
            );
            
            log.info("Importación masiva completada: {} exitosos, {} fallidos de {} totales", 
                     successCount, failCount, users.size());
            
            return new BulkUserImportResult(
                users.size(),
                successCount,
                failCount,
                errors,
                importedUsers
            );
            
        } catch (Exception e) {
            auditHelper.logFailure(
                UserActionType.USER_BULK_IMPORT.name(),
                "Error en importación masiva de usuarios",
                e.getMessage()
            );
            throw e;
        }
    }

    @Override
    public byte[] exportUsersToCSV(List<UserDto> users) throws IOException {
        try {
            log.info("Exportando {} usuarios a CSV", users.size());
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            
            writer.println("name,email,nationalId,phone,role,active,createdAt");
            
            for (UserDto user : users) {
                String role = (user.roles() != null && !user.roles().isEmpty()) 
                    ? user.roles().get(0) 
                    : DEFAULT_ROLE;
                
                String createdAt = (user.createdAt() != null) 
                    ? user.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) 
                    : "";
                
                writer.printf("%s,%s,%s,%s,%s,%s,%s%n",
                    escapeCSV(user.name()),
                    escapeCSV(user.email()),
                    escapeCSV(user.nationalId()),
                    escapeCSV(user.phone()),
                    role,
                    user.active(),
                    createdAt
                );
            }
            
            writer.flush();
            writer.close();
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("usersExported", users.size());
            
            auditHelper.logComplete(
                UserActionType.USER_EXPORT.name(),
                String.format("Exportación de %d usuarios a CSV", users.size()),
                EntityType.USER,
                null,
                ActionResult.SUCCESS,
                metadata
            );
            
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            auditHelper.logFailure(
                UserActionType.USER_EXPORT.name(),
                "Error al exportar usuarios a CSV",
                e.getMessage()
            );
            throw e;
        }
    }

    public byte[] exportAllUsersToCSV() throws IOException {
        List<UserDto> allUsers = findAll();
        return exportUsersToCSV(allUsers);
    }

    @Override
    @Transactional
    public BulkUserImportResult importUsersFromCSV(MultipartFile file) throws IOException {
        try {
            log.info("Iniciando importación de usuarios desde CSV: {}", file.getOriginalFilename());
            
            validateCSVFile(file);
            List<UserSaveDto> users = parseCSVFile(file);
            
            if (users.isEmpty()) {
                throw new BadRequestException("No se encontraron usuarios válidos en el archivo CSV");
            }
            
            log.info("Se parsearon {} registros del CSV", users.size());
            
            return saveBulkUsers(users);
            
        } catch (Exception e) {
            auditHelper.logFailure(
                UserActionType.USER_BULK_IMPORT.name(),
                "Error al importar usuarios desde CSV: " + file.getOriginalFilename(),
                e.getMessage()
            );
            throw e;
        }
    }

    // ==================== MÉTODOS PRIVADOS DE VALIDACIÓN ====================

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
    
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
    
        return value;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private Role findRoleByName(String roleName) {
        Role role = roleRepository.findByNameContainingIgnoreCase(roleName);
        if (role == null) {
            throw new ResourceNotFoundException("Role", "name", roleName);
        }
        return role;
    }

    private String validateAndNormalizeRole(String roleName) {
        String normalized = roleName == null ? "" : roleName.trim().toUpperCase();
        if (!ALLOWED_ROLE_NAMES.contains(normalized)) {
            throw new BadRequestException("Role not allowed: " + roleName);
        }
        return normalized;
    }

    private String normalizeRoleForFilter(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "";
        }
        
        String normalized = roleName.trim().toUpperCase();
        if (!ALLOWED_ROLE_NAMES.contains(normalized)) {
            throw new BadRequestException("Rol no válido: " + roleName + 
                ". Roles permitidos: " + String.join(", ", ALLOWED_ROLE_NAMES));
        }
        return normalized;
    }

    private void validateRolesProvided(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new BadRequestException("Se debe proporcionar al menos un rol para el usuario");
        }
    }

    private void validateOnlyCitizenRole(List<String> roles) {
        if (roles != null && !roles.isEmpty()) {
            boolean hasNonCitizen = roles.stream()
                .anyMatch(r -> !r.equalsIgnoreCase(DEFAULT_ROLE));
            if (hasNonCitizen) {
                throw new BadRequestException("Solo se permite el rol 'CIUDADANO'");
            }
        }
    }

    private void validateUniqueFieldsForNewUser(String email, String nationalId) {
        List<User> existingUsers = userRepository.findByEmailInOrNationalIdIn(
            email != null ? List.of(email) : List.of(),
            nationalId != null ? List.of(nationalId) : List.of()
        );
        
        if (existingUsers.isEmpty()) {
            return;
        }
        
        boolean emailExists = existingUsers.stream()
            .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        
        boolean nationalIdExists = existingUsers.stream()
            .anyMatch(u -> u.getNationalId().equalsIgnoreCase(nationalId));
        
        if (emailExists && nationalIdExists) {
            throw new DuplicateResourceException(
                "User", 
                "email y nationalId", 
                "email: " + email + " y cédula: " + nationalId
            );
        } else if (emailExists) {
            throw new DuplicateResourceException("User", "email", email);
        } else if (nationalIdExists) {
            throw new DuplicateResourceException("User", "nationalId", nationalId);
        }
    }

    private User createUserFromDto(UserSaveDto user) {
        User userToSave = userMapper.toUserSaveDtoToEntity(user);
        userToSave.setPassword(passwordEncoder.encode(userToSave.getPassword()));
        return userToSave;
    }

    private List<Role> validateAndFetchRoles(List<String> roleNames) {
        List<Role> roles = roleNames.stream()
            .map(roleRepository::findByNameContainingIgnoreCase)
            .filter(Objects::nonNull)
            .toList();

        if (roles.isEmpty()) {
            throw new BadRequestException("Los roles proporcionados no existen en la base de datos");
        }
        
        return roles;
    }

    private Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
        int validatedPage = Math.max(page, MIN_PAGE_NUMBER);
        int validatedSize = validatePageSize(size);
        
        Sort.Direction direction = (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
        
        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : DEFAULT_SORT_FIELD;
        Sort sort = Sort.by(direction, sortField);
        
        return PageRequest.of(validatedPage, validatedSize, sort);
    }

    private int validatePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private UserDto enrichWithLastAction(User user) {
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
    }

    private void validateCSVFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo está vacío");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new BadRequestException("El archivo debe tener extensión .csv");
        }
        
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BadRequestException(
                String.format("El archivo es demasiado grande. Tamaño máximo: %.2f MB", 
                             maxSize / (1024.0 * 1024.0))
            );
        }
        
        String contentType = file.getContentType();
        if (contentType != null && 
            !contentType.equals("text/csv") && 
            !contentType.equals("application/csv") &&
            !contentType.equals("text/plain")) {
            log.warn("Content-Type no estándar para CSV: {}", contentType);
        }
        
        log.debug("Archivo CSV validado: {} ({} bytes)", filename, file.getSize());
    }

    private List<UserSaveDto> parseCSVFile(MultipartFile file) throws IOException {
        List<UserSaveDto> users = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            List<String[]> records = reader.readAll();
            
            if (records.isEmpty()) {
                throw new BadRequestException("El archivo CSV está vacío");
            }
            
            boolean hasHeader = detectHeaderRow(records.get(0));
            int startIndex = hasHeader ? 1 : 0;
            
            log.debug("CSV tiene header: {}. Comenzando desde fila {}", hasHeader, startIndex + 1);
            
            for (int i = startIndex; i < records.size(); i++) {
                String[] record = records.get(i);
                
                if (isEmptyOrIncompleteRow(record)) {
                    log.debug("Saltando fila {} (vacía o incompleta)", i + 1);
                    continue;
                }
                
                try {
                    UserSaveDto userDto = parseUserFromCSVRow(record, i + 1);
                    users.add(userDto);
                } catch (BadRequestException e) {
                    log.warn("Error parseando fila {}: {}", i + 1, e.getMessage());
                }
            }
            
        } catch (CsvException e) {
            throw new BadRequestException("Error al leer el archivo CSV: " + e.getMessage());
        }
        
        return users;
    }

    private boolean detectHeaderRow(String[] row) {
        if (row == null || row.length < 5) {
            return false;
        }
        
        String firstCell = row[0].toLowerCase().trim();
        
        return firstCell.contains("name") || 
               firstCell.contains("nombre") ||
               firstCell.contains("usuario") ||
               firstCell.equals("name") ||
               firstCell.equals("nombre");
    }

    private boolean isEmptyOrIncompleteRow(String[] record) {
        if (record == null || record.length < 5) {
            return true;
        }
        
        for (String field : record) {
            if (field != null && !field.trim().isEmpty()) {
                return false;
            }
        }
        
        return true;
    }

    private UserSaveDto parseUserFromCSVRow(String[] record, int rowNumber) {
        if (record.length < 5) {
            throw new BadRequestException(
                String.format("Fila %d: formato incorrecto. Se esperan al menos 5 columnas", rowNumber)
            );
        }
        
        String name = cleanCSVField(record[0]);
        String email = cleanCSVField(record[1]);
        String nationalId = cleanCSVField(record[2]);
        String phone = cleanCSVField(record[3]);
        String password = cleanCSVField(record[4]);
        String role = record.length > 5 ? cleanCSVField(record[5]).toUpperCase() : DEFAULT_ROLE;
        
        if (role.isBlank()) {
            role = DEFAULT_ROLE;
        }
        
        return new UserSaveDto(
            name,
            email,
            nationalId,
            phone,
            password,
            true,
            List.of(role)
        );
    }

    private String cleanCSVField(String field) {
        if (field == null) {
            return "";
        }
        
        field = field.trim();

        if (field.startsWith("\"") && field.endsWith("\"") && field.length() > 1) {
            field = field.substring(1, field.length() - 1);
        }
        
        field = field.replace("\"\"", "\"");
        
        return field;
    }
}