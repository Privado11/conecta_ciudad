package com.unimagdalena.conectaCiudad.Dto.project;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.unimagdalena.conectaCiudad.entities.Project;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    ProjectMapper INSTANCE = Mappers.getMapper(ProjectMapper.class);

    Project toEntity(ProjectSaveDto projectSaveDto);
    ProjectDto toDto(Project project);
    ProjectSaveDto toProjectSaveDto(Project project);
}
