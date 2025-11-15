package com.unimagdalena.conectaCiudad.Dto.review;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.Dto.user.UserMapper;

@Mapper(componentModel = "spring", uses = { UserMapper.class })
public interface ReviewMapper {

    ReviewMapper INSTANCE = Mappers.getMapper(ReviewMapper.class);

    Review toEntity(ReviewDto reviewDto);
    ReviewDto toDto(Review review);
}
