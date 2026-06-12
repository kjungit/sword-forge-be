package com.swordforge.web.api;

import com.swordforge.application.weapon.WeaponCatalogService;
import com.swordforge.application.weapon.WeaponInventoryService;
import com.swordforge.common.api.ApiResponse;
import com.swordforge.web.dto.SaveDataResponse;
import com.swordforge.web.dto.WeaponEquipRequest;
import com.swordforge.web.dto.WeaponLockRequest;
import com.swordforge.web.dto.WeaponResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/weapons")
public class WeaponApiController {

    private final WeaponCatalogService weaponCatalogService;
    private final WeaponInventoryService weaponInventoryService;

    public WeaponApiController(
            WeaponCatalogService weaponCatalogService,
            WeaponInventoryService weaponInventoryService
    ) {
        this.weaponCatalogService = weaponCatalogService;
        this.weaponInventoryService = weaponInventoryService;
    }

    @GetMapping
    public ApiResponse<List<WeaponResponse>> findAll() {
        return ApiResponse.ok(weaponCatalogService.findAll().stream()
                .map(WeaponResponse::from)
                .toList());
    }

    @GetMapping("/{weaponId}")
    public ApiResponse<WeaponResponse> findById(@PathVariable String weaponId) {
        return ApiResponse.ok(WeaponResponse.from(weaponCatalogService.findById(weaponId)));
    }

    @PostMapping("/equip")
    public ApiResponse<SaveDataResponse> equip(@Valid @RequestBody WeaponEquipRequest request) {
        return ApiResponse.ok(SaveDataResponse.from(
                weaponInventoryService.equip(request.userId(), request.weaponId())
        ));
    }

    @PostMapping("/lock")
    public ApiResponse<SaveDataResponse> lock(@Valid @RequestBody WeaponLockRequest request) {
        return ApiResponse.ok(SaveDataResponse.from(
                weaponInventoryService.lock(request.userId(), request.weaponId())
        ));
    }

    @PostMapping("/unlock")
    public ApiResponse<SaveDataResponse> unlock(@Valid @RequestBody WeaponLockRequest request) {
        return ApiResponse.ok(SaveDataResponse.from(
                weaponInventoryService.unlock(request.userId(), request.weaponId())
        ));
    }
}
