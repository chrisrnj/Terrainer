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

package com.epicnicity322.terrainer.core.terrain;

import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.location.Chunk;
import com.epicnicity322.terrainer.core.location.Coordinate;
import com.epicnicity322.terrainer.core.location.WorldCoordinate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class WorldTerrain extends Terrain {
    static final @NotNull Coordinate min = new Coordinate(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    static final @NotNull Coordinate max = new Coordinate(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    @Serial
    private static final long serialVersionUID = -4194791547407605454L;

    public WorldTerrain(@NotNull UUID world, @NotNull String name) {
        // The ID of the terrain is the same as of the world's.
        super(min, max, world, world, name, null, ZonedDateTime.now(), null, Integer.MAX_VALUE, null, null, null, null);
    }

    WorldTerrain(@NotNull Terrain terrain, @NotNull String name) {
        // The ID of the terrain is the same as of the world's.
        super(min, max, terrain.world, terrain.world, name, terrain.description, null, terrain.creationDate, terrain.priority, terrain.moderators, terrain.members, terrain.flags, terrain.memberFlags);
    }

    @Override
    protected @NotNull Set<Chunk> findChunks(@NotNull Coordinate minDiagonal, @NotNull Coordinate maxDiagonal) {
        return Collections.emptySet();
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

    /**
     * {@inheritDoc}
     *
     * @implNote WorldTerrains have null flag values when they are unset.
     */
    @Override
    public boolean usesDefaultFlagValues() {
        return false;
    }

    @Override
    public void setName(@Nullable String name) {
        throw new UnsupportedOperationException("World terrains can not be renamed.");
    }

    @Override
    public void setOwner(@Nullable UUID owner) {
        throw new UnsupportedOperationException("World terrains can not be owned.");
    }

    @Override
    public void setDiagonals(@NotNull Coordinate first, @NotNull Coordinate second) {
        throw new UnsupportedOperationException("World terrains can not have their boundaries changed.");
    }

    @Override
    public @NotNull String description() {
        if (description == null) return Terrainer.lang().get("Description.World Terrain").replace("<world>", name());
        return description;
    }

    @Override
    public double area() {
        return 3_599_996_160_001_024.0; // Very big number.
    }

    @Override
    public boolean deepEquals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldTerrain terrain = (WorldTerrain) o;
        return id.equals(terrain.id) && world.equals(terrain.world) && creationDate.equals(terrain.creationDate) && name.equals(terrain.name) && Objects.equals(description, terrain.description) && priority == terrain.priority && moderators.equals(terrain.moderators) && members.equals(terrain.members) && flags.equals(terrain.flags) && memberFlags.equals(terrain.memberFlags);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldTerrain terrain = (WorldTerrain) o;
        return id.equals(terrain.id);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "WorldTerrain{" + "id=" + id + ", world=" + world + ", creationDate=" + creationDate + ", name='" + name + "', description='" + description + "', priority=" + priority + ", moderators=" + moderators + ", members=" + members + ", flags=" + flags + ", memberFlags=" + memberFlags + '}';
    }
}
