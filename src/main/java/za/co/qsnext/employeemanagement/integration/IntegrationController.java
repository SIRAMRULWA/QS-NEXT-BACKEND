package za.co.qsnext.employeemanagement.integration;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.integration.dto.IntegrationConfigResponse;
import za.co.qsnext.employeemanagement.integration.dto.IntegrationSettingResponse;
import za.co.qsnext.employeemanagement.integration.dto.UpdateIntegrationConfigRequest;
import za.co.qsnext.employeemanagement.integration.dto.UpsertIntegrationSettingRequest;

import java.util.List;

@Tag(name = "Integrations", description = "Third-party integration configuration (email, storage, AI, payroll, ...).")
@RestController
@RequestMapping("/api/v1/integrations")
@PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
public class IntegrationController {

    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @Operation(summary = "Get all configs")
    @GetMapping
    public ResponseEntity<List<IntegrationConfigResponse>> getAllConfigs() {
        return ResponseEntity.ok(integrationService.getAllConfigs());
    }

    @Operation(summary = "Get config")
    @GetMapping("/{type}")
    public ResponseEntity<IntegrationConfigResponse> getConfig(@PathVariable String type) {
        return ResponseEntity.ok(integrationService.getConfig(type));
    }

    @Operation(summary = "Update config")
    @PutMapping("/{type}")
    public ResponseEntity<IntegrationConfigResponse> updateConfig(
            @PathVariable String type,
            @Valid @RequestBody UpdateIntegrationConfigRequest request
    ) {
        return ResponseEntity.ok(
                integrationService.updateConfig(type, request.providerName(), request.enabled())
        );
    }

    @Operation(summary = "Get settings")
    @GetMapping("/{type}/settings")
    public ResponseEntity<List<IntegrationSettingResponse>> getSettings(@PathVariable String type) {
        return ResponseEntity.ok(integrationService.getSettings(type));
    }

    @Operation(summary = "Upsert setting")
    @PutMapping("/{type}/settings/{settingKey}")
    public ResponseEntity<IntegrationSettingResponse> upsertSetting(
            @PathVariable String type,
            @PathVariable String settingKey,
            @Valid @RequestBody UpsertIntegrationSettingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(
                integrationService.upsertSetting(type, settingKey, request.settingValue())
        );
    }

    @Operation(summary = "Delete setting")
    @DeleteMapping("/{type}/settings/{settingKey}")
    public ResponseEntity<Void> deleteSetting(@PathVariable String type, @PathVariable String settingKey) {
        integrationService.deleteSetting(type, settingKey);
        return ResponseEntity.noContent().build();
    }
}
