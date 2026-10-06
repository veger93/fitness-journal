package com.vegas.analytics.controller;

import com.vegas.analytics.dto.MonthlyReportResponse;
import com.vegas.analytics.security.CurrentUser;
import com.vegas.analytics.service.MonthlyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Отчёты")
public class ReportsController {

    private final MonthlyReportService monthlyReportService;

    /** YearMonth из строки "2026-09": Spring разберёт сам благодаря @DateTimeFormat. */
    @GetMapping("/monthly")
    @Operation(summary = "Итоги месяца и полный отчёт (?month=2026-09, по умолчанию — текущий месяц)")
    public MonthlyReportResponse monthly(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return monthlyReportService.report(CurrentUser.id(jwt), month);
    }
}
