package com.vegas.analytics.controller;

import com.vegas.analytics.dto.PersonalRecordResponse;
import com.vegas.analytics.security.CurrentUser;
import com.vegas.analytics.service.RecordsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Tag(name = "Records", description = "Личные рекорды")
public class RecordsController {

    private final RecordsService recordsService;

    @GetMapping
    @Operation(summary = "Лента личных рекордов, новые сверху")
    public List<PersonalRecordResponse> records(@AuthenticationPrincipal Jwt jwt,
                                                @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return recordsService.records(CurrentUser.id(jwt), limit);
    }
}
