package com.flandre923.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleCaptureAction;
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.client.battle.BattleCaptureEndPacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import com.cobblemon.mod.common.util.WorldExtensionsKt;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static com.flandre923.JustCatchLevelCap.LEVEL_CAP;
import static com.flandre923.JustCatchLevelCap.SHOW_LEVEL_CAP_MESSAGES;


@Mixin(EmptyPokeBallEntity.class)
public abstract class EmptyPokeBallMixin {

    @Shadow
    private PokemonEntity capturingPokemon;

    @Shadow protected abstract void drop();

    @Inject(method = "onEntityHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/cobblemon/mod/common/entity/pokeball/EmptyPokeBallEntity;attemptCatch(Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)V",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            ),
            cancellable = true)
    private void applyLevelCap(EntityHitResult hitResult, CallbackInfo ci) {
        EmptyPokeBallEntity self = (EmptyPokeBallEntity) (Object) this;

        if (self.getOwner() instanceof ServerPlayerEntity player) {

            if (!player.isCreative() && !self.getPokeBall().getCatchRateModifier().isGuaranteed() && !capturingPokemon.getPokemon().getShiny()) {

                int targetLevel = capturingPokemon.getPokemon().getLevel();

                GameRules gamerules = self.getWorld().getGameRules();
                int levelCap = gamerules.getInt(LEVEL_CAP);
                boolean showMessages = gamerules.getBoolean(SHOW_LEVEL_CAP_MESSAGES);


                if (!capturingPokemon.isBattling()) {
                    if(targetLevel > levelCap) {
                        if (showMessages) {
                            player.sendMessage(Text.translatableWithFallback("catchlevelcap.fail_message", "It seems too strong to be caught...").formatted(Formatting.RED), true);
                        }

                        WorldExtensionsKt.playSoundServer(self.getWorld(), self.getPos(), SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.NEUTRAL, 0.8F, 1F);
                        drop();
                        ci.cancel();
                    }
                } else if(targetLevel > levelCap) {
                    PokemonBattle battle = PlayerExtensionsKt.getBattleState(player).component1();

                    ActiveBattlePokemon hitBattlePokemon = null;
                    if(battle != null) {
                        for (ActiveBattlePokemon battlePokemon : battle.getActor(player).getSide().getOppositeSide().getActivePokemon()) {
                            if (battlePokemon.getBattlePokemon().getEffectedPokemon().getEntity() == capturingPokemon) {
                                hitBattlePokemon = battlePokemon;
                            }
                        }
                    }
                    if(hitBattlePokemon != null) {
                        battle.broadcastChatMessage(Text.translatableWithFallback("catchlevelcap.fail_message_battle", "The ball had no effect...").formatted(Formatting.RED));
                        battle.sendUpdate(new BattleCaptureEndPacket(hitBattlePokemon.getPNX(), false));

                        BattleCaptureAction captureAction = null;
                        for(BattleCaptureAction action : battle.getCaptureActions()) {
                            if(action.getPokeBallEntity() == self) captureAction = action;
                        }
                        if(captureAction != null) {
                            battle.finishCaptureAction(captureAction);
                        }
                    }

                    if (showMessages) {
                        player.sendMessage(Text.translatableWithFallback("catchlevelcap.fail_message", "It seems too strong to be caught...").formatted(Formatting.RED), true);
                    }

                    WorldExtensionsKt.playSoundServer(self.getWorld(), self.getPos(), SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.NEUTRAL, 0.8F, 1F);
                    drop();
                    ci.cancel();
                }
            }
        }
    }

    @Unique
    private int catchlevelcap$getHighestPartyLevel(ServerPlayerEntity player) {
        PlayerPartyStore party = PlayerExtensionsKt.party(player);

        int max = 10;
        for(Pokemon pokemon : party) {
            int level = pokemon.getLevel();
            if(level > max) {
                max = level;
            }
        }
        return max;
    }
}