package com.unimagdalena.conectaCiudad.Dto.voting;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VotingResultsDto(

        @JsonProperty("idProyecto")
        Long projectId,

        @JsonProperty("votosAFavor")
        Long votesInFavor,

        @JsonProperty("votosEnContra")
        Long votesAgainst,

        @JsonProperty("porcentajeAFavor")
        Double percentageInFavor,

        @JsonProperty("porcentajeEnContra")
        Double percentageAgainst
) {

    public record OptionResultDto(
            @JsonProperty("idOpcion")
            Long optionId,

            @JsonProperty("nombreOpcion")
            String optionName,

            @JsonProperty("cantidadVotos")
            Long voteCount,

            @JsonProperty("porcentaje")
            Double percentage
    ) {}
}
