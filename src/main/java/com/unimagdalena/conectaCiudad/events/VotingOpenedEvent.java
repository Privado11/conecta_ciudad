package com.unimagdalena.conectaCiudad.events;

import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class VotingOpenedEvent extends BaseAuditEvent {
    
    private final Project project;
    
    public VotingOpenedEvent(Object source, Project project) {
        super(source, "VOTING_OPEN",
              "Opened voting for project: " + project.getName(),
              EntityType.PROJECT,
              project.getId(),
              null, 
              buildMetadata(project));
        this.project = project;
    }
    
    private static Map<String, Object> buildMetadata(Project project) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("projectName", project.getName());
        metadata.put("votingStartAt", project.getVotingStartAt());
        metadata.put("votingEndAt", project.getVotingEndAt());
        return metadata;
    }
}
