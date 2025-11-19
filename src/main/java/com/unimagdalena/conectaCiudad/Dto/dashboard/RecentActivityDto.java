package com.unimagdalena.conectaCiudad.Dto.dashboard;

public record RecentActivityDto(
    Long id,
    String user,
    String action,
    String timestamp,
    String status
) {}
