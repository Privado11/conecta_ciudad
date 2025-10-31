package com.unimagdalena.conectaCiudad.Dto.menu;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MenuResponseDto(
    UserInfoDto user,              
    List<MenuItemDto> menu         
) {
    public record UserInfoDto(
        String username,           
        String fullName,           
        String role,               
        String avatar             
    ) {}
}
