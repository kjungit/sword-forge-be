package com.codex.swordgrowth.web.api;

import com.codex.swordgrowth.application.item.SpecialItemCatalogService;
import com.codex.swordgrowth.application.item.SpecialItemService;
import com.codex.swordgrowth.common.api.ApiResponse;
import com.codex.swordgrowth.web.dto.SaveDataResponse;
import com.codex.swordgrowth.web.dto.SpecialItemResponse;
import com.codex.swordgrowth.web.dto.SpecialItemUseRequest;
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
}
