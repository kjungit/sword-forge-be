package com.swordforge.web.dto;

import com.swordforge.domain.item.SpecialItemDefinition;

public record SpecialItemResponse(
        String id,
        String nameKo,
        String scope,
        String effect,
        String description
) {
    public static SpecialItemResponse from(SpecialItemDefinition definition) {
        return new SpecialItemResponse(
                definition.id(),
                definition.nameKo(),
                definition.scope(),
                definition.effect(),
                definition.description()
        );
    }
}

