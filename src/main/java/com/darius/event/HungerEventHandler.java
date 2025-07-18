package com.darius.event;


import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Mod.EventBusSubscriber(modid = "tummyhurtmod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HungerEventHandler {

    private static final Map<UUID, Boolean> fullPlayers = new HashMap<>();
    private static final int FULL_EFFECT_DURATION = 6000;

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        ItemStack food = event.getItem();
        if (player.level().isClientSide()) {
            return;
        }



        Consumable consumable = food.get(DataComponents.CONSUMABLE);
        if (consumable != null) {
            if (consumable.consumeSeconds() > 1.0f) {
                handleFoodConsumption(player, food);
            }
        }
    }

    private static void handleFoodConsumption(Player player, ItemStack food) {
        FoodData foodData = player.getFoodData();
        FoodProperties foodProperties = food.get(DataComponents.FOOD);
        UUID playerId = player.getUUID();

        boolean isCurrentlyFull = fullPlayers.getOrDefault(playerId, false);

        if (foodData.getFoodLevel() >= 20) {
            if (isCurrentlyFull) {
                applyVomitEffects(player);
                fullPlayers.remove(playerId);

                player.displayClientMessage(
                        Component.literal("You ate too much and got sick...")
                                .withStyle(style -> style.withColor(0xFF6B6B)),
                        true
                );
            } else {
                applySaturationBonus(player, foodProperties);
                fullPlayers.put(playerId, true);

                player.addEffect(new MobEffectInstance(
                        MobEffects.SATURATION,
                        FULL_EFFECT_DURATION,
                        0,
                        false,
                        true,
                        true
                ));

                player.displayClientMessage(
                        Component.literal("You feel well fed")
                                .withStyle(style -> style.withColor(0x90EE90)),
                        true
                );
            }
        }
    }
    private static void applySaturationBonus(Player player, FoodProperties foodProperties) {
        FoodData foodData = player.getFoodData();

        if (foodProperties != null) {
            float maxSaturation = foodData.getFoodLevel();
            float targetSaturation = maxSaturation * 0.8f;

            float currentSaturation = foodData.getSaturationLevel();
            float saturationToAdd = Math.max(0, targetSaturation - currentSaturation);

            if (saturationToAdd > 0) {
                foodData.setSaturation(currentSaturation + saturationToAdd);

                System.out.println("Saturacion aplicada: "+saturationToAdd);
                System.out.println("Saturacion actual: "+foodData.getSaturationLevel());
            }
        }
    }

    private static void applyVomitEffects(Player player) {
        FoodData foodData = player.getFoodData();

        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 300, 1));
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 400, 4));

        foodData.setFoodLevel(0);
        foodData.setSaturation(0.0f);
    }

    public static void cleanupPlayer(UUID playerId) {
        fullPlayers.remove(playerId);
    }
}
