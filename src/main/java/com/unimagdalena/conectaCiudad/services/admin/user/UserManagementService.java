package com.unimagdalena.conectaCiudad.services.admin.user;

import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.user.BulkUserImportResult;
import com.unimagdalena.conectaCiudad.Dto.user.CuratorInfoDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;

import java.util.List;


public interface UserManagementService {

    UserDto createUser(UserSaveDto userDto);

    UserDto updateUser(Long userId, UserSaveDto updateDto);

    void deleteUser(Long userId);

    UserDto toggleUserStatus(Long userId);


    UserDto assignRole(Long userId, String roleName);


    UserDto removeRole(Long userId, String roleName);


    PagedResponse<UserDto> findAllExceptCurrent(Long currentUserId, int page, int size,
                                                 String sortBy, String sortDirection);

    PagedResponse<UserDto> findByFilters(String roleName, Boolean active, Long currentUserId,
                                          int page, int size, String sortBy, String sortDirection);

    PagedResponse<UserDto> findByNameAndFilters(String name, String roleName, Boolean active,
                                                 Long currentUserId, int page, int size,
                                                 String sortBy, String sortDirection);

    CuratorInfoDto findAllCuratorsWithStats(Long projectId);


    List<UserDto> findAll();


    BulkUserImportResult saveBulkUsers(List<UserSaveDto> users);
}
