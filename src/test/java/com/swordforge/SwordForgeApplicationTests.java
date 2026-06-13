package com.swordforge;

import com.swordforge.application.enhance.EnhanceCostService;
import com.swordforge.application.item.ItemPurchasePriceService;
import com.swordforge.application.weapon.WeaponCatalogService;
import com.swordforge.application.shop.WeaponPurchaseCostService;
import com.swordforge.application.shop.WeaponSalePriceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SwordForgeApplicationTests {

    @Autowired
    private WeaponCatalogService weaponCatalogService;

    @Autowired
    private WeaponPurchaseCostService weaponPurchaseCostService;

    @Autowired
    private WeaponSalePriceService weaponSalePriceService;

    @Autowired
    private EnhanceCostService enhanceCostService;

    @Autowired
    private ItemPurchasePriceService itemPurchasePriceService;

    @Test
    void contextLoads() {
    }

    @Test
    void loadsWeaponCatalog() {
        Assertions.assertEquals(27, weaponCatalogService.findAll().size());
        Assertions.assertEquals("normal_01", weaponCatalogService.findAll().get(0).id());
    }

    @Test
    void loadsPurchaseCosts() {
        Assertions.assertEquals(4, weaponPurchaseCostService.findCost("rare_03").get("blue_black_jade_fragment"));
    }

    @Test
    void loadsGoldEconomyPrices() {
        Assertions.assertEquals(36, weaponSalePriceService.investedGoldFor("rare_03"));
        Assertions.assertEquals(72, weaponSalePriceService.findPrice("rare_03"));
        Assertions.assertTrue(weaponSalePriceService.findPrice("rare_03") >= weaponSalePriceService.investedGoldFor("rare_03") * 2);
        Assertions.assertEquals(5, enhanceCostService.findCost("normal_10"));
        Assertions.assertEquals(40, itemPurchasePriceService.findPrice("enhance_rate_boost_5"));
    }
}
