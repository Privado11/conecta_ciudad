package com.unimagdalena.conectaCiudad.services.admin;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.user.BulkUserImportResult;
import com.unimagdalena.conectaCiudad.Dto.user.CuratorInfoDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface AdminService {

    //Users
    UserDto createUser(UserSaveDto user);
    UserDto updateUser(Long id, UserSaveDto user);
    void deleteUser(Long id);
    UserDto toggleUserStatus(Long userId);
    UserDto assignRole(Long userId, String roleName);
    UserDto removeRole(Long userId, String roleName);
    PagedResponse<UserDto> findAllExceptCurrent(Long currentUserId, int page, int size, String sortBy, String sortDirection);
    PagedResponse<UserDto> findByFilters(String roleName, Boolean active, Long currentUserId, 
                                 int page, int size, String sortBy, String sortDirection);
    PagedResponse<UserDto> findByNameAndFilters(String name, String roleName, Boolean active, 
                                        Long currentUserId, int page, int size, 
                                        String sortBy, String sortDirection);
    BulkUserImportResult importUsersFromCSV(MultipartFile file) throws IOException;
    BulkUserImportResult saveBulkUsers(List<UserSaveDto> users);
    byte[] exportUsersToCSV(List<UserDto> users) throws IOException;
    byte[] exportAllUsersToCSV() throws IOException;
    CuratorInfoDto findAllCuratorsWithStats(Long projectId);
    List<UserDto> findAll();

    //Projects
    ProjectDto reassignCurator(Long projectId, Long curatorId, Long adminId, Long accessId);
    Page<ProjectDto> findWithFilters(
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
    );
}
