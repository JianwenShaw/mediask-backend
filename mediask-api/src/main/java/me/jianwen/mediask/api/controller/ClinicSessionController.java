package me.jianwen.mediask.api.controller;

import java.time.LocalDate;
import me.jianwen.mediask.api.assembler.OutpatientAssembler;
import me.jianwen.mediask.api.dto.ClinicSessionListResponse;
import me.jianwen.mediask.application.outpatient.query.ListClinicSessionsQuery;
import me.jianwen.mediask.application.outpatient.usecase.ListClinicSessionsUseCase;
import me.jianwen.mediask.common.result.Result;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clinic-sessions")
public class ClinicSessionController {

    private final ListClinicSessionsUseCase listClinicSessionsUseCase;

    public ClinicSessionController(ListClinicSessionsUseCase listClinicSessionsUseCase) {
        this.listClinicSessionsUseCase = listClinicSessionsUseCase;
    }

    @GetMapping
    public Result<ClinicSessionListResponse> list(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return Result.ok(OutpatientAssembler.toClinicSessionListResponse(
                listClinicSessionsUseCase.handle(new ListClinicSessionsQuery(departmentId, dateFrom, dateTo))));
    }
}
