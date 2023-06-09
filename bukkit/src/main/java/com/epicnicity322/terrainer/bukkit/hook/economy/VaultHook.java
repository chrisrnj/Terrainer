package com.epicnicity322.terrainer.bukkit.hook.economy;

import com.epicnicity322.terrainer.bukkit.util.EconomyHandler;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

public final class VaultHook implements EconomyHandler {
    private Economy econ = null;

    public VaultHook() {
        if (!setupEconomy()) throw new UnsupportedOperationException("Vault not found");
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;

        econ = rsp.getProvider();
        return true;
    }

    @Override
    public boolean withdrawPlayer(@NotNull Player player, double amount) {
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }
}
