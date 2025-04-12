package com.flandre923;

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
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.flandre923.JustCatchLevelCap.LEVEL_CAP;
import static com.flandre923.JustCatchLevelCap.SHOW_LEVEL_CAP_MESSAGES;

public class LevelCapHelper {

    /**
     * 获取玩家队伍中等级最高的宝可梦的等级。
     *
     * @param player 服务器玩家实体
     * @return 最高等级，如果队伍为空则返回 0
     */
    public static int getHighestPartyLevel(ServerPlayerEntity player) {
        PlayerPartyStore party = PlayerExtensionsKt.party(player);
        int maxLevel = 0;
        for (Pokemon pokemon : party) {
            if (pokemon != null) { // 检查宝可梦是否为 null
                int level = pokemon.getLevel();
                if (level > maxLevel) {
                    maxLevel = level;
                }
            }
        }
        return maxLevel;
    }

    /**
     * 应用等级上限检查逻辑，决定是否取消宝可梦的捕捉。
     *
     * @param self             空的精灵球实体
     * @param player           投掷精灵球的玩家
     * @param capturingPokemon 被捕捉的宝可梦实体
     * @param ci               回调信息，用于取消捕捉事件
     * @return `true` 如果应该掉落精灵球, `false` 反之
     */
    public static boolean applyLevelCapCheck(EmptyPokeBallEntity self, ServerPlayerEntity player, PokemonEntity capturingPokemon, CallbackInfo ci) {
        if (player == null || capturingPokemon == null) {
            return false; // 避免空指针异常, 默认不掉落，安全起见可以根据实际情况调整为 true
        }

        if (!player.isCreative() && !self.getPokeBall().getCatchRateModifier().isGuaranteed() && !capturingPokemon.getPokemon().getShiny()) {

            int playerLevel = LevelCapHelper.getHighestPartyLevel(player);
            int targetLevel = capturingPokemon.getPokemon().getLevel();

            GameRules gamerules = self.getWorld().getGameRules();
            int levelCap = gamerules.getInt(LEVEL_CAP);
            int externalLevelCap = gamerules.getInt(LEVEL_CAP);
            boolean showMessages = gamerules.getBoolean(SHOW_LEVEL_CAP_MESSAGES);

            if (!capturingPokemon.isBattling()) {
                // 非战斗状态下的等级上限检查
                if (targetLevel > playerLevel + externalLevelCap) {
                    return handleLevelCapFailure(self, player, showMessages, externalLevelCap < levelCap && targetLevel < playerLevel + levelCap, false, ci);
                }
            } else {
                // 战斗状态下的等级上限检查
                if (targetLevel > playerLevel + levelCap) {
                    return handleLevelCapFailureInBattle(self, player, capturingPokemon, showMessages, ci);
                }
            }
        }
        return false; // 默认情况，不掉落
    }

    /**
     * 处理捕捉失败的情况，包括发送消息、播放声音和掉落精灵球。
     * 适用于非战斗状态下的等级上限失败。
     *
     * @param self             空的精灵球实体
     * @param player           投掷精灵球的玩家
     * @param showMessages     是否显示等级上限消息
     * @param isExternalCap    是否是外部等级上限导致的失败
     * @param inBattle         是否在战斗中 (此处应为 false)
     * @param ci               回调信息，用于取消捕捉事件
     * @return `true` 表示已经处理了失败情况 (总是返回 true)
     */
    private static boolean handleLevelCapFailure(EmptyPokeBallEntity self, ServerPlayerEntity player, boolean showMessages, boolean isExternalCap, boolean inBattle, CallbackInfo ci) {
        if (showMessages) {
            if (isExternalCap) {
                player.sendMessage(Text.translatableWithFallback("catchlevelcap.fail_message_external", "The ball bounced off... try battling first?").formatted(Formatting.RED), true);
            } else {
                player.sendMessage(Text.translatableWithFallback("catchlevelcap.fail_message", "It seems too strong to be caught...").formatted(Formatting.RED), true);
            }
        }
        WorldExtensionsKt.playSoundServer(self.getWorld(), self.getPos(), SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.NEUTRAL, 0.8F, 1F);
        // 注意这里不再调用 self.drop(), 而是让 Mixin 根据返回值来调用
        ci.cancel(); // 仍然需要取消捕捉事件
        return true; // 返回 true 表示需要掉落精灵球
    }

    /**
     * 处理战斗中捕捉失败的情况，包括发送战斗消息、更新战斗状态、播放声音和掉落精灵球。
     *
     * @param self             空的精灵球实体
     * @param player           投掷精灵球的玩家
     * @param capturingPokemon 被捕捉的宝可梦实体
     * @param showMessages     是否显示等级上限消息
     * @param ci               回调信息，用于取消捕捉事件
     * @return `true` 表示已经处理了失败情况 (总是返回 true)
     */
    private static boolean handleLevelCapFailureInBattle(EmptyPokeBallEntity self, ServerPlayerEntity player, PokemonEntity capturingPokemon, boolean showMessages, CallbackInfo ci) {
        PokemonBattle battle = PlayerExtensionsKt.getBattleState(player).component1();

        ActiveBattlePokemon hitBattlePokemon = null;
        if (battle != null) {
            for (ActiveBattlePokemon battlePokemon : battle.getActor(player).getSide().getOppositeSide().getActivePokemon()) {
                if (battlePokemon.getBattlePokemon().getEffectedPokemon().getEntity() == capturingPokemon) {
                    hitBattlePokemon = battlePokemon;
                }
            }
        }
        if (hitBattlePokemon != null) {
            battle.broadcastChatMessage(Text.translatableWithFallback("catchlevelcap.fail_message_battle", "The ball had no effect...").formatted(Formatting.RED));
            battle.sendUpdate(new BattleCaptureEndPacket(hitBattlePokemon.getPNX(), false));

            BattleCaptureAction captureAction = null;
            for (BattleCaptureAction action : battle.getCaptureActions()) {
                if (action.getPokeBallEntity() == self) captureAction = action;
            }
            if (captureAction != null) {
                battle.finishCaptureAction(captureAction);
            }
        }

        if (showMessages) {
            player.sendMessage(Text.translatableWithFallback("catchlevelcap.fail_message", "It seems too strong to be caught...").formatted(Formatting.RED), true);
        }

        WorldExtensionsKt.playSoundServer(self.getWorld(), self.getPos(), SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.NEUTRAL, 0.8F, 1F);
        // 注意这里不再调用 self.drop(), 而是让 Mixin 根据返回值来调用
        ci.cancel(); // 仍然需要取消捕捉事件
        return true; // 返回 true 表示需要掉落精灵球
    }
}