package com.unimagdalena.conectaCiudad.Dto.user;

import java.util.List;

import com.unimagdalena.conectaCiudad.validation.OnCreate;
import com.unimagdalena.conectaCiudad.validation.OnUpdate;
import com.unimagdalena.conectaCiudad.validation.OnUpdatedUSerForAdmin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserSaveDto(

    @NotBlank(groups = {OnCreate.class, OnUpdate.class})
    String name,

    @NotBlank(groups = {OnCreate.class, OnUpdate.class})
    @Pattern(groups = {OnCreate.class, OnUpdate.class}, regexp = "^[0-9]+$")
    String nationalId,

    @NotBlank(groups = {OnCreate.class, OnUpdate.class})
    @Email(groups = {OnCreate.class, OnUpdate.class})
    String email,

    @NotBlank(groups = OnCreate.class)
    @Size(groups = OnCreate.class, min = 6)
    String password,

    @NotBlank(groups = {OnCreate.class, OnUpdate.class})
    @Pattern(groups = {OnCreate.class, OnUpdate.class}, regexp = "^[0-9]+$")
    String phone,

    @NotNull(groups = {OnUpdatedUSerForAdmin.class})
    Boolean active,

    @NotNull(groups = {OnUpdatedUSerForAdmin.class})
    List<String> roles
) {}
