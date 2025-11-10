package com.unimagdalena.conectaCiudad.Dto.page;

import org.springframework.data.domain.Page;

public record PagedResponse<T>(
    Page<T> page,
    Statistics<T> statistics
) {}
