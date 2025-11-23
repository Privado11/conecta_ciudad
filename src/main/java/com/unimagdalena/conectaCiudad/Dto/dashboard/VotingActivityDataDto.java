package com.unimagdalena.conectaCiudad.Dto.dashboard;

public record VotingActivityDataDto(
        Long id,
    String projectName,
    Long votes,
    String endDate
) {}
