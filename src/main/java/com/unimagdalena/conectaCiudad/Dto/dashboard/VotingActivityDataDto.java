package com.unimagdalena.conectaCiudad.Dto.dashboard;

public record VotingActivityDataDto(
    String projectName,
    Long votes,
    String endDate
) {}
