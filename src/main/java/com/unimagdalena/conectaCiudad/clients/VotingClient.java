package com.unimagdalena.conectaCiudad.clients;

import com.unimagdalena.conectaCiudad.Dto.voting.VoteDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingResultsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VotingClient {

    private final RestTemplate restTemplate;
    private static final String VOTING_BASE_URL =
            "https://participacion-ciudadana-aze8f3ezf0ene3g2.eastus2-01.azurewebsites.net";

    public List<VoteDto> getUserVotes(Long projectId, Long citizenId, String token) {
        try {
            String url = String.format("%s/votaciones/%d/mis-votos",
                    VOTING_BASE_URL, projectId);

            HttpHeaders headers = new HttpHeaders();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            } else {
                log.warn("NO se envía token");
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<VoteDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    VoteDto.class
            );

            VoteDto vote = response.getBody();
            return vote != null ? List.of(vote) : List.of();

        } catch (HttpClientErrorException.NotFound e) {
            return List.of();
        } catch (HttpClientErrorException e) {
            return List.of();
        } catch (RestClientException e) {
            return List.of();
        } catch (Exception e) {
            log.error("Error inesperado: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public boolean hasVoted(Long projectId, Long citizenId, String token) {
        List<VoteDto> votes = getUserVotes(projectId, citizenId, token);
        return !votes.isEmpty();
    }

    public long getProjectVotesCount(Long projectId, String token) {
        try {
            String url = String.format("%s/votaciones/%d/resultados",
                    VOTING_BASE_URL, projectId);

            log.debug("Consultando resultados de votación para proyecto {}", projectId);

            HttpHeaders headers = new HttpHeaders();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
                log.debug("Token enviado para obtener resultados");
            } else {
                log.warn("No se envía token para obtener resultados del proyecto {}", projectId);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<VotingResultsDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    VotingResultsDto.class
            );

            VotingResultsDto results = response.getBody();

            if (results != null) {
                long totalVotes = results.votesAgainst() + results.votesInFavor();
                log.debug("Proyecto {} tiene {} votos", projectId, totalVotes);
                return totalVotes;
            }

            return 0L;

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("No se encontraron resultados para proyecto {}", projectId);
            return 0L;
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP al obtener resultados del proyecto {}: {}",
                    projectId, e.getMessage());
            return 0L;
        } catch (RestClientException e) {
            log.error("Error de conexión al obtener resultados del proyecto {}: {}",
                    projectId, e.getMessage());
            return 0L;
        } catch (Exception e) {
            log.error("Error inesperado al obtener resultados del proyecto {}: {}",
                    projectId, e.getMessage(), e);
            return 0L;
        }
    }

    public VotingResultsDto getProjectVotingResults(Long projectId, String token) {
        try {
            String url = String.format("%s/votaciones/%d/resultados",
                    VOTING_BASE_URL, projectId);

            log.debug("Consultando resultados completos de votación para proyecto {}", projectId);

            HttpHeaders headers = new HttpHeaders();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
                log.debug("Token enviado para obtener resultados completos");
            } else {
                log.warn("No se envía token para obtener resultados del proyecto {}", projectId);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<VotingResultsDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    VotingResultsDto.class
            );

            VotingResultsDto results = response.getBody();

            if (results != null) {
                log.debug("Proyecto {}: {} votos a favor, {} en contra", 
                        projectId, results.votesInFavor(), results.votesAgainst());
                return results;
            }

            log.warn("Respuesta vacía para resultados del proyecto {}", projectId);
            return null;

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("No se encontraron resultados para proyecto {}", projectId);
            return null;
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP al obtener resultados del proyecto {}: {}",
                    projectId, e.getMessage());
            return null;
        } catch (RestClientException e) {
            log.error("Error de conexión al obtener resultados del proyecto {}: {}",
                    projectId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error inesperado al obtener resultados del proyecto {}: {}",
                    projectId, e.getMessage(), e);
            return null;
        }
    }

  
    public VoteDto getUserVoteForProject(Long projectId, String token) {
        try {
            String url = String.format("%s/votaciones/%d/mis-votos",
                    VOTING_BASE_URL, projectId);

            log.debug("Consultando voto del usuario para proyecto {}", projectId);

            HttpHeaders headers = new HttpHeaders();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            } else {
                log.warn("No se envía token para obtener voto del proyecto {}", projectId);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<VoteDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    VoteDto.class
            );

            VoteDto vote = response.getBody();

            if (vote != null) {
                log.debug("Usuario votó {} en proyecto {}", 
                        vote.decision() ? "A FAVOR" : "EN CONTRA", projectId);
                return vote;
            }

            log.debug("No se encontró voto del usuario para proyecto {}", projectId);
            return null;

        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Usuario no ha votado en proyecto {}", projectId);
            return null;
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP al obtener voto del proyecto {}: {}",
                    projectId, e.getMessage());
            return null;
        } catch (RestClientException e) {
            log.error("Error de conexión al obtener voto del proyecto {}: {}",
                    projectId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error inesperado al obtener voto del proyecto {}: {}",
                    projectId, e.getMessage(), e);
            return null;
        }
    }
}