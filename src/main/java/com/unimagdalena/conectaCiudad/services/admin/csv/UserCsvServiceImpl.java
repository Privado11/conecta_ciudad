package com.unimagdalena.conectaCiudad.services.admin.csv;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.unimagdalena.conectaCiudad.Dto.user.BulkUserImportResult;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.enums.ErrorCode;
import com.unimagdalena.conectaCiudad.enums.UserActionType;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.services.admin.user.UserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserCsvServiceImpl implements UserCsvService {

    private static final String DEFAULT_ROLE = "CITIZEN";
    private static final String[] EXPECTED_CSV_HEADERS = {
            "name", "email", "nationalId", "phone", "password", "role"
    };
    private static final long MAX_CSV_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    private final UserManagementService userManagementService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

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

            return userManagementService.saveBulkUsers(users);

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
        List<UserDto> allUsers = userManagementService.findAll();
        return exportUsersToCSV(allUsers);
    }


    private User getCurrentUser() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String nationalId = authentication.getName();
        return userRepository.findByNationalId(nationalId);
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
}
