package com.codex.swordgrowth.web.api;

import com.codex.swordgrowth.application.shop.WeaponPurchaseService;
import com.codex.swordgrowth.common.api.ApiResponse;
import com.codex.swordgrowth.web.dto.WeaponPurchaseRequest;
import com.codex.swordgrowth.web.dto.WeaponPurchaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shop")
public class ShopApiController {

    private final WeaponPurchaseService weaponPurchaseService;

    public ShopApiController(WeaponPurchaseService weaponPurchaseService) {
        this.weaponPurchaseService = weaponPurchaseService;
    }

    @GetMapping("/preview/{weaponId}")
    public ApiResponse<WeaponPurchaseService.PurchasePreview> preview(@PathVariable String weaponId) {
        return ApiResponse.ok(weaponPurchaseService.preview(weaponId));
    }

    @PostMapping("/purchase")
    public ApiResponse<WeaponPurchaseResponse> purchase(@Valid @RequestBody WeaponPurchaseRequest request) {
        return ApiResponse.ok(WeaponPurchaseResponse.from(
                weaponPurchaseService.purchase(request.userId(), request.weaponId())
        ));
    }
}

