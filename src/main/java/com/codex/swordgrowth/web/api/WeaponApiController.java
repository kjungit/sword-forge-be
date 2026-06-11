package com.codex.swordgrowth.web.api;

import com.codex.swordgrowth.application.weapon.WeaponCatalogService;
import com.codex.swordgrowth.common.api.ApiResponse;
import com.codex.swordgrowth.web.dto.WeaponResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/weapons")
public class WeaponApiController {

    private final WeaponCatalogService weaponCatalogService;

    public WeaponApiController(WeaponCatalogService weaponCatalogService) {
        this.weaponCatalogService = weaponCatalogService;
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
}

