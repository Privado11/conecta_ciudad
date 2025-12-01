package com.unimagdalena.conectaCiudad.events;

import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class VotingClosedEvent extends BaseAuditEvent {
    
    private final Project project;
    private final long totalVotes;
    
    public VotingClosedEvent(Object source, Project project, long totalVotes) {
        super(source, "VOTING_CLOSE",
              "Closed voting for project: " + project.getName(),
              EntityType.PROJECT,
              project.getId(),
              null, // System action
              buildMetadata(project, totalVotes));
        this.project = project;
        this.totalVotes = totalVotes;
    }
    
    private static Map<String, Object> buildMetadata(Project project, long totalVotes) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("projectName", project.getName());
        metadata.put("totalVotes", totalVotes);
        return metadata;
    }
}
