package com.swordforge.web.api;

import com.swordforge.application.item.SpecialItemCatalogService;
import com.swordforge.application.item.SpecialItemService;
import com.swordforge.common.api.ApiResponse;
import com.swordforge.web.dto.SaveDataResponse;
import com.swordforge.web.dto.SpecialItemPurchaseResponse;
import com.swordforge.web.dto.SpecialItemResponse;
import com.swordforge.web.dto.SpecialItemUseRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
public class SpecialItemApiController {

    private final SpecialItemCatalogService specialItemCatalogService;
    private final SpecialItemService specialItemService;

    public SpecialItemApiController(
            SpecialItemCatalogService specialItemCatalogService,
            SpecialItemService specialItemService
    ) {
        this.specialItemCatalogService = specialItemCatalogService;
        this.specialItemService = specialItemService;
    }

    @GetMapping
    public ApiResponse<List<SpecialItemResponse>> findAll() {
        return ApiResponse.ok(specialItemCatalogService.findAll().stream()
                .map(SpecialItemResponse::from)
                .toList());
    }

    @GetMapping("/{itemId}")
    public ApiResponse<SpecialItemResponse> findById(@PathVariable String itemId) {
        return ApiResponse.ok(SpecialItemResponse.from(specialItemCatalogService.findById(itemId)));
    }

    @PostMapping("/grant")
    public ApiResponse<SaveDataResponse> grant(@Valid @RequestBody SpecialItemUseRequest request) {
        return ApiResponse.ok(SaveDataResponse.from(
                specialItemService.grant(request.userId(), request.itemId(), request.amount())
        ));
    }

    @PostMapping("/consume")
    public ApiResponse<SaveDataResponse> consume(@Valid @RequestBody SpecialItemUseRequest request) {
        return ApiResponse.ok(SaveDataResponse.from(
                specialItemService.consume(request.userId(), request.itemId(), request.amount())
        ));
    }

    @PostMapping("/purchase")
    public ApiResponse<SpecialItemPurchaseResponse> purchase(@Valid @RequestBody SpecialItemUseRequest request) {
        return ApiResponse.ok(SpecialItemPurchaseResponse.from(
                specialItemService.purchase(request.userId(), request.itemId(), request.amount())
        ));
    }
}
