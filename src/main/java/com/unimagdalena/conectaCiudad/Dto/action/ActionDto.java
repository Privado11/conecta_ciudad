package com.unimagdalena.conectaCiudad.Dto.action;

import java.time.LocalDateTime;

import com.unimagdalena.conectaCiudad.Dto.access.AccessDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;

import lombok.Builder;

@Builder
public record ActionDto(
    Long id,
    String name,
    String description,
    LocalDateTime actionAt,
    AccessDto access
) {}
