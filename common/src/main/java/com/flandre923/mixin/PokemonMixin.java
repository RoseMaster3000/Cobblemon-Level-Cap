package com.flandre923.mixin;

import com.cobblemon.mod.common.api.moves.MoveTemplate; // Required for the Set type argument
import com.cobblemon.mod.common.api.pokemon.experience.ExperienceSource;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.AddExperienceResult;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.flandre923.JustCatchLevelCap;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections; // Required for Collections.emptySet()
import java.util.Set;         // Required for Set type

@Mixin(Pokemon.class)
public abstract class PokemonMixin {
    /**
     * Mixin for the version of addExperience that includes a player context.
     * Checks level cap before adding XP.
     */
    @Inject(
            method = "addExperienceWithPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;Lcom/cobblemon/mod/common/api/pokemon/experience/ExperienceSource;I)Lcom/cobblemon/mod/common/pokemon/AddExperienceResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventXpGainAtCapWithPlayer(
            ServerPlayerEntity player,
            ExperienceSource source,
            int xp,
            CallbackInfoReturnable<AddExperienceResult> cir)
    {
        Pokemon self = (Pokemon) (Object) this;
        World world = player.getWorld();

        if (!world.isClient()) {
            GameRules gameRules = world.getGameRules();
            int levelCap = gameRules.getInt(JustCatchLevelCap.LEVEL_CAP);
            int currentLevel = self.getLevel();

            if (currentLevel >= levelCap) {
                // Construct the AddExperienceResult correctly representing no change
                AddExperienceResult noChangeResult = new AddExperienceResult(
                        currentLevel,           // oldLevel
                        levelCap,               // newLevel (reset to level cap)
                        Collections.emptySet(), // newMoves (empty Set<MoveTemplate>)
                        0                       // experienceAdded = 0
                );
                cir.setReturnValue(noChangeResult); // Prevent XP gain and return the "no change" result
            }
        }
    }

    /**
     * Mixin for the version of addExperience without a direct player context.
     * Checks level cap before adding XP, relying on the Pokemon's entity for world context.
     */
    @Inject(
            method = "addExperience(Lcom/cobblemon/mod/common/api/pokemon/experience/ExperienceSource;I)Lcom/cobblemon/mod/common/pokemon/AddExperienceResult;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false // Add this ONLY for testing in deobfuscated dev env
    )
    private void preventXpGainAtCapNoPlayer(
            ExperienceSource source,
            int xp,
            CallbackInfoReturnable<AddExperienceResult> cir)
    {
        Pokemon self = (Pokemon) (Object) this;
        PokemonEntity entity =  self.getEntity();

        if (entity != null) {
            World world = entity.getWorld();

            if (!world.isClient()) {
                GameRules gameRules = world.getGameRules();
                int levelCap = gameRules.getInt(JustCatchLevelCap.LEVEL_CAP);
                int currentLevel = self.getLevel();

                if (currentLevel >= levelCap) {
                    // Construct the AddExperienceResult correctly representing no change
                    AddExperienceResult noChangeResult = new AddExperienceResult(
                            currentLevel,           // oldLevel
                            levelCap,               // newLevel (reset to level cap)
                            Collections.emptySet(), // newMoves (empty Set<MoveTemplate>)
                            0                       // experienceAdded = 0
                    );
                    cir.setReturnValue(noChangeResult); // Prevent XP gain and return the "no change" result
                }
            }
        }
    }
}