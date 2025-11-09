package com.unimagdalena.conectaCiudad.Dto.user;

import java.util.List;

import com.unimagdalena.conectaCiudad.validation.OnCreate;
import com.unimagdalena.conectaCiudad.validation.OnUpdate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserSaveDto(

    @NotBlank(groups = {OnCreate.class, OnUpdate.class}, message = "El nombre es obligatorio.")
    String name,

    @NotBlank(groups = {OnCreate.class, OnUpdate.class}, message = "El número de documento es obligatorio.")
    @Pattern(groups = {OnCreate.class, OnUpdate.class}, regexp = "^[0-9]+$", message = "El número de documento solo debe contener números.")
    String nationalId,

    @NotBlank(groups = {OnCreate.class, OnUpdate.class}, message = "El correo electrónico es obligatorio.")
    @Email(groups = {OnCreate.class, OnUpdate.class}, message = "El formato del correo electrónico no es válido.")
    String email,

    @NotBlank(groups = OnCreate.class, message = "La contraseña es obligatoria.")
    @Size(groups = OnCreate.class, min = 6, message = "La contraseña debe tener al menos 6 caracteres.")
    String password,

    @NotBlank(groups = {OnCreate.class, OnUpdate.class}, message = "El número de teléfono es obligatorio.")
    @Pattern(groups = {OnCreate.class, OnUpdate.class}, regexp = "^[0-9]+$", message = "El número de teléfono solo debe contener números.")
    String phone,

    List<String> roles
) {}
