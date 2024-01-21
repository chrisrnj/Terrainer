/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2024 Christiano Rangel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.epicnicity322.terrainer.bukkit.gui;

import com.epicnicity322.epicpluginlib.bukkit.util.InventoryUtils;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Consumer;

public class TerrainListGUI extends ListGUI<Terrain> {
    public TerrainListGUI(@NotNull Collection<Terrain> terrains, @NotNull String title) {
        super(terrains, title);
    }

    @Override
    protected @NotNull ItemStack item(@NotNull Terrain obj) {
        return InventoryUtils.getItemStack("Terrain List.GUI.Terrain Item", Configurations.CONFIG.getConfiguration(), TerrainerPlugin.getLanguage(), obj.name(), obj.id().toString(), obj.description(), Double.toString(obj.area()), TerrainerPlugin.getPlayerUtil().getOwnerName(obj.owner()));
    }

    @Override
    protected @NotNull Consumer<InventoryClickEvent> event(@NotNull Terrain obj) {
        return event -> {
        };
    }
}
