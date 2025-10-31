package com.unimagdalena.conectaCiudad.Dto.menu;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MenuItemDto(
    String label,           
    String route,           
    String icon,            
    Boolean highlight,      
    Integer order,          
    List<MenuItemDto> children  
) {
    public MenuItemDto(String label, String route, String icon) {
        this(label, route, icon, false, 0, List.of());
    }
    public MenuItemDto(String label, String route, String icon, List<MenuItemDto> children) {
        this(label, route, icon, false, 0, children);
    }
    public MenuItemDto(String label, String route, String icon, int order) {
        this(label, route, icon, false, order, List.of());
    }
    public MenuItemDto(String label, String route, String icon, Boolean highlight, Integer order) {
        this(label, route, icon, highlight, order, List.of());
    }
}
