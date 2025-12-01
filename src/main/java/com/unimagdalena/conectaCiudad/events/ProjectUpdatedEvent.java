package com.unimagdalena.conectaCiudad.events;

import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ProjectUpdatedEvent extends BaseAuditEvent {
    
    private final Project project;
    
    public ProjectUpdatedEvent(Object source, Project project, User updater) {
        super(source, "PROJECT_UPDATE",
              "Updated project: " + project.getName(),
              EntityType.PROJECT,
              project.getId(),
              updater,
              buildMetadata(project));
        this.project = project;
    }
    
    private static Map<String, Object> buildMetadata(Project project) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("projectName", project.getName());
        metadata.put("status", project.getStatus().name());
        return metadata;
    }
}
