package com.unimagdalena.conectaCiudad.services.user;

import java.util.List;


import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;

public interface UserService {
    UserDto findByEmail(String email);
    UserDto findByNationalId(String nationalId);
    List<UserDto> findByNameContainingIgnoreCase(String name);
    UserDto saveUser(UserSaveDto user);
    UserDto findById(Long id);
    UserDto findByEmailOrNationalId(String email, String nationalId);
    List<UserDto> findAll();
    UserDto updateUser(Long id, UserSaveDto user);
    void deleteUser(Long id);
    UserDto addRole(Long userId, String roleName);
    UserDto removeRole(Long userId, String roleName);
    UserDto createUserWithRoles(UserSaveDto user, java.util.List<String> roles);

}


