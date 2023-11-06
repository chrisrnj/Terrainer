/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2023 Christiano Rangel
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

package com.epicnicity322.terrainer.core.terrain;

import com.epicnicity322.terrainer.core.Coordinate;
import com.epicnicity322.terrainer.core.WorldCoordinate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

public final class WorldTerrain extends Terrain {
    @Serial
    private static final long serialVersionUID = 6844639450981466893L;

    public WorldTerrain(@NotNull UUID world, @NotNull String name) {
        super(new Coordinate(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE), new Coordinate(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE),
                world, world, name, null, ZonedDateTime.now(), null, 0, null, null, null);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote WorldTerrains cover the whole world and overlaps all terrains. In order to not cause issues with the
     * overlapping check, {@link #isOverlapping(Terrain)} for world terrains always return false.
     */
    @Override
    public boolean isOverlapping(@NotNull Terrain terrain) {
        return false;
    }

    @Override
    public boolean isWithin(double x, double y, double z) {
        return true;
    }

    @Override
    public boolean isWithin(@NotNull WorldCoordinate worldCoordinate) {
        return worldCoordinate.world().equals(this.world);
    }

    @Override
    public boolean isWithinChunk(int chunkX, int chunkZ) {
        return true;
    }

    @Override
    public boolean usesDefaultFlagValues() {
        return false;
    }

    @Override
    public void setOwner(@Nullable UUID owner) {
        throw new UnsupportedOperationException("World terrains can not be owned.");
    }

    @Override
    public void setMaxDiagonal(@NotNull Coordinate second) {
        throw new UnsupportedOperationException("World terrains can not have their boundaries changed.");
    }

    @Override
    public void setMinDiagonal(@NotNull Coordinate first) {
        throw new UnsupportedOperationException("World terrains can not have their boundaries changed.");
    }

    @Override
    public double area() {
        return Double.MAX_VALUE;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldTerrain terrain = (WorldTerrain) o;
        return id.equals(terrain.id) && world.equals(terrain.world) && minDiagonal.equals(terrain.minDiagonal) && maxDiagonal.equals(terrain.maxDiagonal) && Objects.equals(owner, terrain.owner) && creationDate.equals(terrain.creationDate) && name.equals(terrain.name) && Objects.equals(description, terrain.description) && priority == terrain.priority && moderators.equals(terrain.moderators) && members.equals(terrain.members) && flags.equals(terrain.flags);
    }

    @Override
    public @NotNull String toString() {
        return "WorldTerrain{" + "id=" + id + ", world=" + world + ", creationDate=" + creationDate + ", name='" + name + "', description='" + description + "', priority=" + priority + ", moderators=" + moderators + ", members=" + members + ", flags=" + flags + '}';
    }
}
