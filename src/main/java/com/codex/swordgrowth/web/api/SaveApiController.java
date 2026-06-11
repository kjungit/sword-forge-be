package com.codex.swordgrowth.web.api;

import com.codex.swordgrowth.application.save.PlayerSaveService;
import com.codex.swordgrowth.common.api.ApiResponse;
import com.codex.swordgrowth.web.dto.SaveDataResponse;
import com.codex.swordgrowth.web.dto.SaveUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/saves")
public class SaveApiController {

    private final PlayerSaveService playerSaveService;

    public SaveApiController(PlayerSaveService playerSaveService) {
        this.playerSaveService = playerSaveService;
    }

    @GetMapping("/{userId}")
    public ApiResponse<SaveDataResponse> findByUserId(@PathVariable String userId) {
        return ApiResponse.ok(SaveDataResponse.from(playerSaveService.getOrCreate(userId)));
    }

    @PutMapping("/{userId}")
    public ApiResponse<SaveDataResponse> upsert(
            @PathVariable String userId,
            @Valid @RequestBody SaveUpsertRequest request
    ) {
        return ApiResponse.ok(SaveDataResponse.from(playerSaveService.upsert(
                new com.codex.swordgrowth.domain.save.PlayerSaveData(
                        userId,
                        request.currentWeaponId(),
                        request.materials(),
                        request.specialItems(),
                        request.weaponInventory(),
                        request.ownedWeaponIds(),
                        request.unlockedWeaponShop(),
                        request.discoveredWeaponIds(),
                        request.highestReachedWeaponId(),
                        java.time.Instant.now()
                )
        )));
    }
}
