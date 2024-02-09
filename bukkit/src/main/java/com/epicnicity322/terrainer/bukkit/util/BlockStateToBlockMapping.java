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

package com.epicnicity322.terrainer.bukkit.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.AbstractList;
import java.util.List;
import java.util.stream.Collectors;

public final class BlockStateToBlockMapping {
    private BlockStateToBlockMapping() {
    }

    public static @NotNull Block mapCoordinatesOnly(@NotNull BlockState state) {
        return (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[]{Block.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getX" -> state.getX();
            case "getY" -> state.getY();
            case "getZ" -> state.getZ();
            case "equals" -> {
                if (args.length == 1) {
                    Object otherObject = args[0];
                    if (!(otherObject instanceof Block b)) yield false;
                    yield b.getX() == state.getX() && b.getY() == state.getY() && b.getZ() == state.getZ();
                }
                yield false;
            }
            case "hashCode" -> state.hashCode();
            case "toString" -> "TerrainerCustomOnlyCoordinatesBlock";
            default -> {
                // Returning default values for irrelevant methods.
                Class<?> returnType = method.getReturnType();
                if (returnType.isPrimitive()) {
                    // Using Array class to our advantage to get primitive default values.
                    yield Array.get(Array.newInstance(returnType, 1), 0);
                } else yield null;
            }
        });
    }

    /**
     * Wraps the list so {@link List#get(int)} returns a {@link Block} instead of {@link BlockState}. The only methods
     * that reflect the provided list are: {@link List#get(int)}, {@link List#size()}, {@link List#remove(int)} and {@link List#toArray()}.
     *
     * @param states The list to wrap.
     * @return The list in which {@link List#get(int)} returns a block.
     */
    public static @NotNull List<Block> wrapBlockStates(@NotNull List<BlockState> states) {
        List<Block> blocks = states.stream().map(BlockStateToBlockMapping::mapCoordinatesOnly).collect(Collectors.toList());

        class ListWrapper extends AbstractList<Block> {
            @Override
            public Block get(int index) {
                return blocks.get(index);
            }

            @Override
            public int size() {
                return states.size();
            }

            @Override
            public Block remove(int index) {
                states.remove(index);
                blocks.remove(index);
                return null;
            }

            @NotNull
            @Override
            public Object @NotNull [] toArray() {
                return blocks.toArray();
            }
        }

        return new ListWrapper();
    }
}
