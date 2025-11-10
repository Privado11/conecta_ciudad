package com.unimagdalena.conectaCiudad.controllers;

import java.time.LocalDateTime;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;
import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.services.action.ActionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(
    name = "Auditoría y Trazabilidad",
    description = "API para consultar acciones y estadísticas de auditoría del sistema."
)
public class AuditController {

    private final ActionService actionService;
    private static final int MAX_PAGE_SIZE = 100;

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    @Operation(summary = "Obtener todas las acciones con estadísticas")
    public ResponseEntity<PagedResponse<ActionDto>> getAllActions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        return ResponseEntity.ok(actionService.findByDateRange(start, end, pageable));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/user/{userId}")
    @Operation(summary = "Obtener acciones de un usuario con estadísticas")
    public ResponseEntity<PagedResponse<ActionDto>> getActionsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(actionService.findByUserId(userId, pageable));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Obtener historial de una entidad con estadísticas")
    public ResponseEntity<PagedResponse<ActionDto>> getActionsByEntity(
            @PathVariable EntityType entityType,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(actionService.findByEntityTypeAndId(entityType, entityId, pageable));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/type/{actionType}")
    @Operation(summary = "Filtrar acciones por tipo con estadísticas")
    public ResponseEntity<PagedResponse<ActionDto>> getActionsByType(
            @PathVariable String actionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(actionService.findByActionType(actionType, pageable));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/result/{result}")
    @Operation(summary = "Filtrar acciones por resultado con estadísticas")
    public ResponseEntity<PagedResponse<ActionDto>> getActionsByResult(
            @PathVariable ActionResult result,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(actionService.findByResult(result, pageable));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/date-range")
    @Operation(summary = "Filtrar acciones por rango de fechas con estadísticas")
    public ResponseEntity<PagedResponse<ActionDto>> getActionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(actionService.findByDateRange(startDate, endDate, pageable));
    }
}
