package com.unimagdalena.conectaCiudad.services.admin;

import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.page.Statistics;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.user.BulkUserImportResult;
import com.unimagdalena.conectaCiudad.Dto.user.CuratorInfoDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.Dto.voting.UserVotingStatsDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingProjectDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingStatsDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.services.admin.csv.UserCsvService;
import com.unimagdalena.conectaCiudad.services.admin.project.ProjectAdminService;
import com.unimagdalena.conectaCiudad.services.admin.statistics.StatisticsService;
import com.unimagdalena.conectaCiudad.services.admin.user.UserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserManagementService userManagementService;
    private final UserCsvService userCsvService;
    private final ProjectAdminService projectAdminService;
    private final StatisticsService statisticsService;
    private final com.unimagdalena.conectaCiudad.services.admin.voting.VotingProjectService votingProjectService;
    private final com.unimagdalena.conectaCiudad.services.admin.voting.VotingStatisticsService votingStatisticsService;


    @Override
    @Transactional
    public UserDto createUser(UserSaveDto user) {
        return userManagementService.createUser(user);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, UserSaveDto user) {
        return userManagementService.updateUser(id, user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userManagementService.deleteUser(id);
    }

    @Override
    @Transactional
    public UserDto toggleUserStatus(Long userId) {
        return userManagementService.toggleUserStatus(userId);
    }

    @Override
    @Transactional
    public UserDto assignRole(Long userId, String roleName) {
        return userManagementService.assignRole(userId, roleName);
    }

    @Override
    @Transactional
    public UserDto removeRole(Long userId, String roleName) {
        return userManagementService.removeRole(userId, roleName);
    }

    @Override
    public PagedResponse<UserDto> findAllExceptCurrent(Long currentUserId, int page, int size,
                                                       String sortBy, String sortDirection) {
        return userManagementService.findAllExceptCurrent(currentUserId, page, size, sortBy, sortDirection);
    }

    @Override
    public PagedResponse<UserDto> findByFilters(String roleName, Boolean active, Long currentUserId,
                                                int page, int size, String sortBy, String sortDirection) {
        return userManagementService.findByFilters(roleName, active, currentUserId, page, size, sortBy, sortDirection);
    }

    @Override
    public PagedResponse<UserDto> findByNameAndFilters(String name, String roleName, Boolean active,
                                                       Long currentUserId, int page, int size,
                                                       String sortBy, String sortDirection) {
        return userManagementService.findByNameAndFilters(name, roleName, active, currentUserId, page, size, sortBy, sortDirection);
    }

    @Override
    public CuratorInfoDto findAllCuratorsWithStats(Long projectId) {
        return userManagementService.findAllCuratorsWithStats(projectId);
    }

    @Override
    public List<UserDto> findAll() {
        return userManagementService.findAll();
    }

    @Override
    @Transactional
    public BulkUserImportResult saveBulkUsers(List<UserSaveDto> users) {
        return userManagementService.saveBulkUsers(users);
    }


    @Override
    @Transactional
    public BulkUserImportResult importUsersFromCSV(MultipartFile file) throws IOException {
        return userCsvService.importUsersFromCSV(file);
    }

    @Override
    public byte[] exportUsersToCSV(List<UserDto> users) throws IOException {
        return userCsvService.exportUsersToCSV(users);
    }

    @Override
    public byte[] exportAllUsersToCSV() throws IOException {
        return userCsvService.exportAllUsersToCSV();
    }


    @Override
    public ProjectDto reassignCurator(Long projectId, Long curatorId, Long adminId, Long accessId) {
        return projectAdminService.reassignCurator(projectId, curatorId, adminId, accessId);
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
        return projectAdminService.findWithFilters(
                searchTerm, status, creatorId, curatorId,
                projectStartFrom, projectStartTo,
                projectEndFrom, projectEndTo,
                votingStartFrom, votingStartTo,
                votingEndFrom, votingEndTo,
                createdFrom, createdTo,
                pageable
        );
    }


    @Override
    public Statistics<ProjectDto> getGlobalStatistics() {
        return statisticsService.getGlobalStatistics();
    }

    @Override
    public List<VotingProjectDto> getAllVotingProjects(String token) {
        return votingProjectService.getAllVotingProjects(token);
    }

    @Override
    public VotingStatsDto getVotingStatistics(String token) {
        return votingStatisticsService.getVotingStatistics(token);
    }

    @Override
    public List<VotingProjectDto> getOpenVotingProjects(String token) {
        return votingProjectService.getOpenVotingProjects(token);
    }

    @Override
    public List<VotingProjectDto> getClosedVotingProjects(String token) {
        return votingProjectService.getClosedVotingProjects(token);
    }

    @Override
    public UserVotingStatsDto getUserVotingStats(String token) {
        return votingStatisticsService.getUserVotingStats(token);
    }
}