package com.unimagdalena.conectaCiudad.Dto.user;

import java.util.List;

public record CuratorInfoDto(
        CuratorDto currentCurator,
        List<CuratorDto> activeCurators
) {}
