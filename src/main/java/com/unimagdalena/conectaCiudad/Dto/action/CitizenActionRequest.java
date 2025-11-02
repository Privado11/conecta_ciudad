package com.unimagdalena.conectaCiudad.Dto.action;

import com.unimagdalena.conectaCiudad.enums.CitizenActionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenActionRequest {
    
    @NotNull(message = "Action type is required")
    private CitizenActionType actionType;
    
    @NotBlank(message = "Description is required")
    @Size(max = 250, message = "Description cannot exceed 250 characters")
    private String description;
    
    @NotNull(message = "Project ID is required")
    private Long projectId;
    
    public static CitizenActionRequest of(CitizenActionType actionType, String description, Long projectId) {
        return CitizenActionRequest.builder()
                .actionType(actionType)
                .description(description)
                .projectId(projectId)
                .build();
    }
}
