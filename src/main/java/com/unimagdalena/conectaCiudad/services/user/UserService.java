package com.unimagdalena.conectaCiudad.services.user;

import java.util.List;

import com.unimagdalena.conectaCiudad.Dto.user.*;


public interface UserService {
    UserDto findByEmail(String email);
    UserDto findByNationalId(String nationalId);
    UserDto registerUser(UserSaveDto user);
    UserDto findById(Long id);
    UserDto findByEmailOrNationalId(String email, String nationalId);
    UserDto updateOwnProfile(Long id, UserSaveDto user);
    UserDto changePassword(Long userId, String oldPassword, String newPassword);
    void validateUniqueFields(String email, String nationalId);
}


