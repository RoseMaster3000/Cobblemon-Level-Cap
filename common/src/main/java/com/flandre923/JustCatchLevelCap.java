package com.flandre923;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.GameRules;

public final class JustCatchLevelCap {
//    public static final String MOD_ID = "example_mod";
    public static final String MOD_ID = "just_catch_level_cap";


    public static final GameRules.Key<GameRules.IntRule> LEVEL_CAP;
    public static final GameRules.Key<GameRules.IntRule> EXTERNAL_LEVEL_CAP;
    public static final GameRules.Key<GameRules.BooleanRule> SHOW_LEVEL_CAP_MESSAGES;
    // 修改游戏规则的描述，使其基于道馆数量


    static {
        LEVEL_CAP = registerIntRule("levelCap", GameRules.Category.MISC, 5);
        EXTERNAL_LEVEL_CAP = registerIntRule("externalLevelCap", GameRules.Category.MISC, 5);
        SHOW_LEVEL_CAP_MESSAGES = registerBooleanRule("showLevelCapMessages", GameRules.Category.MISC, true);
    }

    public static void init() {
        // Write common init code here.
    }

    @ExpectPlatform
    public static GameRules.Key<GameRules.IntRule> registerIntRule(String name, GameRules.Category category, int initialValue) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static GameRules.Key<GameRules.BooleanRule> registerBooleanRule(String name, GameRules.Category category, boolean initialValue) {
        throw new AssertionError();
    }
}
