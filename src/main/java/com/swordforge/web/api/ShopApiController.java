package com.swordforge.web.api;

import com.swordforge.application.shop.WeaponPurchaseService;
import com.swordforge.common.api.ApiResponse;
import com.swordforge.web.dto.WeaponPurchaseRequest;
import com.swordforge.web.dto.WeaponPurchaseResponse;
import com.swordforge.web.dto.WeaponSaleRequest;
import com.swordforge.web.dto.WeaponSaleResponse;
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

    @GetMapping("/sell-preview/{weaponId}")
    public ApiResponse<WeaponPurchaseService.SalePreview> salePreview(@PathVariable String weaponId) {
        return ApiResponse.ok(weaponPurchaseService.salePreview(weaponId));
    }

    @PostMapping("/purchase")
    public ApiResponse<WeaponPurchaseResponse> purchase(@Valid @RequestBody WeaponPurchaseRequest request) {
        return ApiResponse.ok(WeaponPurchaseResponse.from(
                weaponPurchaseService.purchase(request.userId(), request.weaponId())
        ));
    }

    @PostMapping("/sell")
    public ApiResponse<WeaponSaleResponse> sell(@Valid @RequestBody WeaponSaleRequest request) {
        return ApiResponse.ok(WeaponSaleResponse.from(
                weaponPurchaseService.sell(request.userId(), request.weaponId(), request.amount())
        ));
    }
}
