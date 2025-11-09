package com.unimagdalena.conectaCiudad.services.user;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import com.unimagdalena.conectaCiudad.Dto.user.BulkUserImportResult;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;

public interface UserService {
    UserDto findByEmail(String email);
    UserDto findByNationalId(String nationalId);
    List<UserDto> findByNameContainingIgnoreCase(String name);
    UserDto saveUser(UserSaveDto user);
    UserDto saveUserDefault(UserSaveDto user);
    UserDto findById(Long id);
    UserDto findByEmailOrNationalId(String email, String nationalId);
    List<UserDto> findAll();
    UserDto updateUser(Long id, UserSaveDto user);
    void deleteUser(Long id);
    UserDto addRole(Long userId, String roleName);
    UserDto removeRole(Long userId, String roleName);
    UserDto toggleUserStatus(Long userId);
    Page<UserDto> findAllExceptCurrent(Long currentUserId, int page, int size, String sortBy, String sortDirection);
    Page<UserDto> findByNameWithPagination(String name, Long currentUserId, int page, int size, String sortBy, String sortDirection);
    Page<UserDto> findByFilters(String roleName, Boolean active, Long currentUserId, 
                                 int page, int size, String sortBy, String sortDirection);
    Page<UserDto> findByNameAndFilters(String name, String roleName, Boolean active, 
                                        Long currentUserId, int page, int size, 
                                        String sortBy, String sortDirection);
    void validateUniqueFields(String email, String nationalId);
     BulkUserImportResult importUsersFromCSV(MultipartFile file) throws IOException;
    
    BulkUserImportResult saveBulkUsers(List<UserSaveDto> users);
    byte[] exportUsersToCSV(List<UserDto> users) throws IOException;
    byte[] exportAllUsersToCSV() throws IOException;
    
}


