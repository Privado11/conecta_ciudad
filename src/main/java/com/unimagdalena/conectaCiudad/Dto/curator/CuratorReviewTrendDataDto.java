package com.unimagdalena.conectaCiudad.Dto.curator;

public record CuratorReviewTrendDataDto(
    String month,
    Long reviewed,
    Long approved,
    Long returned
) {}
