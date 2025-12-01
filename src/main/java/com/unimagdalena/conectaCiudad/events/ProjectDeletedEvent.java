package com.unimagdalena.conectaCiudad.events;

import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ProjectDeletedEvent extends BaseAuditEvent {
    
    private final String projectName;
    
    public ProjectDeletedEvent(Object source, Long projectId, String projectName, User deleter) {
        super(source, "PROJECT_DELETE",
              "Deleted project: " + projectName,
              EntityType.PROJECT,
              projectId,
              deleter,
              buildMetadata(projectName));
        this.projectName = projectName;
    }
    
    private static Map<String, Object> buildMetadata(String projectName) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("projectName", projectName);
        return metadata;
    }
}
