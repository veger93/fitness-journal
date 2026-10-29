package com.vegas.user.controller;

import com.vegas.user.dto.ClaimLinkResponse;
import com.vegas.user.dto.ClientResponse;
import com.vegas.user.dto.CreateClientRequest;
import com.vegas.user.dto.MyTrainerResponse;
import com.vegas.user.dto.TrainerModeRequest;
import com.vegas.user.dto.UpdateClientRequest;
import com.vegas.user.dto.UserResponse;
import com.vegas.user.security.AuthenticatedUser;
import com.vegas.user.service.ClaimService;
import com.vegas.user.service.TrainerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Режим тренера и клиенты тренера. Путь /api/users/me/... — gateway уже ведёт его в user-service,
 * а "me" подчёркивает: это МОИ клиенты, тренер берётся из токена, подменить его нельзя.
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "Trainer", description = "Режим тренера и его клиенты")
@SecurityRequirement(name = "bearerAuth")
public class TrainerController {

    private final TrainerService trainerService;
    private final ClaimService claimService;

    @PutMapping("/trainer-mode")
    @Operation(summary = "Включить/выключить режим тренера")
    public UserResponse setTrainerMode(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                       @Valid @RequestBody TrainerModeRequest request) {
        return trainerService.setTrainerMode(currentUser.id(), request.enabled());
    }

    @GetMapping("/clients")
    @Operation(summary = "Мои клиенты (активные), новые сверху")
    public List<ClientResponse> clients(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return trainerService.listClients(currentUser.id());
    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Завести клиента (без регистрации: аккаунт ведёт тренер)")
    public ClientResponse createClient(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                       @Valid @RequestBody CreateClientRequest request) {
        return trainerService.createClient(currentUser.id(), request);
    }

    @GetMapping("/clients/{clientId}")
    @Operation(summary = "Карточка клиента")
    public ClientResponse client(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                 @PathVariable UUID clientId) {
        return trainerService.getClient(currentUser.id(), clientId);
    }

    @PutMapping("/clients/{clientId}")
    @Operation(summary = "Изменить имя (только у ведомого клиента) и телефон")
    public ClientResponse updateClient(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                       @PathVariable UUID clientId,
                                       @Valid @RequestBody UpdateClientRequest request) {
        return trainerService.updateClient(currentUser.id(), clientId, request);
    }

    @PostMapping("/clients/{clientId}/claim-link")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Передать аккаунт: одноразовая ссылка на 7 дней (старые перестают работать)")
    public ClaimLinkResponse claimLink(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                       @PathVariable UUID clientId) {
        return claimService.createLink(currentUser.id(), clientId);
    }

    // ---- со стороны клиента ----

    @GetMapping("/trainer")
    @Operation(summary = "Мой тренер (204 — тренера нет)")
    public ResponseEntity<MyTrainerResponse> myTrainer(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        // есть тренер — 200 с телом, нет — 204 без тела (как GET /api/workouts/current)
        return claimService.myTrainer(currentUser.id())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/trainer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Отключить тренера: он больше не видит и не записывает мои тренировки")
    public void leaveTrainer(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        claimService.leaveTrainer(currentUser.id());
    }
}
