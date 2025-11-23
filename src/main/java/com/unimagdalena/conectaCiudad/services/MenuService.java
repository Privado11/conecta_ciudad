package com.unimagdalena.conectaCiudad.services;

import com.unimagdalena.conectaCiudad.Dto.menu.MenuItemDto;
import com.unimagdalena.conectaCiudad.Dto.menu.MenuResponseDto;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {
    
    private final UserRepository userRepository;
    
    private static final Map<String, Integer> ROLE_HIERARCHY = Map.of(
        "ADMIN", 1,
        "CURATOR", 2,
        "LIDER_COMUNITARIO", 3,
        "CIUDADANO", 4
    );
    
    public MenuResponseDto getCompleteMenuForUser(String username) {
        User user = findUserByUsername(username);
        
        List<MenuItemDto> menu = buildMenuForUser(user);
        
        MenuResponseDto.UserInfoDto userInfo = new MenuResponseDto.UserInfoDto(
            user.getEmail(),
            user.getName(),
            getPrimaryRole(user),
            null 
        );
        
        return new MenuResponseDto(userInfo, menu);
    }
    
    @Cacheable(value = "userMenu", key = "#username")
    public List<MenuItemDto> getMenuForUser(String username) {
        User user = findUserByUsername(username);
        return buildMenuForUser(user);
    }
    
    private List<MenuItemDto> buildMenuForUser(User user) {
        log.debug("Building menu for user: {}", user.getEmail());
        
        List<MenuItemDto> menu = new ArrayList<>();
        int order = 1;
        
        menu.add(new MenuItemDto("Inicio", "/dashboard", "Home", null, order++));
        
        String primaryRole = getHighestPriorityRole(user);
        
        switch (primaryRole) {
            case "ADMIN" -> {
                menu.addAll(createAdminMenu(order));
                order += 10;
            }
            case "CURATOR" -> {
                menu.addAll(createCuratorMenu(order));
                order += 10;
            }
            case "LIDER_COMUNITARIO" -> {
                menu.addAll(createCommunityLeaderMenu(order));
                order += 10;
            }
            case "CIUDADANO" -> {
                menu.addAll(createCitizenMenu(order));
                order += 10;
            }
        }

        
        menu.add(new MenuItemDto("Configuración", "/setting", "Settings"));
        
        menu.add(new MenuItemDto("Mi Perfil", "/profile", "User", false, 99, List.of()));
        
        return menu;
    }
    
    private List<MenuItemDto> createAdminMenu(int startOrder) {
        List<MenuItemDto> menu = new ArrayList<>();
        
        List<MenuItemDto> userMgmt = List.of(
            new MenuItemDto("Todos los Usuarios", "/admin/users", "Users"),
            new MenuItemDto("Roles y Permisos", "/admin/roles", "Shield")
        );
        menu.add(new MenuItemDto("Gestión de Usuarios", "#", "Users", userMgmt));
        
        menu.add(new MenuItemDto("Proyectos", "/admin/projects", "FolderKanban"));
        
        menu.add(new MenuItemDto("Votaciones", "/admin/voting", "Vote"));
        
        List<MenuItemDto> comms = List.of(
            new MenuItemDto("Envío Masivo", "/admin/notifications", "Send"),
            new MenuItemDto("Historial", "/admin/communications/history", "History")
        );
        menu.add(new MenuItemDto("Comunicaciones", "#", "MessageSquare", comms));

        menu.add(new MenuItemDto("Auditoría", "/admin/audit", "ShieldCheck"));
        
        
        return menu;
    }
    
    private List<MenuItemDto> createCuratorMenu(int startOrder) {
        List<MenuItemDto> menu = new ArrayList<>();
        
        menu.add(new MenuItemDto("Cola de Revisión", "/curator/review/pending", "Clock"));
        
       
        menu.add(new MenuItemDto("Historial", "/curator/review/history", "History"));
        
       
        
        return menu;
    }
    
    private List<MenuItemDto> createCommunityLeaderMenu(int startOrder) {
        List<MenuItemDto> menu = new ArrayList<>();
        
        List<MenuItemDto> myProjects = List.of(
            new MenuItemDto("Crear Proyecto", "/lider/projects/create", "PlusCircle", true, 1, List.of()),
            new MenuItemDto("En Revisión", "/lider/projects/review", "Clock"),
            new MenuItemDto("Publicados", "/lider/projects/published", "Globe"),
            new MenuItemDto("Devueltos", "/lider/projects/returned", "RotateCcw"),
            new MenuItemDto("Todos", "/lider/projects/my-projects", "FolderOpen")
        );
        menu.add(new MenuItemDto("Mis Proyectos", "#", "FolderKanban", myProjects));
        
        menu.add(new MenuItemDto("Explorar Proyectos", "/lider/projects/explore", "Search"));
        menu.add(new MenuItemDto("Resultados", "/lider/results", "BarChart3"));
        
        return menu;
    }
  
    private List<MenuItemDto> createCitizenMenu(int startOrder) {
        List<MenuItemDto> menu = new ArrayList<>();

        List<MenuItemDto> projectsSubmenu = List.of(
                new MenuItemDto("Proyectos en Votación", "/citizen/projects/voting", "Vote"),
                new MenuItemDto("Próximas Votaciones", "/citizen/projects/upcoming", "Clock")
        );

        menu.add(new MenuItemDto("Proyectos", "#", "FolderKanban", projectsSubmenu));

        List<MenuItemDto> votationsSubmenu = List.of(
                new MenuItemDto("Mis Votaciones", "citizen/my-votes", "FileCheck"),
                new MenuItemDto("Resultados", "/citizen/projects/upcoming", "BarChart3")
        );

        menu.add(new MenuItemDto("Votaciones", "#", "Vote", votationsSubmenu));

        return menu;
    }
    
    private String getHighestPriorityRole(User user) {
        return user.getRoles().stream()
            .map(role -> role.getName().toUpperCase())
            .filter(ROLE_HIERARCHY::containsKey)
            .min((role1, role2) -> 
                ROLE_HIERARCHY.get(role1).compareTo(ROLE_HIERARCHY.get(role2))
            )
            .orElse("CIUDADANO"); 
    }
    
    
    private String getPrimaryRole(User user) {
        String role = getHighestPriorityRole(user);
        
        return switch (role) {
            case "ADMIN" -> "Administrador";
            case "CURATOR" -> "Curador";
            case "LIDER_COMUNITARIO" -> "Líder Comunitario";
            case "CIUDADANO" -> "Ciudadano";
            default -> "Usuario";
        };
    }
    
    private User findUserByUsername(String username) {
        return userRepository.findByEmailOrNationalId(username, username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}