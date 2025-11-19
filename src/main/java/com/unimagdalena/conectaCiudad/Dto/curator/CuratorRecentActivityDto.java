package com.unimagdalena.conectaCiudad.Dto.curator;

public record CuratorRecentActivityDto(
    Long id,
    String projectName,
    String action,
    String timestamp,
    String outcome
) {}
