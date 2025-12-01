package com.unimagdalena.conectaCiudad.services.admin;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.page.Statistics;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectMapper;
import com.unimagdalena.conectaCiudad.Dto.user.*;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.entities.Role;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.*;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.DuplicateResourceException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.ReviewRepository;
import com.unimagdalena.conectaCiudad.repositories.RoleRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.services.action.ActionService;
import com.unimagdalena.conectaCiudad.events.UserCreatedEvent;
import com.unimagdalena.conectaCiudad.events.UserUpdatedEvent;
import com.unimagdalena.conectaCiudad.events.UserDeletedEvent;
import com.unimagdalena.conectaCiudad.events.ReviewAssignedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.unimagdalena.conectaCiudad.specifications.ProjectSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {


    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_PAGE_NUMBER = 0;
    private static final String DEFAULT_SORT_FIELD = "name";
    private static final String DEFAULT_ROLE = "CIUDADANO";
    private static final Set<String> ALLOWED_ROLE_NAMES = Set.of(
            "ADMIN", "CIUDADANO", "CURATOR", "LIDER_COMUNITARIO"
    );
    private static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final String PHONE_REGEX = "^(\\+?\\d{10,15})$";
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final String[] EXPECTED_CSV_HEADERS = {
            "name", "email", "nationalId", "phone", "password", "role"
    };
    private static final Long SYSTEM_USER_ID = 1L;
    private static final long MAX_CSV_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB


    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RoleRepository roleRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReviewRepository reviewRepository;
    private final ActionService actionService;
    private final ProjectMapper projectMapper;


    @Override
    @Transactional
    public UserDto createUser(UserSaveDto userDto) {
        log.info("Admin creando usuario: {}", userDto.email());

        validateRolesProvided(userDto.roles());
        validateUniqueFields(userDto.email(), userDto.nationalId());

        User user = buildUserFromDto(userDto);
        List<Role> roles = fetchAndValidateRoles(userDto.roles());

        user.setActive(true);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        log.info("Usuario {} creado exitosamente con ID: {}",
                savedUser.getEmail(), savedUser.getId());

        User creator = getCurrentUser();
        eventPublisher.publishEvent(new UserCreatedEvent(this, savedUser, creator));

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long userId, UserSaveDto updateDto) {
        log.info("Admin actualizando usuario ID: {}", userId);

        User user = findUserByIdOrThrow(userId);

        updateUserFields(user, updateDto);

        User savedUser = userRepository.save(user);

        log.info("Usuario {} actualizado exitosamente", savedUser.getEmail());

        User updater = getCurrentUser();
        eventPublisher.publishEvent(new UserUpdatedEvent(this, savedUser, updater));

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Admin eliminando usuario ID: {}", userId);

        User user = findUserByIdOrThrow(userId);

        validateNoActiveProjects(userId);

        int reassignedProjects = reassignInactiveProjects(userId);

        String userName = user.getName();
        String userEmail = user.getEmail();
        userRepository.delete(user);

        log.info("Usuario {} ({}) eliminado exitosamente. Proyectos reasignados: {}",
                userName, userEmail, reassignedProjects);

        User deleter = getCurrentUser();
        eventPublisher.publishEvent(new UserDeletedEvent(this, userId, userName, userEmail, deleter));
    }


    @Override
    @Transactional
    public UserDto toggleUserStatus(Long userId) {
        log.info("Admin cambiando estado del usuario ID: {}", userId);

        User user = findUserByIdOrThrow(userId);

        boolean oldStatus = user.getActive();
        boolean newStatus = !oldStatus;

        user.setActive(newStatus);
        User savedUser = userRepository.save(user);

        log.info("Estado de usuario {} cambiado a: {}",
                user.getEmail(), newStatus ? "ACTIVO" : "INACTIVO");

        User updater = getCurrentUser();
        eventPublisher.publishEvent(new UserUpdatedEvent(this, savedUser, updater));

        return userMapper.toDto(savedUser);
    }


    @Override
    @Transactional
    public UserDto assignRole(Long userId, String roleName) {
        log.info("Admin asignando rol '{}' al usuario ID: {}", roleName, userId);

        User user = findUserByIdOrThrow(userId);
        String normalizedRole = normalizeAndValidateRole(roleName);
        Role role = findRoleByNameOrThrow(normalizedRole);

        String oldRole = getCurrentRoleName(user);

        user.getRoles().clear();
        user.getRoles().add(role);

        User savedUser = userRepository.save(user);

        log.info("Rol del usuario {} cambiado de '{}' a '{}'",
                user.getEmail(), oldRole, role.getName());

        User updater = getCurrentUser();
        eventPublisher.publishEvent(new UserUpdatedEvent(this, savedUser, updater));

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto removeRole(Long userId, String roleName) {
        log.info("Admin removiendo rol '{}' del usuario ID: {}", roleName, userId);

        User user = findUserByIdOrThrow(userId);
        String normalizedRole = normalizeAndValidateRole(roleName);

        List<Role> roles = new ArrayList<>(user.getRoles());
        roles.removeIf(r -> r.getName().equalsIgnoreCase(normalizedRole));

        boolean assignedDefault = false;
        if (roles.isEmpty()) {
            Role defaultRole = findRoleByNameOrThrow(DEFAULT_ROLE);
            roles.add(defaultRole);
            assignedDefault = true;
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        log.info("Rol '{}' removido del usuario {}. Rol por defecto asignado: {}",
                normalizedRole, user.getEmail(), assignedDefault);

        User updater = getCurrentUser();
        eventPublisher.publishEvent(new UserUpdatedEvent(this, savedUser, updater));

        return userMapper.toDto(savedUser);
    }

    @Override
    public PagedResponse<UserDto> findAllExceptCurrent(Long currentUserId, int page, int size,
                                                       String sortBy, String sortDirection) {
        log.debug("Buscando todos los usuarios excepto ID: {}", currentUserId);

        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);

        Page<User> userPage = (currentUserId != null)
                ? userRepository.findAllExceptUser(currentUserId, pageable)
                : userRepository.findAll(pageable);

        Page<UserDto> enrichedUsers = userPage.map(this::enrichWithLastAction);

        Statistics<UserDto> stats = calculateUserStatistics(currentUserId, null, null, null);

        return new PagedResponse<>(enrichedUsers, stats);
    }


    @Override
    public PagedResponse<UserDto> findByFilters(String roleName, Boolean active, Long currentUserId,
                                                int page, int size, String sortBy, String sortDirection) {
        log.debug("Filtrando usuarios - Rol: '{}', Activo: {}, Excepto ID: {}",
                roleName, active, currentUserId);

        String normalizedRole = normalizeRoleForFilter(roleName);
        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);

        Page<User> userPage = userRepository.findByRoleAndActiveStatus(
                normalizedRole, active, currentUserId, pageable
        );

        Page<UserDto> enrichedUsers = userPage.map(this::enrichWithLastAction);

        Statistics<UserDto> stats = calculateUserStatistics(
                currentUserId, null, normalizedRole, active
        );

        return new PagedResponse<>(enrichedUsers, stats);
    }

    @Override
    public PagedResponse<UserDto> findByNameAndFilters(String name, String roleName, Boolean active,
                                                       Long currentUserId, int page, int size,
                                                       String sortBy, String sortDirection) {
        log.debug("Búsqueda completa - Nombre: '{}', Rol: '{}', Activo: {}, Excepto ID: {}",
                name, roleName, active, currentUserId);

        String normalizedRole = normalizeRoleForFilter(roleName);
        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);

        Page<User> userPage = userRepository.findByNameAndRoleAndActiveStatus(
                name, normalizedRole, active, currentUserId, pageable
        );

        Page<UserDto> enrichedUsers = userPage.map(this::enrichWithLastAction);

        Statistics<UserDto> stats = calculateUserStatistics(
                currentUserId, name, normalizedRole, active
        );

        return new PagedResponse<>(enrichedUsers, stats);
    }


    @Override
    public CuratorInfoDto findAllCuratorsWithStats(Long projectId) {
        log.debug("Obteniendo curadores con estadísticas para proyecto ID: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        Long currentCuratorId = getCurrentCuratorId(project);

        List<User> allCurators = userRepository.findActiveCurators();

        if (allCurators.isEmpty()) {
            return new CuratorInfoDto(null, Collections.emptyList());
        }

        Map<Long, Map<String, Object>> statsMap = getCuratorStatsMap(allCurators);

        return buildCuratorInfo(allCurators, currentCuratorId, statsMap);
    }

    @Override
    public List<UserDto> findAll() {
        log.debug("Obteniendo todos los usuarios");

        return userRepository.findAll()
                .stream()
                .map(this::enrichWithLastAction)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BulkUserImportResult saveBulkUsers(List<UserSaveDto> users) {
        log.info("Iniciando importación masiva de {} usuarios", users.size());

        try {
            ValidationResult internalValidation = validateInternalDuplicates(users);

            ValidationResult dbValidation = validateAgainstDatabase(users, internalValidation.validRows());

            List<User> usersToSave = prepareUsersForSave(users, dbValidation.validRows());

            List<UserDto> savedUsers = saveUsersInBatch(usersToSave);


            List<UserImportError> allErrors = new ArrayList<>();
            allErrors.addAll(internalValidation.errors());
            allErrors.addAll(dbValidation.errors());

            BulkUserImportResult result = new BulkUserImportResult(
                    users.size(),
                    savedUsers.size(),
                    allErrors.size(),
                    allErrors,
                    savedUsers
            );

            log.info("Importación masiva completada: {} exitosos, {} fallidos de {} totales",
                    result.successCount(), result.failCount(), result.totalRecords());

            return result;

        } catch (Exception e) {
            eventPublisher.publishEvent(new com.unimagdalena.conectaCiudad.events.ActionFailedEvent(
                    this,
                    UserActionType.USER_BULK_IMPORT.name(),
                    "Error en importación masiva",
                    e,
                    EntityType.USER,
                    null,
                    getCurrentUser()
            ));
            throw e;
        }
    }

    @Override
    public Statistics<ProjectDto> getGlobalStatistics() {

    List<Project> all = projectRepository.findAll();

    Map<ProjectStatus, Long> countByStatus = new EnumMap<>(ProjectStatus.class);

    for (ProjectStatus s : ProjectStatus.values()) {
        long count = all.stream()
                .filter(p -> p.getStatus() == s)
                .count();
        countByStatus.put(s, count);
    }

    long total = all.size();

    long createdToday = all.stream()
            .filter(p -> p.getCreatedAt().isAfter(
                    OffsetDateTime.now().toLocalDate().atStartOfDay().atOffset(OffsetDateTime.now().getOffset())
            )).count();

    return new Statistics<>(
        total,
        Map.of(
            "total", total,
            "pending", countByStatus.getOrDefault(ProjectStatus.PENDING_REVIEW, 0L),
            "inReview", countByStatus.getOrDefault(ProjectStatus.IN_REVIEW, 0L),
            "withObservations", countByStatus.getOrDefault(ProjectStatus.RETURNED_WITH_OBSERVATIONS, 0L),
            "readyToPublish", countByStatus.getOrDefault(ProjectStatus.READY_TO_PUBLISH, 0L),
            "published", countByStatus.getOrDefault(ProjectStatus.PUBLISHED, 0L),
            "votingClosed", countByStatus.getOrDefault(ProjectStatus.VOTING_CLOSED, 0L),

            "inProgress",
            countByStatus.getOrDefault(ProjectStatus.PENDING_REVIEW, 0L)
            + countByStatus.getOrDefault(ProjectStatus.IN_REVIEW, 0L)
            + countByStatus.getOrDefault(ProjectStatus.RETURNED_WITH_OBSERVATIONS, 0L),


            "completed",
                countByStatus.getOrDefault(ProjectStatus.READY_TO_PUBLISH, 0L)
                + countByStatus.getOrDefault(ProjectStatus.PUBLISHED, 0L)
                + countByStatus.getOrDefault(ProjectStatus.VOTING_CLOSED, 0L),

            "createdToday", createdToday
        )
    );
}

    @Override
    @Transactional
    public BulkUserImportResult importUsersFromCSV(MultipartFile file) throws IOException {
        log.info("Importando usuarios desde CSV: {}", file.getOriginalFilename());

        try {
            validateCSVFile(file);

            List<UserSaveDto> users = parseCSVFile(file);

            if (users.isEmpty()) {
                throw new BadRequestException(ErrorCode.CSV_EMPTY);
            }

            log.info("Se parsearon {} registros del CSV", users.size());

            return saveBulkUsers(users);

        } catch (Exception e) {
            eventPublisher.publishEvent(new com.unimagdalena.conectaCiudad.events.ActionFailedEvent(
                    this,
                    UserActionType.USER_BULK_IMPORT.name(),
                    "Error importing users from CSV",
                    e,
                    EntityType.USER,
                    null,
                    getCurrentUser()
            ));
            throw e;
        }
    }

    @Override
    public byte[] exportUsersToCSV(List<UserDto> users) throws IOException {
        log.info("Exportando {} usuarios a CSV", users.size());

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try (PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {

                writer.println("name,email,nationalId,phone,role,active,createdAt");

                for (UserDto user : users) {
                    writer.println(buildCSVRow(user));
                }

                writer.flush();
            }

            return outputStream.toByteArray();

        } catch (Exception e) {
            eventPublisher.publishEvent(new com.unimagdalena.conectaCiudad.events.ActionFailedEvent(
                    this,
                    UserActionType.USER_EXPORT.name(),
                    "Error al exportar usuarios a CSV",
                    e,
                    EntityType.USER,
                    null,
                    getCurrentUser()
            ));
            throw e;
        }
    }

    @Override
    public byte[] exportAllUsersToCSV() throws IOException {
        List<UserDto> allUsers = findAll();
        return exportUsersToCSV(allUsers);
    }

    @Override
    public Page<ProjectDto> findWithFilters(
            String searchTerm,
            ProjectStatus status,
            Long creatorId,
            Long curatorId,
            LocalDate projectStartFrom,
            LocalDate projectStartTo,
            LocalDate projectEndFrom,
            LocalDate projectEndTo,
            LocalDate votingStartFrom,
            LocalDate votingStartTo,
            LocalDate votingEndFrom,
            LocalDate votingEndTo,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo,
            Pageable pageable
    ) {

        validateDateRange(projectStartFrom, projectStartTo);
        validateDateRange(projectEndFrom, projectEndTo);
        validateDateRange(votingStartFrom, votingStartTo);
        validateDateRange(votingEndFrom, votingEndTo);
        validateDateRange(createdFrom, createdTo);
        String normalizedSearchTerm = (searchTerm != null && !searchTerm.trim().isEmpty())
                ? searchTerm.trim()
                : null;

        Specification<Project> spec = ProjectSpecifications.withFilters(
                normalizedSearchTerm,
                status,
                creatorId,
                curatorId,
                projectStartFrom,
                projectStartTo,
                projectEndFrom,
                projectEndTo,
                votingStartFrom,
                votingStartTo,
                votingEndFrom,
                votingEndTo,
                createdFrom,
                createdTo
        );

        return projectRepository
                .findAll(spec, pageable)
                .map(projectMapper::toDto);
    }

    @Override
    public ProjectDto reassignCurator(Long projectId, Long curatorId, Long adminId, Long accessId) {
        Project project = findProjectById(projectId);
        User newCurator = findUserById(curatorId);

        if (!project.getStatus().canBeReviewed()) {
            throw new BadRequestException(
                    ErrorCode.PROJECT_NOT_REVIEWABLE,
                    Map.of("currentStatus", project.getStatus().name())
            );
        }

        List<Review> reviews = reviewRepository.findByProjectId(projectId);
        Review review = getOrCreateReview(reviews, project, newCurator);

        review.setCurator(newCurator);
        Review savedReview = reviewRepository.save(review);

        User admin = findUserById(adminId);
        eventPublisher.publishEvent(new ReviewAssignedEvent(this, savedReview, admin));

        return projectMapper.toDto(project);
    }



    private User getCurrentUser() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String nationalId = authentication.getName();
        return userRepository.findByNationalId(nationalId);
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

    private String normalizeAndValidateRole(String roleName) {
        String normalized = roleName == null ? "" : roleName.trim().toUpperCase();
        if (!ALLOWED_ROLE_NAMES.contains(normalized)) {
            throw new BadRequestException(
                    ErrorCode.INVALID_ROLE_FOR_OPERATION,
                    Map.of(
                            "providedRole", roleName,
                            "allowedRoles", String.join(", ", ALLOWED_ROLE_NAMES)
                    )
            );
        }
        return normalized;
    }

    private String normalizeRoleForFilter(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "";
        }
        return normalizeAndValidateRole(roleName);
    }

    private void validateRolesProvided(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new BadRequestException(
                    ErrorCode.REQUIRED_FIELD,
                    Map.of("field", "roles")
            );
        }
    }

    private void validateUniqueFields(String email, String nationalId) {
        List<User> existingUsers = userRepository.findByEmailInOrNationalIdIn(
                email != null ? List.of(email) : Collections.emptyList(),
                nationalId != null ? List.of(nationalId) : Collections.emptyList()
        );

        if (existingUsers.isEmpty()) {
            return;
        }

        boolean emailExists = existingUsers.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));

        boolean nationalIdExists = existingUsers.stream()
                .anyMatch(u -> nationalId.equalsIgnoreCase(u.getNationalId()));

        if (emailExists && nationalIdExists) {
            throw new BadRequestException(
                    ErrorCode.DUPLICATE_EMAIL_AND_NATIONAL_ID,
                    Map.of(
                            "email", email,
                            "nationalId", nationalId
                    )
            );
        } else if (emailExists) {
            throw new DuplicateResourceException("User", "email", email);
        } else if (nationalIdExists) {
            throw new DuplicateResourceException("User", "nationalId", nationalId);
        }
    }

    private void validateNoActiveProjects(Long userId) {
        long activeProjects = projectRepository.countByCreatorIdAndStatus(
                userId, ProjectStatus.PUBLISHED
        );

        if (activeProjects > 0) {
            throw new BadRequestException(
                    ErrorCode.CANNOT_DEACTIVATE_SELF,
                    Map.of(
                            "activeProjects", activeProjects
                    )
            );
        }
    }

    private int reassignInactiveProjects(Long userId) {
        List<Project> inactiveProjects = projectRepository
                .findByCreatorIdAndStatusNot(userId, ProjectStatus.PUBLISHED);

        if (inactiveProjects.isEmpty()) {
            return 0;
        }

        User systemUser = userRepository.findById(SYSTEM_USER_ID)
                .orElseThrow(() -> new BadRequestException(
                        ErrorCode.SYSTEM_USER_NOT_FOUND,
                        Map.of("systemUserId", SYSTEM_USER_ID)
                ));

        log.info("Reasignando {} proyectos inactivos al usuario sistema",
                inactiveProjects.size());

        inactiveProjects.forEach(project -> project.setCreator(systemUser));
        projectRepository.saveAll(inactiveProjects);

        return inactiveProjects.size();
    }

    private User buildUserFromDto(UserSaveDto dto) {
        User user = userMapper.toUserSaveDtoToEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.password()));
        return user;
    }

    private void updateUserFields(User user, UserSaveDto updateDto) {
        user.setName(updateDto.name());
        user.setEmail(updateDto.email());
        user.setNationalId(updateDto.nationalId());
        user.setPhone(updateDto.phone());

        if (updateDto.active() != null) {
            user.setActive(updateDto.active());
        }

        if (updateDto.roles() != null && !updateDto.roles().isEmpty()) {
            String normalizedRole = normalizeAndValidateRole(updateDto.roles().get(0));
            Role role = findRoleByNameOrThrow(normalizedRole);
            user.getRoles().clear();
            user.getRoles().add(role);
        }
    }

    private List<Role> fetchAndValidateRoles(List<String> roleNames) {
        List<Role> roles = roleNames.stream()
                .map(roleRepository::findByNameContainingIgnoreCase)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (roles.isEmpty()) {
            throw new BadRequestException(ErrorCode.ROLE_NOT_FOUND);
        }

        return roles;
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        int validPage = Math.max(page, MIN_PAGE_NUMBER);
        int validSize = Math.max(Math.min(size, MAX_PAGE_SIZE), DEFAULT_PAGE_SIZE);

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        String sortField = (sortBy != null && !sortBy.isBlank())
                ? sortBy
                : DEFAULT_SORT_FIELD;

        return PageRequest.of(validPage, validSize, Sort.by(direction, sortField));
    }

    private Statistics<UserDto> calculateUserStatistics(Long currentUserId, String name,
                                                        String role, Boolean active) {
        long total, activeCount, inactiveCount;

        if (name != null && !name.isBlank() && role != null && !role.isBlank()) {
            total = userRepository.countByNameAndRoleAndCurrentUser(name, role, currentUserId);
            activeCount = userRepository.countByNameAndRoleAndActiveAndCurrentUser(
                    name, role, true, currentUserId
            );
            inactiveCount = userRepository.countByNameAndRoleAndActiveAndCurrentUser(
                    name, role, false, currentUserId
            );
        } else if (name != null && !name.isBlank()) {
            total = userRepository.countByNameAndCurrentUser(name, currentUserId);
            activeCount = userRepository.countByNameAndActiveAndCurrentUser(
                    name, true, currentUserId
            );
            inactiveCount = userRepository.countByNameAndActiveAndCurrentUser(
                    name, false, currentUserId
            );
        } else if (role != null && !role.isBlank()) {
            total = userRepository.countByRoleAndCurrentUser(role, currentUserId);
            activeCount = userRepository.countByRoleAndActiveAndCurrentUser(
                    role, true, currentUserId
            );
            inactiveCount = userRepository.countByRoleAndActiveAndCurrentUser(
                    role, false, currentUserId
            );
        } else {
            total = currentUserId != null
                    ? userRepository.countExcludingUser(currentUserId)
                    : userRepository.count();
            activeCount = currentUserId != null
                    ? userRepository.countByActiveExcludingUser(true, currentUserId)
                    : userRepository.countByActive(true);
            inactiveCount = total - activeCount;
        }

        return new Statistics<>(total, Map.of(
                "active", activeCount,
                "inactive", inactiveCount
        ));
    }

    private UserDto enrichWithLastAction(User user) {
        UserDto dto = userMapper.toDto(user);
        OffsetDateTime lastAction = actionService.getLastActionDateByUserId(user.getId());

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

    private ValidationResult validateInternalDuplicates(List<UserSaveDto> users) {
        List<UserImportError> errors = new ArrayList<>();
        Set<Integer> validRows = new HashSet<>();
        Map<String, Integer> emailMap = new HashMap<>();
        Map<String, Integer> nationalIdMap = new HashMap<>();

        for (int i = 0; i < users.size(); i++) {
            UserSaveDto user = users.get(i);
            int rowNumber = i + 2;
            String role = getRoleName(user);

            List<String> fieldErrors = validateUserFields(user);
            if (!fieldErrors.isEmpty()) {
                errors.add(new UserImportError(
                        rowNumber, user.email(), user.nationalId(), role,
                        String.join("; ", fieldErrors)
                ));
                continue;
            }

            if (emailMap.containsKey(user.email())) {
                errors.add(new UserImportError(
                        rowNumber, user.email(), user.nationalId(), role,
                        ErrorCode.CSV_DUPLICATE_EMAIL.getCode() + ": row " + emailMap.get(user.email())
                ));
                continue;
            }

            if (nationalIdMap.containsKey(user.nationalId())) {
                errors.add(new UserImportError(
                        rowNumber, user.email(), user.nationalId(), role,
                        ErrorCode.CSV_DUPLICATE_NATIONAL_ID.getCode() + ": row " + nationalIdMap.get(user.nationalId())
                ));
                continue;
            }

            emailMap.put(user.email(), rowNumber);
            nationalIdMap.put(user.nationalId(), rowNumber);
            validRows.add(i);
        }

        return new ValidationResult(validRows, errors);
    }

    private ValidationResult validateAgainstDatabase(List<UserSaveDto> users, Set<Integer> validRows) {
        List<UserImportError> errors = new ArrayList<>();

        Set<String> emails = validRows.stream()
                .map(i -> users.get(i).email())
                .collect(Collectors.toSet());

        Set<String> nationalIds = validRows.stream()
                .map(i -> users.get(i).nationalId())
                .collect(Collectors.toSet());

        List<User> existingUsers = userRepository.findByEmailInOrNationalIdIn(
                new ArrayList<>(emails),
                new ArrayList<>(nationalIds)
        );

        Map<String, User> existingEmailMap = existingUsers.stream()
                .collect(Collectors.toMap(
                        User::getEmail,
                        u -> u,
                        (u1, u2) -> u1
                ));

        Map<String, User> existingNationalIdMap = existingUsers.stream()
                .collect(Collectors.toMap(
                        User::getNationalId,
                        u -> u,
                        (u1, u2) -> u1
                ));

        Set<Integer> stillValid = new HashSet<>();

        for (int i : validRows) {
            UserSaveDto user = users.get(i);
            int rowNumber = i + 2;
            String role = getRoleName(user);

            if (existingEmailMap.containsKey(user.email())) {
                errors.add(new UserImportError(
                        rowNumber, user.email(), user.nationalId(), role,
                        ErrorCode.CSV_EMAIL_EXISTS.getCode()
                ));
                continue;
            }

            if (existingNationalIdMap.containsKey(user.nationalId())) {
                errors.add(new UserImportError(
                        rowNumber, user.email(), user.nationalId(), role,
                        ErrorCode.CSV_NATIONAL_ID_EXISTS.getCode()
                ));
                continue;
            }

            stillValid.add(i);
        }

        return new ValidationResult(stillValid, errors);
    }

    private List<User> prepareUsersForSave(List<UserSaveDto> users, Set<Integer> validRows) {
        Map<String, Role> roleCache = cacheAllRoles();
        List<User> usersToSave = new ArrayList<>();

        for (int i : validRows) {
            UserSaveDto userDto = users.get(i);
            String roleName = getRoleName(userDto).toUpperCase();

            Role role = roleCache.get(roleName);
            if (role == null) {
                log.warn("Rol '{}' no encontrado para usuario en fila {}, usando rol por defecto",
                        roleName, i + 2);
                role = roleCache.get(DEFAULT_ROLE);
            }

            User user = userMapper.toUserSaveDtoToEntity(userDto);
            user.setPassword(passwordEncoder.encode(userDto.password()));
            user.setActive(true);
            user.setRoles(List.of(role));

            usersToSave.add(user);
        }

        return usersToSave;
    }

    private List<UserDto> saveUsersInBatch(List<User> users) {
        if (users.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<User> savedUsers = userRepository.saveAll(users);
            log.info("Se guardaron exitosamente {} usuarios en batch", savedUsers.size());

            return savedUsers.stream()
                    .map(userMapper::toDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error guardando usuarios en batch: {}", e.getMessage(), e);
            throw new BadRequestException(ErrorCode.CSV_SAVE_ERROR);
        }
    }

    private List<String> validateUserFields(UserSaveDto user) {
        List<String> errors = new ArrayList<>();

        if (user.name() == null || user.name().isBlank()) {
            errors.add(ErrorCode.CSV_FIELD_NAME_REQUIRED.getCode());
        }

        if (user.email() == null || user.email().isBlank()) {
            errors.add(ErrorCode.CSV_FIELD_EMAIL_REQUIRED.getCode());
        } else if (!user.email().matches(EMAIL_REGEX)) {
            errors.add(ErrorCode.CSV_FIELD_EMAIL_INVALID.getCode());
        }

        if (user.nationalId() == null || user.nationalId().isBlank()) {
            errors.add(ErrorCode.CSV_FIELD_NATIONAL_ID_REQUIRED.getCode());
        }

        if (user.phone() == null || user.phone().isBlank()) {
            errors.add(ErrorCode.CSV_FIELD_PHONE_REQUIRED.getCode());
        } else if (!user.phone().matches(PHONE_REGEX)) {
            errors.add(ErrorCode.CSV_FIELD_PHONE_INVALID.getCode());
        }

        if (user.password() == null || user.password().isBlank()) {
            errors.add(ErrorCode.CSV_FIELD_PASSWORD_REQUIRED.getCode());
        } else if (user.password().length() < MIN_PASSWORD_LENGTH) {
            errors.add(ErrorCode.CSV_FIELD_PASSWORD_TOO_SHORT.getCode() + ": min " + MIN_PASSWORD_LENGTH);
        }

        return errors;
    }

    private Map<String, Role> cacheAllRoles() {
        Map<String, Role> roleMap = new HashMap<>();
        for (String roleName : ALLOWED_ROLE_NAMES) {
            Role role = roleRepository.findByNameContainingIgnoreCase(roleName);
            if (role != null) {
                roleMap.put(roleName, role);
            }
        }
        return roleMap;
    }

    private String getRoleName(UserSaveDto user) {
        return (user.roles() != null && !user.roles().isEmpty())
                ? user.roles().get(0)
                : DEFAULT_ROLE;
    }


    private void validateCSVFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(ErrorCode.FILE_EMPTY);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new BadRequestException(
                    ErrorCode.FILE_INVALID_EXTENSION,
                    Map.of("expectedExtension", ".csv")
            );
        }

        if (file.getSize() > MAX_CSV_SIZE_BYTES) {
            throw new BadRequestException(
                    ErrorCode.CSV_FILE_TOO_LARGE,
                    Map.of("maxSizeMB", String.format("%.2f", MAX_CSV_SIZE_BYTES / (1024.0 * 1024.0)))
            );
        }

        log.debug("Archivo CSV validado: {} ({} bytes)", filename, file.getSize());
    }

    private List<UserSaveDto> parseCSVFile(MultipartFile file) throws IOException {
        List<UserSaveDto> users = new ArrayList<>();

        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            List<String[]> records = reader.readAll();

            if (records.isEmpty()) {
                throw new BadRequestException(ErrorCode.CSV_EMPTY);
            }

            String[] headers = records.get(0);
            if (looksLikeDataRow(headers)) {
                throw new BadRequestException(
                        ErrorCode.CSV_INVALID_HEADERS,
                        Map.of("expectedHeaders", String.join(", ", EXPECTED_CSV_HEADERS))
                );
            }

            validateCSVHeaders(headers);
            log.info("Headers CSV validados correctamente");

            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);

                if (isEmptyRow(record)) {
                    log.debug("Saltando fila {} (vacía)", i + 1);
                    continue;
                }

                if (record.length < EXPECTED_CSV_HEADERS.length) {
                    log.warn("Fila {}: tiene solo {} columnas, se esperan {}",
                            i + 1, record.length, EXPECTED_CSV_HEADERS.length);
                    continue;
                }

                try {
                    UserSaveDto userDto = parseUserFromCSVRow(record);
                    users.add(userDto);
                } catch (BadRequestException e) {
                    log.warn("Error parseando fila {}: {}", i + 1, e.getMessage());
                }
            }

        } catch (CsvException e) {
            throw new BadRequestException(ErrorCode.CSV_READ_ERROR);
        }

        return users;
    }

    private boolean looksLikeDataRow(String[] row) {
        if (row == null || row.length < 2) {
            return false;
        }
        String secondColumn = cleanCSVField(row[1]).toLowerCase();
        return secondColumn.matches(EMAIL_REGEX);
    }

    private void validateCSVHeaders(String[] headerRow) {
        if (headerRow == null || headerRow.length < EXPECTED_CSV_HEADERS.length) {
            throw new BadRequestException(
                    ErrorCode.CSV_INSUFFICIENT_COLUMNS,
                    Map.of(
                            "requiredColumns", EXPECTED_CSV_HEADERS.length,
                            "providedColumns", headerRow != null ? headerRow.length : 0,
                            "expectedHeaders", String.join(", ", EXPECTED_CSV_HEADERS)
                    )
            );
        }

        List<Map<String, Object>> columnErrors = new ArrayList<>();

        for (int i = 0; i < EXPECTED_CSV_HEADERS.length; i++) {
            String expected = EXPECTED_CSV_HEADERS[i].toLowerCase();
            String actual = cleanCSVField(headerRow[i]).toLowerCase();

            if (!actual.equals(expected)) {
                columnErrors.add(Map.of(
                        "columnNumber", i + 1,
                        "expected", expected,
                        "actual", actual
                ));
            }
        }

        if (!columnErrors.isEmpty()) {
            throw new BadRequestException(
                    ErrorCode.CSV_COLUMN_MISMATCH,
                    Map.of(
                            "columnErrors", columnErrors,
                            "expectedHeaders", String.join(", ", EXPECTED_CSV_HEADERS)
                    )
            );
        }
    }

    private boolean isEmptyRow(String[] record) {
        if (record == null || record.length == 0) {
            return true;
        }
        return Arrays.stream(record).allMatch(field -> field == null || field.trim().isEmpty());
    }

    private UserSaveDto parseUserFromCSVRow(String[] record) {
        if (record.length < EXPECTED_CSV_HEADERS.length) {
            throw new BadRequestException(
                    ErrorCode.CSV_INVALID_FORMAT,
                    Map.of(
                            "requiredColumns", EXPECTED_CSV_HEADERS.length,
                            "providedColumns", record.length,
                            "expectedHeaders", String.join(", ", EXPECTED_CSV_HEADERS)
                    )
            );
        }

        String name = cleanCSVField(record[0]);
        String email = cleanCSVField(record[1]);
        String nationalId = cleanCSVField(record[2]);
        String phone = cleanCSVField(record[3]);
        String password = cleanCSVField(record[4]);
        String role = cleanCSVField(record[5]).toUpperCase();

        if (role.isBlank()) {
            role = DEFAULT_ROLE;
        }

        return new UserSaveDto(name, email, nationalId, phone, password, true, List.of(role));
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

    private String buildCSVRow(UserDto user) {
        String role = (user.roles() != null && !user.roles().isEmpty())
                ? user.roles().get(0)
                : DEFAULT_ROLE;

        String createdAt = (user.createdAt() != null)
                ? user.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : "";

        return String.format("%s,%s,%s,%s,%s,%s,%s",
                escapeCSV(user.name()),
                escapeCSV(user.email()),
                escapeCSV(user.nationalId()),
                escapeCSV(user.phone()),
                role,
                user.active(),
                createdAt
        );
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }

        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    private Long getCurrentCuratorId(Project project) {
        return project.getReviews().stream()
                .max(Comparator.comparing(Review::getStartAt))
                .map(Review::getCurator)
                .filter(Objects::nonNull)
                .map(User::getId)
                .orElse(null);
    }

    private Map<Long, Map<String, Object>> getCuratorStatsMap(List<User> curators) {
        List<Long> curatorIds = curators.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        return reviewRepository.getCuratorStatsByIds(curatorIds)
                .stream()
                .collect(Collectors.toMap(
                        map -> (Long) map.get("curatorId"),
                        map -> map
                ));
    }

    private CuratorInfoDto buildCuratorInfo(List<User> allCurators, Long currentCuratorId,
                                            Map<Long, Map<String, Object>> statsMap) {
        CuratorDto currentCuratorDto = null;
        List<CuratorDto> otherCurators = new ArrayList<>();

        for (User curator : allCurators) {
            CuratorDto dto = buildCuratorDto(curator, statsMap);

            if (curator.getId().equals(currentCuratorId)) {
                currentCuratorDto = dto;
            } else {
                otherCurators.add(dto);
            }
        }

        return new CuratorInfoDto(currentCuratorDto, otherCurators);
    }

    private CuratorDto buildCuratorDto(User curator, Map<Long, Map<String, Object>> statsMap) {
        Map<String, Object> stats = statsMap.getOrDefault(
                curator.getId(),
                Map.of("active", 0L, "completed", 0L, "total", 0L)
        );

        return new CuratorDto(
                userMapper.toDto(curator),
                (Long) stats.get("active"),
                (Long) stats.get("completed"),
                (Long) stats.get("total")
        );
    }



    private String getCurrentRoleName(User user) {
        return user.getRoles().isEmpty()
                ? "ninguno"
                : user.getRoles().get(0).getName();
    }





    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private Project findProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
    }

    private Review getOrCreateReview(List<Review> reviews, Project project, User curator) {
        if (reviews.isEmpty()) {
            return Review.builder()
                    .project(project)
                    .curator(curator)
                    .dueAt(OffsetDateTime.now().plusDays(7))
                    .build();
        }
        return reviews.get(0);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void validateDateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private record ValidationResult(Set<Integer> validRows, List<UserImportError> errors) {}
}