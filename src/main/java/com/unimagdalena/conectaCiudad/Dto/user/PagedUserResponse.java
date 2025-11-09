package com.unimagdalena.conectaCiudad.Dto.user;

import org.springframework.data.domain.Page;

public record PagedUserResponse(
    Page<UserDto> page,
    UserStatistics statistics
) {}