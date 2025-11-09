package com.unimagdalena.conectaCiudad.services.user;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;
import com.unimagdalena.conectaCiudad.Dto.action.ActionSaveDto;
import com.unimagdalena.conectaCiudad.Dto.user.BulkUserImportResult;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserImportError;
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
import lombok.extern.slf4j.Slf4j;

import com.unimagdalena.conectaCiudad.repositories.RoleRepository;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;

import org.springframework.web.multipart.MultipartFile;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
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
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final ProjectRepository projectRepository;
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
                .map(this::enrichWithLastAction)
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
        
        validateUserCanBeDeleted(id);
        
        UserDto userDto = userMapper.toDto(user);
        logAction(UserActionType.USER_DELETED, "Usuario eliminado " + userDto.id(), id);
        userRepository.delete(user);
    }

    @Override
    public UserDto addRole(Long userId, String roleName) {
        User user = findUserById(userId);
        String normalizedRole = validateAndNormalizeRole(roleName);
        Role role = findRoleByName(normalizedRole);
        
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
        User user = findUserById(userId);
        String normalizedRole = validateAndNormalizeRole(roleName);
        
        List<Role> roles = new ArrayList<>(user.getRoles());
        roles.removeIf(r -> r.getName().equalsIgnoreCase(normalizedRole));
        
        if (roles.isEmpty()) {
            Role defaultRole = findRoleByName(DEFAULT_ROLE);
            roles.add(defaultRole);
        }
        
        user.setRoles(roles);
        UserDto userDto = userMapper.toDto(userRepository.save(user));
        logAction(UserActionType.USER_ROLE_REMOVED, "Rol " + roleName + " removido del usuario " + userDto.id(), userId);
        return userDto;
    }

    @Override
    public UserDto saveUser(UserSaveDto user) {
        validateRolesProvided(user.roles());
        validateUniqueFieldsForNewUser(user.email(), user.nationalId());
        
        User userToSave = createUserFromDto(user);
        List<Role> roles = validateAndFetchRoles(user.roles());
        
        userToSave.setActive(true);
        userToSave.setRoles(roles);
        
        UserDto userDto = userMapper.toDto(userRepository.save(userToSave));
        
        String roleNames = roles.stream().map(Role::getName).collect(Collectors.joining(", "));
        logAction(UserActionType.USER_CREATED, "Usuario " + userDto.id() + " creado con rol: " + roleNames, userDto.id());
        
        return userDto;
    }

    @Override
    public UserDto saveUserDefault(UserSaveDto user) {
        validateOnlyCitizenRole(user.roles());
        validateUniqueFieldsForNewUser(user.email(), user.nationalId());
        
        User userToSave = createUserFromDto(user);
        userToSave.setActive(true);
        
        Role defaultRole = findRoleByName(DEFAULT_ROLE);
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
        User user = findUserById(userId);
        
        boolean newStatus = !user.getActive();
        user.setActive(newStatus);
        
        log.info("Estado de usuario {} cambiado a {}", user.getEmail(), newStatus ? "activo" : "inactivo");
        
        logActionForAdmin(
            newStatus ? UserActionType.USER_ACTIVATED : UserActionType.USER_DEACTIVATED,
            "El estado del usuario " + user.getEmail() + " ha sido cambiado a " + (newStatus ? "activo" : "inactivo")
        );
        
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public Page<UserDto> findAllExceptCurrent(Long currentUserId, int page, int size, String sortBy, String sortDirection) {
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        
        Page<User> userPage = (currentUserId != null) 
            ? userRepository.findAllExceptUser(currentUserId, pageable)
            : userRepository.findAll(pageable);
        
        return userPage.map(this::enrichWithLastAction);
    }

    @Override
    public Page<UserDto> findByNameWithPagination(String name, Long currentUserId, int page, int size, String sortBy, String sortDirection) {
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        
        Page<User> userPage = (currentUserId != null)
            ? userRepository.findByNameContainingIgnoreCaseAndIdNot(name, currentUserId, pageable)
            : userRepository.findByNameContainingIgnoreCase(name, pageable);
        
        return userPage.map(this::enrichWithLastAction);
    }

    @Override
    public Page<UserDto> findByFilters(String roleName, Boolean active, Long currentUserId,
                                        int page, int size, String sortBy, String sortDirection) {
        String normalizedRole = normalizeRoleForFilter(roleName);
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        
        Page<User> userPage = userRepository.findByRoleAndActiveStatus(
            normalizedRole, 
            active, 
            currentUserId, 
            pageable
        );
        
        return userPage.map(this::enrichWithLastAction);
    }

    @Override
    public Page<UserDto> findByNameAndFilters(String name, String roleName, Boolean active, 
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
        
        return userPage.map(this::enrichWithLastAction);
    }

    @Override
    public void validateUniqueFields(String email, String nationalId) {
        if (email == null && nationalId == null) {
            return;
        }

        Optional<User> existingUser = userRepository.findByEmailOrNationalId(email, nationalId);
        existingUser.ifPresent(u -> {
            if (email != null && u.getEmail().equalsIgnoreCase(email)) {
                throw new DuplicateResourceException("User", "email", email);
            }
            if (nationalId != null && u.getNationalId().equalsIgnoreCase(nationalId)) {
                throw new DuplicateResourceException("User", "nationalId", nationalId);
            }
        });
    }

    @Override
@Transactional
public BulkUserImportResult saveBulkUsers(List<UserSaveDto> users) {
    log.info("Iniciando importación masiva de {} usuarios", users.size());
    
    List<UserDto> importedUsers = new ArrayList<>();
    List<UserImportError> errors = new ArrayList<>();
    
    // Pre-validación: verificar duplicados en el CSV
    Map<String, Integer> emailMap = new HashMap<>();
    Map<String, Integer> nationalIdMap = new HashMap<>();
    Set<Integer> rowsWithErrors = new HashSet<>();
    
    for (int i = 0; i < users.size(); i++) {
        UserSaveDto user = users.get(i);
        int rowNumber = i + 2; // +2 porque empieza en 1 y la primera es header
        String role = (user.roles() != null && !user.roles().isEmpty()) 
            ? user.roles().get(0) 
            : DEFAULT_ROLE;
        
        // Verificar duplicados dentro del CSV
        if (emailMap.containsKey(user.email())) {
            errors.add(new UserImportError(
                rowNumber, 
                user.email(), 
                user.nationalId(),
                role,
                "Email duplicado en el CSV (también en fila " + emailMap.get(user.email()) + ")"
            ));
            rowsWithErrors.add(rowNumber);
            continue;
        }
        
        if (nationalIdMap.containsKey(user.nationalId())) {
            errors.add(new UserImportError(
                rowNumber, 
                user.email(), 
                user.nationalId(),
                role,
                "Cédula duplicada en el CSV (también en fila " + nationalIdMap.get(user.nationalId()) + ")"
            ));
            rowsWithErrors.add(rowNumber);
            continue;
        }
        
        // Validar formato básico
        if (user.email() == null || user.email().isBlank()) {
            errors.add(new UserImportError(rowNumber, user.email(), user.nationalId(), role, 
                "Email es obligatorio"));
            rowsWithErrors.add(rowNumber);
            continue;
        }
        
        if (user.nationalId() == null || user.nationalId().isBlank()) {
            errors.add(new UserImportError(rowNumber, user.email(), user.nationalId(), role, 
                "Cédula es obligatoria"));
            rowsWithErrors.add(rowNumber);
            continue;
        }
        
        emailMap.put(user.email(), rowNumber);
        nationalIdMap.put(user.nationalId(), rowNumber);
    }
    
    // Verificar duplicados en BD en una sola consulta
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
    
    // Obtener roles existentes una sola vez
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
    
    // Procesar cada usuario y guardar los que sean válidos
    List<User> usersToSave = new ArrayList<>();
    
    for (int i = 0; i < users.size(); i++) {
        UserSaveDto userDto = users.get(i);
        int rowNumber = i + 2;
        
        // Saltar si ya tiene error
        if (rowsWithErrors.contains(rowNumber)) {
            continue;
        }
        
        try {
            String roleName = (userDto.roles() != null && !userDto.roles().isEmpty()) 
                ? userDto.roles().get(0).trim().toUpperCase() 
                : DEFAULT_ROLE;
            
            // Verificar si ya existe en BD
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
            
            // Validar rol
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
            
            // Crear usuario
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
    
    // Guardar todos los usuarios válidos en lote
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
    
    // Log de acción única
    logActionForAdmin(
        UserActionType.USER_BULK_IMPORT,
        String.format("Importación masiva completada: %d exitosos, %d fallidos de %d totales", 
                     successCount, failCount, users.size())
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
}

@Override
public byte[] exportUsersToCSV(List<UserDto> users) throws IOException {
    log.info("Exportando {} usuarios a CSV", users.size());
    
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
    
    // Header
    writer.println("name,email,nationalId,phone,role,active,createdAt");
    
    // Datos
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
    
    logActionForAdmin(
        UserActionType.USER_EXPORT,
        String.format("Exportación de %d usuarios a CSV", users.size())
    );
    
    return outputStream.toByteArray();
}

@Override
public byte[] exportAllUsersToCSV() throws IOException {
    List<UserDto> allUsers = findAll();
    return exportUsersToCSV(allUsers);
}

private String escapeCSV(String value) {
    if (value == null) {
        return "";
    }
    
    // Si contiene coma, comillas o salto de línea, envolver en comillas
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

    private void validateUserCanBeDeleted(Long userId) {
        long projects = projectRepository.countByCreatorId(userId);
        if (projects > 0) {
            throw new BadRequestException("No se puede eliminar el usuario: tiene " + projects + " proyecto(s) asociados");
        }
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
        Optional<User> existingUser = userRepository.findByEmailOrNationalId(email, nationalId);
        existingUser.ifPresent(u -> {
            if (u.getEmail().equals(email)) {
                throw new DuplicateResourceException("User", "email", email);
            }
            if (u.getNationalId().equals(nationalId)) {
                throw new DuplicateResourceException("User", "nationalId", nationalId);
            }
        });
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

    @Override
@Transactional
public BulkUserImportResult importUsersFromCSV(MultipartFile file) throws IOException {
    log.info("Iniciando importación de usuarios desde CSV: {}", file.getOriginalFilename());
    

    validateCSVFile(file);
    

    List<UserSaveDto> users = parseCSVFile(file);
    
    if (users.isEmpty()) {
        throw new BadRequestException("No se encontraron usuarios válidos en el archivo CSV");
    }
    
    log.info("Se parsearon {} registros del CSV", users.size());
    
    return saveBulkUsers(users);
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
                } catch (Exception e) {
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
        
        if (name == null || name.isBlank()) {
            throw new BadRequestException(String.format("Fila %d: el nombre es obligatorio", rowNumber));
        }
        
        if (email == null || email.isBlank()) {
            throw new BadRequestException(String.format("Fila %d: el email es obligatorio", rowNumber));
        }
        
        if (nationalId == null || nationalId.isBlank()) {
            throw new BadRequestException(String.format("Fila %d: la cédula es obligatoria", rowNumber));
        }
        
        if (password == null || password.isBlank()) {
            throw new BadRequestException(String.format("Fila %d: la contraseña es obligatoria", rowNumber));
        }
        
        if (role.isBlank()) {
            role = DEFAULT_ROLE;
        }
        
        return new UserSaveDto(
            name,
            email,
            nationalId,
            phone,
            password,
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