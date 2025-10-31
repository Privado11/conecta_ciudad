package com.unimagdalena.conectaCiudad.controllers;

import com.unimagdalena.conectaCiudad.Dto.menu.MenuResponseDto;
import com.unimagdalena.conectaCiudad.services.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
@Tag(name = "Menú de Navegación", description = "Operaciones para obtener el menú dinámico según el rol del usuario autenticado")
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    @Operation(
        summary = "Obtener menú completo e información del usuario",
        description = """
            Devuelve la estructura completa del menú junto con los datos del usuario autenticado.
            
            El menú se genera dinámicamente según el rol del usuario:
            - **ADMIN (Administrador)**: Acceso completo a gestión de usuarios, proyectos, votaciones, comunicaciones y configuración
            - **CURATOR (Curador)**: Acceso a cola de revisión, proyectos, auditoría y reportes
            - **LIDER_COMUNITARIO (Líder Comunitario)**: Acceso a creación y gestión de proyectos propios, exploración y resultados
            - **CIUDADANO (Ciudadano)**: Acceso a exploración de proyectos y votaciones
            
            El sistema usa jerarquía de roles: si un usuario tiene múltiples roles, se usa el de mayor prioridad.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Menú completo cargado correctamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MenuResponseDto.class),
                examples = {
                    @ExampleObject(
                        name = "Menú Administrador",
                        summary = "Ejemplo de menú para rol ADMIN",
                        value = """
                        {
                            "user": {
                                "username": "admin@conectaciudad.com",
                                "fullName": "Walter Jiménez",
                                "role": "Administrador",
                                "avatar": null
                            },
                            "menu": [
                                {
                                    "label": "Inicio",
                                    "route": "/dashboard",
                                    "icon": "Home",
                                    "highlight": false,
                                    "order": 1,
                                    "children": []
                                },
                                {
                                    "label": "Gestión de Usuarios",
                                    "route": "#",
                                    "icon": "Users",
                                    "highlight": false,
                                    "order": 2,
                                    "children": [
                                        {
                                            "label": "Todos los Usuarios",
                                            "route": "/admin/users",
                                            "icon": "Users",
                                            "highlight": false,
                                            "order": 1,
                                            "children": []
                                        },
                                        {
                                            "label": "Roles y Permisos",
                                            "route": "/admin/roles",
                                            "icon": "Shield",
                                            "highlight": false,
                                            "order": 2,
                                            "children": []
                                        },
                                        {
                                            "label": "Actividad Reciente",
                                            "route": "/admin/activity",
                                            "icon": "Activity",
                                            "highlight": false,
                                            "order": 3,
                                            "children": []
                                        }
                                    ]
                                },
                                {
                                    "label": "Proyectos",
                                    "route": "#",
                                    "icon": "FolderKanban",
                                    "highlight": false,
                                    "order": 3,
                                    "children": [
                                        {
                                            "label": "Todos los Proyectos",
                                            "route": "/admin/projects",
                                            "icon": "FolderKanban",
                                            "highlight": false,
                                            "order": 1,
                                            "children": []
                                        },
                                        {
                                            "label": "Estados",
                                            "route": "/admin/projects/status",
                                            "icon": "ListChecks",
                                            "highlight": false,
                                            "order": 2,
                                            "children": []
                                        }
                                    ]
                                },
                                {
                                    "label": "Votaciones",
                                    "route": "#",
                                    "icon": "Vote",
                                    "highlight": false,
                                    "order": 4,
                                    "children": [
                                        {
                                            "label": "Procesos Activos",
                                            "route": "/admin/voting/active",
                                            "icon": "Vote",
                                            "highlight": false,
                                            "order": 1,
                                            "children": []
                                        },
                                        {
                                            "label": "Auditoría",
                                            "route": "/admin/voting/audit",
                                            "icon": "ShieldCheck",
                                            "highlight": false,
                                            "order": 2,
                                            "children": []
                                        }
                                    ]
                                },
                                {
                                    "label": "Comunicaciones",
                                    "route": "#",
                                    "icon": "MessageSquare",
                                    "highlight": false,
                                    "order": 5,
                                    "children": [
                                        {
                                            "label": "Envío Masivo",
                                            "route": "/admin/notifications",
                                            "icon": "Send",
                                            "highlight": false,
                                            "order": 1,
                                            "children": []
                                        },
                                        {
                                            "label": "Historial",
                                            "route": "/admin/communications/history",
                                            "icon": "History",
                                            "highlight": false,
                                            "order": 2,
                                            "children": []
                                        }
                                    ]
                                },
                                {
                                    "label": "Configuración",
                                    "route": "#",
                                    "icon": "Settings",
                                    "highlight": false,
                                    "order": 6,
                                    "children": [
                                        {
                                            "label": "Seguridad",
                                            "route": "/admin/config/security",
                                            "icon": "Lock",
                                            "highlight": false,
                                            "order": 1,
                                            "children": []
                                        }
                                    ]
                                },
                                {
                                    "label": "Mi Perfil",
                                    "route": "/profile",
                                    "icon": "User",
                                    "highlight": false,
                                    "order": 99,
                                    "children": []
                                }
                            ]
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Menú Curador",
                        summary = "Ejemplo de menú para rol CURATOR",
                        value = """
                        {
                            "user": {
                                "username": "curador@conectaciudad.com",
                                "fullName": "María González",
                                "role": "Curador",
                                "avatar": null
                            },
                            "menu": [
                                {
                                    "label": "Inicio",
                                    "route": "/dashboard",
                                    "icon": "Home",
                                    "highlight": false,
                                    "order": 1,
                                    "children": []
                                },
                                {
                                    "label": "Cola de Revisión",
                                    "route": "#",
                                    "icon": "ClipboardCheck",
                                    "highlight": false,
                                    "order": 2,
                                    "children": [
                                        {
                                            "label": "Pendientes",
                                            "route": "/review/pending",
                                            "icon": "Clock",
                                            "highlight": true,
                                            "order": 1,
                                            "children": []
                                        },
                                        {
                                            "label": "En Proceso",
                                            "route": "/review/in-progress",
                                            "icon": "Loader",
                                            "highlight": false,
                                            "order": 2,
                                            "children": []
                                        },
                                        {
                                            "label": "Historial",
                                            "route": "/review/history",
                                            "icon": "History",
                                            "highlight": false,
                                            "order": 3,
                                            "children": []
                                        }
                                    ]
                                },
                                {
                                    "label": "Proyectos",
                                    "route": "#",
                                    "icon": "FolderKanban",
                                    "highlight": false,
                                    "order": 3,
                                    "children": [
                                        {
                                            "label": "Aprobados",
                                            "route": "/projects/approved",
                                            "icon": "CheckCircle",
                                            "highlight": false,
                                            "order": 1,
                                            "children": []
                                        },
                                        {
                                            "label": "Rechazados",
                                            "route": "/projects/rejected",
                                            "icon": "XCircle",
                                            "highlight": false,
                                            "order": 2,
                                            "children": []
                                        },
                                        {
                                            "label": "Todos",
                                            "route": "/projects/all",
                                            "icon": "List",
                                            "highlight": false,
                                            "order": 3,
                                            "children": []
                                        }
                                    ]
                                },
                                {
                                    "label": "Auditoría",
                                    "route": "/audit",
                                    "icon": "Shield",
                                    "highlight": false,
                                    "order": 4,
                                    "children": []
                                },
                                {
                                    "label": "Reportes",
                                    "route": "/reports",
                                    "icon": "FileText",
                                    "highlight": false,
                                    "order": 5,
                                    "children": []
                                },
                                {
                                    "label": "Mi Perfil",
                                    "route": "/profile",
                                    "icon": "User",
                                    "highlight": false,
                                    "order": 99,
                                    "children": []
                                }
                            ]
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Menú Líder Comunitario",
                        summary = "Ejemplo de menú para rol LIDER_COMUNITARIO",
                        value = """
                        {
                            "user": {
                                "username": "lider@conectaciudad.com",
                                "fullName": "Carlos Rodríguez",
                                "role": "Líder Comunitario",
                                "avatar": null
                            },
                            "menu": [
                                {
                                    "label": "Inicio",
                                    "route": "/dashboard",
                                    "icon": "Home",
                                    "highlight": false,
                                    "order": 1,
                                    "children": []
                                },
                                {
                                    "label": "Mis Proyectos",
                                    "route": "#",
                                    "icon": "FolderKanban",
                                    "highlight": false,
                                    "order": 2,
                                    "children": [
                                        {
                                            "label": "Crear Proyecto",
                                            "route": "/projects/create",
                                            "icon": "PlusCircle",
                                            "highlight": true,
                                            "order": 1,
                                            "children": []
                                        },
                                        {
                                            "label": "En Revisión",
                                            "route": "/projects/review",
                                            "icon": "Clock",
                                            "highlight": false,
                                            "order": 2,
                                            "children": []
                                        },
                                        {
                                            "label": "Publicados",
                                            "route": "/projects/published",
                                            "icon": "Globe",
                                            "highlight": false,
                                            "order": 3,
                                            "children": []
                                        },
                                        {
                                            "label": "Devueltos",
                                            "route": "/projects/returned",
                                            "icon": "RotateCcw",
                                            "highlight": false,
                                            "order": 4,
                                            "children": []
                                        },
                                        {
                                            "label": "Todos",
                                            "route": "/projects/my-projects",
                                            "icon": "FolderOpen",
                                            "highlight": false,
                                            "order": 5,
                                            "children": []
                                        }
                                    ]
                                },
                                {
                                    "label": "Explorar Proyectos",
                                    "route": "/projects/explore",
                                    "icon": "Search",
                                    "highlight": false,
                                    "order": 3,
                                    "children": []
                                },
                                {
                                    "label": "Resultados",
                                    "route": "/results",
                                    "icon": "BarChart3",
                                    "highlight": false,
                                    "order": 4,
                                    "children": []
                                },
                                {
                                    "label": "Mi Perfil",
                                    "route": "/profile",
                                    "icon": "User",
                                    "highlight": false,
                                    "order": 99,
                                    "children": []
                                }
                            ]
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Menú Ciudadano",
                        summary = "Ejemplo de menú para rol CIUDADANO",
                        value = """
                        {
                            "user": {
                                "username": "ciudadano@conectaciudad.com",
                                "fullName": "Ana Martínez",
                                "role": "Ciudadano",
                                "avatar": null
                            },
                            "menu": [
                                {
                                    "label": "Inicio",
                                    "route": "/dashboard",
                                    "icon": "Home",
                                    "highlight": false,
                                    "order": 1,
                                    "children": []
                                },
                                {
                                    "label": "Proyectos",
                                    "route": "#",
                                    "icon": "FolderKanban",
                                    "highlight": false,
                                    "order": 2,
                                    "children": [
                                        {
                                            "label": "Explorar",
                                            "route": "/projects",
                                            "icon": "Search",
                                            "highlight": false,
                                            "order": 1,
                                            "children": []
                                        },
                                        {
                                            "label": "Mis Votaciones",
                                            "route": "/my-votes",
                                            "icon": "Vote",
                                            "highlight": false,
                                            "order": 2,
                                            "children": []
                                        }
                                    ]
                                },
                                {
                                    "label": "Votar",
                                    "route": "/vote",
                                    "icon": "Vote",
                                    "highlight": false,
                                    "order": 3,
                                    "children": []
                                },
                                {
                                    "label": "Mi Perfil",
                                    "route": "/profile",
                                    "icon": "User",
                                    "highlight": false,
                                    "order": 99,
                                    "children": []
                                }
                            ]
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Usuario no autenticado - Token JWT no válido o ausente"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Usuario no encontrado en el sistema"
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Error interno al construir el menú"
        )
    })
    public ResponseEntity<MenuResponseDto> getCompleteMenu() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (String) authentication.getPrincipal();
        
        MenuResponseDto response = menuService.getCompleteMenuForUser(username);
        return ResponseEntity.ok(response);
    }
}