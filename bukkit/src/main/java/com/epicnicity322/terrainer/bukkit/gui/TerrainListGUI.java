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
import com.epicnicity322.terrainer.core.terrain.WorldTerrain;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TerrainListGUI extends ListGUI<WeakReference<Terrain>> {
    private final @NotNull BiConsumer<InventoryClickEvent, Terrain> onClick;

    public TerrainListGUI(@NotNull Collection<Terrain> terrains, @NotNull String title, @NotNull BiConsumer<InventoryClickEvent, Terrain> onClick) {
        super(terrains.stream().map(WeakReference::new).collect(Collectors.toSet()), title);
        this.onClick = onClick;
    }

    @Override
    protected @NotNull ItemStack item(@NotNull WeakReference<Terrain> obj) {
        Terrain t = obj.get();
        if (t == null) return new ItemStack(Material.AIR);
        return InventoryUtils.getItemStack("Terrain List.GUI." + (t instanceof WorldTerrain ? "World " : "") + "Terrain Item", Configurations.CONFIG.getConfiguration(), TerrainerPlugin.getLanguage(), t.name(), t.id().toString(), t.description(), Double.toString(t.area()), TerrainerPlugin.getPlayerUtil().getOwnerName(t.owner()));
    }

    @Override
    protected @NotNull Consumer<InventoryClickEvent> event(@NotNull WeakReference<Terrain> obj) {
        return event -> {
            Terrain terrain = obj.get();
            if (terrain == null) return; // Do nothing.
            getOnClick().accept(event, terrain);
        };
    }

    /* Since you can't do anything before "super", this is a workaround to get the consumer.
       It's only going to be actually used after someone clicks on the GUI button of a terrain, so by then, the variable should not be null.
       This will be no longer necessary in Java 22, where "super" being the first statement in a constructor is not a requirement. */
    private @NotNull BiConsumer<InventoryClickEvent, Terrain> getOnClick() {
        return onClick;
    }
}
