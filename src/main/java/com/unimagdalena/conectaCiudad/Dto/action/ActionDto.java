package com.unimagdalena.conectaCiudad.Dto.action;


import java.time.OffsetDateTime;

import com.unimagdalena.conectaCiudad.Dto.access.AccessDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;

import lombok.Builder;

@Builder
public record ActionDto(
    Long id,
    String actionType,      
    String description,
    EntityType entityType,  
    Long entityId,          
    ActionResult result,    
    String metadata,        
    String ipAddress,       
    OffsetDateTime actionAt,
    UserDto user,            
    AccessDto access         
) {}
