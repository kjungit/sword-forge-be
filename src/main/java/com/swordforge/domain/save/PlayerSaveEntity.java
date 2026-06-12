package com.swordforge.domain.save;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "player_saves")
public class PlayerSaveEntity {

    @Id
    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "current_weapon_id", nullable = false, length = 128)
    private String currentWeaponId;

    @Column(name = "materials_json", nullable = false, columnDefinition = "text")
    private String materialsJson;

    @Column(name = "special_items_json", nullable = false, columnDefinition = "text")
    private String specialItemsJson;

    @Column(name = "weapon_inventory_json", nullable = false, columnDefinition = "text")
    private String weaponInventoryJson;

    @Column(name = "locked_weapon_ids_json", nullable = false, columnDefinition = "text")
    private String lockedWeaponIdsJson;

    @Column(name = "owned_weapon_ids_json", nullable = false, columnDefinition = "text")
    private String ownedWeaponIdsJson;

    @Column(name = "unlocked_weapon_shop_json", nullable = false, columnDefinition = "text")
    private String unlockedWeaponShopJson;

    @Column(name = "discovered_weapon_ids_json", nullable = false, columnDefinition = "text")
    private String discoveredWeaponIdsJson;

    @Column(name = "highest_reached_weapon_id", nullable = false, length = 128)
    private String highestReachedWeaponId;

    @Column(name = "pity_stacks_json", nullable = false, columnDefinition = "text")
    private String pityStacksJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected PlayerSaveEntity() {
    }

    public PlayerSaveEntity(
            String userId,
            String currentWeaponId,
            String materialsJson,
            String specialItemsJson,
            String weaponInventoryJson,
            String lockedWeaponIdsJson,
            String ownedWeaponIdsJson,
            String unlockedWeaponShopJson,
            String discoveredWeaponIdsJson,
            String highestReachedWeaponId,
            String pityStacksJson,
            Instant updatedAt
    ) {
        this.userId = userId;
        this.currentWeaponId = currentWeaponId;
        this.materialsJson = materialsJson;
        this.specialItemsJson = specialItemsJson;
        this.weaponInventoryJson = weaponInventoryJson;
        this.lockedWeaponIdsJson = lockedWeaponIdsJson;
        this.ownedWeaponIdsJson = ownedWeaponIdsJson;
        this.unlockedWeaponShopJson = unlockedWeaponShopJson;
        this.discoveredWeaponIdsJson = discoveredWeaponIdsJson;
        this.highestReachedWeaponId = highestReachedWeaponId;
        this.pityStacksJson = pityStacksJson;
        this.updatedAt = updatedAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getCurrentWeaponId() {
        return currentWeaponId;
    }

    public String getMaterialsJson() {
        return materialsJson;
    }

    public String getSpecialItemsJson() {
        return specialItemsJson;
    }

    public String getWeaponInventoryJson() {
        return weaponInventoryJson;
    }

    public String getLockedWeaponIdsJson() {
        return lockedWeaponIdsJson;
    }

    public String getOwnedWeaponIdsJson() {
        return ownedWeaponIdsJson;
    }

    public String getUnlockedWeaponShopJson() {
        return unlockedWeaponShopJson;
    }

    public String getDiscoveredWeaponIdsJson() {
        return discoveredWeaponIdsJson;
    }

    public String getHighestReachedWeaponId() {
        return highestReachedWeaponId;
    }

    public String getPityStacksJson() {
        return pityStacksJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setCurrentWeaponId(String currentWeaponId) {
        this.currentWeaponId = currentWeaponId;
    }

    public void setMaterialsJson(String materialsJson) {
        this.materialsJson = materialsJson;
    }

    public void setSpecialItemsJson(String specialItemsJson) {
        this.specialItemsJson = specialItemsJson;
    }

    public void setWeaponInventoryJson(String weaponInventoryJson) {
        this.weaponInventoryJson = weaponInventoryJson;
    }

    public void setLockedWeaponIdsJson(String lockedWeaponIdsJson) {
        this.lockedWeaponIdsJson = lockedWeaponIdsJson;
    }

    public void setOwnedWeaponIdsJson(String ownedWeaponIdsJson) {
        this.ownedWeaponIdsJson = ownedWeaponIdsJson;
    }

    public void setUnlockedWeaponShopJson(String unlockedWeaponShopJson) {
        this.unlockedWeaponShopJson = unlockedWeaponShopJson;
    }

    public void setDiscoveredWeaponIdsJson(String discoveredWeaponIdsJson) {
        this.discoveredWeaponIdsJson = discoveredWeaponIdsJson;
    }

    public void setHighestReachedWeaponId(String highestReachedWeaponId) {
        this.highestReachedWeaponId = highestReachedWeaponId;
    }

    public void setPityStacksJson(String pityStacksJson) {
        this.pityStacksJson = pityStacksJson;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
