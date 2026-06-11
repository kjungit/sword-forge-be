package com.codex.swordgrowth;

import com.codex.swordgrowth.application.weapon.WeaponCatalogService;
import com.codex.swordgrowth.application.shop.WeaponPurchaseCostService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SwordGrowthApplicationTests {

    @Autowired
    private WeaponCatalogService weaponCatalogService;

    @Autowired
    private WeaponPurchaseCostService weaponPurchaseCostService;

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
        Assertions.assertEquals(90, weaponPurchaseCostService.findCost("rare_03").get("blue_black_jade_fragment"));
    }
}
