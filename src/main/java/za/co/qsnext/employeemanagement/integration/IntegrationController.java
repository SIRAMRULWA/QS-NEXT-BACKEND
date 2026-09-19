package za.co.qsnext.employeemanagement.integration;

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

@RestController
@RequestMapping("/api/v1/integrations")
@PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
public class IntegrationController {

    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @GetMapping
    public ResponseEntity<List<IntegrationConfigResponse>> getAllConfigs() {
        return ResponseEntity.ok(integrationService.getAllConfigs());
    }

    @GetMapping("/{type}")
    public ResponseEntity<IntegrationConfigResponse> getConfig(@PathVariable String type) {
        return ResponseEntity.ok(integrationService.getConfig(type));
    }

    @PutMapping("/{type}")
    public ResponseEntity<IntegrationConfigResponse> updateConfig(
            @PathVariable String type,
            @Valid @RequestBody UpdateIntegrationConfigRequest request
    ) {
        return ResponseEntity.ok(
                integrationService.updateConfig(type, request.providerName(), request.enabled())
        );
    }

    @GetMapping("/{type}/settings")
    public ResponseEntity<List<IntegrationSettingResponse>> getSettings(@PathVariable String type) {
        return ResponseEntity.ok(integrationService.getSettings(type));
    }

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

    @DeleteMapping("/{type}/settings/{settingKey}")
    public ResponseEntity<Void> deleteSetting(@PathVariable String type, @PathVariable String settingKey) {
        integrationService.deleteSetting(type, settingKey);
        return ResponseEntity.noContent().build();
    }
}
