package com.epicnicity322.terrainer.bukkit.util;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface EconomyHandler {
    boolean withdrawPlayer(@NotNull Player player, double amount);
}
