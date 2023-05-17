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

package com.epicnicity322.terrainer.core.config;

import com.epicnicity322.epicpluginlib.core.EpicPluginLib;
import com.epicnicity322.epicpluginlib.core.config.ConfigurationHolder;
import com.epicnicity322.epicpluginlib.core.config.ConfigurationLoader;
import com.epicnicity322.terrainer.core.TerrainerVersion;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class Configurations {
    public static final @NotNull Path DATA_FOLDER = Path.of(EpicPluginLib.Platform.getPlatform() == EpicPluginLib.Platform.BUKKIT ? "plugins" : "config", "Terrainer");

    public static final ConfigurationHolder CONFIG = new ConfigurationHolder(DATA_FOLDER.resolve("config.yml"), """
            Version: '#VERSION#'
                        
            # Available: EN_US, PT_BR
            Language: EN_US
                        
            # The minimum of area in blocks a terrain must have to be claimed.
            Min Area: 9
                        
            # The maximum length a description or leave message of a terrain can have.
            Max Description Length: 30
                        
            # You can set a group's default block limits through this setting.
            # You can add you own limits and use them in groups through the permission 'terrainer.limit.blocks.<group>'
            # For unlimited blocks, use 'terrainer.bypass.limit.blocks' permission.
            Block Limits:
              Default: 450 # Permission: terrainer.limit.blocks.Default
              VIP: 25000
              Staff: 75000
            # Additionally, you can use the command '/tr limit <player> blocks give <amount>' to give more blocks to a specific player.
                        
            # You can set a group's default claim limits through this setting.
            # You can add you own limits and use them in groups through the permission 'terrainer.limit.claims.<group>'
            # For unlimited claims, use 'terrainer.bypass.limit.claims' permission.
            Claim Limits:
              Default: 2 # Permission: terrainer.limit.claims.Default
              VIP: 10
              Staff: 30
            # Additionally, you can use the command '/tr limit <player> claims give <amount>' to give more claims to a specific player.
                        
            Input:
              # Whether an anvil should be used to get inputs from the player. If disabled, the input will be get from chat.
              Anvil GUI:
                Enabled: true
                Material: FEATHER
                Glowing: true
              # The max time in ticks to get inputs from chat, if the player can't answer the input within this time, it will be cancelled.
              Chat Interval: 200
                        
            # The commands to execute when a player joins the server in a terrain, and the TerrainEnterEvent is cancelled.
            # An example of this happening is if the player had permission to enter a terrain which ENTRY flag is denied,
            #and when they left the server their permission to the terrain was revoked. So when they join later,
            #TerrainEnterEvent will be cancelled because the player is not allowed, then these commands will execute.
            # This is not exclusive to ENTRY flag denied, any hooking plugin might cancel TerrainEnterEvent in any occasion.
            # It is suggested to teleport the player to a place where entry is always allowed, either that or kick the
            #player from the server.
            # Use %p for the player's name, and %t for the terrain's ID.
            Commands When TerrainEnterEvent Cancelled on Join or Create:
            - 'spawn %p'
            #- 'kick %p You tried to join in a terrain where ENTRY is denied.'
                        
            List:
              Chat:
                Max Per Page: 20
              GUI Items:
                Next Page:
                  Material: SPECTRAL_ARROW
                  Glowing: false
                Previous Page:
                  Material: SPECTRAL_ARROW
                  Glowing: false
                        
            Terrain List:
              GUI:
                Terrain Item:
                  Material: GRASS_BLOCK
                  Glowing: false
                        
            Flags:
              # GUI Items for the Flag Management GUI.
              Values:
                Armor Stands:
                  Material: ARMOR_STAND
                Build:
                  Material: BRICKS
                Build Vehicles:
                  Material: MINECART
                Buttons:
                  Material: LEVER
                Containers:
                  Material: CHEST
                Dispensers:
                  Material: DISPENSER
                Doors:
                  Material: OAK_DOOR
                Effects:
                  Material: BEACON
                Enemy Harm:
                  Material: SHIELD
                Enter:
                  Material: LIME_WOOL
                Enter Vehicles:
                  Material: OAK_BOAT
                Entity Harm:
                  Material: PORKCHOP
                Entity Interactions:
                  Material: LEAD
                Explosion Damage:
                  Material: TNT
                Farmland Trample:
                  Material: FARMLAND
                Fire Damage:
                  Material: FIRE_CHARGE
                Interactions:
                  Material: FLINT_AND_STEEL
                Item Drop:
                  Material: DROPPER
                Item Frames:
                  Material: ITEM_FRAME
                Item Pickup:
                  Material: HOPPER
                Leaf Decay:
                  Material: OAK_LEAVES
                Leave:
                  Material: RED_WOOL
                Leave Message:
                  Material: PAPER
                Liquid Flow:
                  Material: WATER_BUCKET
                Message Location:
                  Material: PAPER
                Mob Spawn:
                  Material: ZOMBIE_HEAD
                Mods Can Edit Flags:
                  Material: WRITABLE_BOOK
                Mods Can Manage Mods:
                  Material: WOODEN_AXE
                Outside Dispensers:
                  Material: DISPENSER
                Outside Pistons:
                  Material: STICKY_PISTON
                Outside Projectiles:
                  Material: ARROW
                Pistons:
                  Material: PISTON
                Pressure Plates:
                  Material: OAK_PRESSURE_PLATE
                Projectiles:
                  Material: BOW
                PvP:
                  Material: DIAMOND_SWORD
                Sign Click:
                  Material: OAK_SIGN
                Sign Edit:
                  Material: DARK_OAK_SIGN
                Spawners:
                  Material: SPAWNER
                Vulnerability:
                  Material: TOTEM_OF_UNDYING""".replace("#VERSION#", TerrainerVersion.VERSION_STRING));
    public static final ConfigurationHolder LANG_EN_US = new ConfigurationHolder(DATA_FOLDER.resolve("Language").resolve("Language EN-US.yml"), """
            Version: '#VERSION#'
                        
            General:
              Player Not Found: '&cA player with name "&7<value>&c" could not be found.'
              Prefix: '&8[&cTerrains&8] '
              No Permission: '&4You don''t have permission to do this.'
              No Permission Others: '&4You can not do this with other players.'
              Not A Number: '&cThe value "&7<value>&c" is not a number.'
              World Not Found: '&cA world with name "&7<value>&c" was not found.'
                        
            # Translations of command arguments.
            Commands:
              Confirm:
                Confirm: 'confirm'
                List: 'list'
                        
            Confirm:
              Arguments: '[<id>|list] [page]'
              Error:
                Multiple: '&7You have more than one confirmation pending, use &a&n<command>&7 to see the list.'
                Not Found: '&4A confirmation with that ID was not found.'
                Nothing Pending: '&4You have nothing to confirm.'
                Run: '&4Something went wrong while confirming this request.'
              Header: '&eList of pending confirmations (Page &7<page>&e of &7<total>&e):'
              Entry: '&a<id>: &f<description>'
              Footer: '&eTo view more confirmations use &a&n<command>'
                        
            Create:
              Error:
                Different Worlds: '&cTerrain could not be created because the selections are in different worlds!'
                No Block Limit: '&cThis terrain has an area of &7<area>&c blocks and you have only &7<used>&c blocks left! To increase your block limit, buy in the &7&n/tr shop&c.'
                No Claim Limit: '&cYou can''t create more than &7<max>&c terrains! To increase your claim limit, buy in the &7&n/tr shop&c.'
                Not Selected: '&eYou need to make a selection before creating a terrain. You can select with the command &f&n/<label> pos1 and pos2&e or with the selection wand: &f&n/<label> wand&e.'
                Overlap: '&cThis terrain would overlap &7<other>&c terrain! You can only overlap your own terrains.'
                Too Small: '&cArea too small! Terrains must have at least &7<min>&c blocks.'
                Unknown: '&cAn unknown error occurred while creating this terrain.'
              Success: '&2Terrain ''&a<name>&2'' claimed successfully! Used block limit: &7<used>&f/&7<max>'
              Define: '&2Terrain ''&a<name>&2'' defined successfully with all protection flags!'
                        
            Delete:
              Confirmation: '&7Are you sure you want to delete &e<name>&7? Please confirm the deletion with &f&n/<label> <label2>'
              Confirmation Description: 'Delete <name>'
              Success: '&e<name>&a was deleted successfully!'
                        
            Protections:
              Armor Stands: '<cooldown=2000> &4You''re not allowed to use armor stands here.'
              Build: '<cooldown=2000> &4You''re not allowed to build here.'
              Build Vehicles: '<cooldown=2000> &4You''re not allowed to break or place vehicles here.'
              Buttons: '<cooldown=2000> &4You''re not allowed to use buttons and levers here.'
              Containers: '<cooldown=2000> &4You''re not allowed to open containers here.'
              Doors: '<cooldown=2000> &4You''re not allowed to open doors or gates here.'
              Drop: '<cooldown=2000> &4You''re not allowed to drop items here.'
              Enemy Harm: '<cooldown=2000> &4You''re not allowed to harm enemy entities here.'
              Enter: '<cooldown=2000> &4You''re not allowed to enter.'
              Enter Vehicles: '<cooldown=2000> &4You''re not allowed to enter vehicles here.'
              Entity Interactions: '<cooldown=2000> &4You''re not allowed to interact with entities here.'
              Farmland Trampling: '<cooldown=2000> &4You''re not allowed to trample farmland here.'
              Harm: '<cooldown=2000> &4You''re not allowed to harm entities here.'
              Interactions: '<cooldown=2000> &4You''re not allowed to use this here.'
              Item Frames: '<cooldown=2000> &4You''re not allowed to use item frames here.'
              Join Loading Server Message: |-
                &cServer is loading, please try joining in a few seconds.
              # Use <default> for the default spigot shutdown message.
              Kick Message: |-
                &4<default>
                &cYou were kicked early to prevent damages to terrains.
              Leave: '<cooldown=2000> &4You''re not allowed to leave.'
              Pickup: '<cooldown=2000> &4You''re not allowed to pick up items here.'
              Pressure Plates: '<cooldown=2000> &4You''re not allowed to use pressure plates here.'
              Projectiles: '<cooldown=2000> &4You''re not allowed to shoot projectiles here.'
              PvP: '<cooldown=2000> &4You''re not allowed to engage in combat here.'
              Signs: '<cooldown=2000> &4You''re not allowed to interact with signs here.'
                        
            Reload:
              Error: '&cSomething went wrong while reloading Terrainer, check console to see more info.'
              Success: '&aConfigurations and listeners reloaded successfully.'
                        
            Rename:
              Error:
                Name Length: '&cTerrain names must have at least 1 character and &7<max>&c characters max!'
                        
            Select:
              Error:
                Coordinates: '&4You don''t have permission to use coordinates in commands.'
                World: '&4You don''t have permission to make selections in this world.'
              Success:
                First: '&6First position selected in world &7<world>&6 at &7<coord>&6.'
                Second: '&6Second position selected in world &7<world>&6 at &7<coord>&6.'
                Suggest: '&ePositions were selected successfully. Now, to create a terrain, use the command &7&n/<label> claim [name]&e.'
                        
            Info:
              Error:
                No Terrains: '&7No terrains could be found.'
                No Relating Terrains: '&7No terrains that you have relations could be found.'
              Text: |-
                &8Information of &f<name>&8:
                &7ID: &f<id>
                &7Owner: &f<owner>
                &7Creation Date: &f<date>
                &7Area: &f<area> blocks
                &7World: &f<world>
                &7First Diagonal: &fX: <x1>, Z: <z1>
                &7Second Diagonal: &fX: <x2>, Z: <z2>
                &7Moderators: &f<mods>
                &7Members: &f<members>
                &7Flags: &f<flags>
                &7Description: &f<desc>
                &8----------------------------------------
                        
            Input:
              Anvil GUI:
                Display Name: 'Input'
                Lore: >-
                  Please type in what do you want to
                  <line>input.
              Ask: '&ePlease type the input in chat, you have <time> seconds.'
              Submitted: '&aInput submitted: &7<input>'
              Error: '&4Something went wrong while processing the input, please contact an administrator.'
                        
            Limits:
              No Others: '&4You don''t have permission to see/edit limits of other players!'
              Info:
                No Limits:
                  You: '&7You have no limits!'
                  Other: '&7<other> has no limits!'
                Header:
                  You: '&8Your limits:'
                  Other: '&8<other>''s limits:'
                Blocks: '<noprefix> &7Blocks: &f<used>/<max>'
                Claims: '<noprefix> &7Terrains: &f<used>/<max>'
                Footer: '<noprefix> &7Obtain more blocks and claims with &n/<label> shop&7!'
              Edit:
                Blocks: '&f<player>&7 can claim &f<value>&7 blocks now.'
                Claims: '&f<player>&7 can claim &f<value>&7 terrains now.'
                Can Not Give: '&cCan''t give more limit because limit is already maxed out!'
                Can Not Take: '&cCan''t take more limit because limit is already 0!'
                        
            Description:
              Default: '&fA protected area.'
              Max: '&cThe value must be <max> characters long max!'
                        
            Invalid Arguments:
              Error: '&4Invalid syntax! Use: &7/<label> <label2> <args>&4.'
              Flag: '<flag>'
              Flag Values: '[values]'
              Player: '<player>'
              Terrain: '<terrain>'
              World: 'world'
                        
            Target:
              And: 'and'
              Console: 'Console'
              Everyone: 'Everyone'
              None: 'None'
              You: 'You'
                        
            Permission:
              Error:
                Console: '&4You can not manage permissions of console.'
                Mods Can Manage Mods Denied: '&4Moderators are not allowed to manage moderation roles in this terrain.'
                Multiple: '&4You can only manage permission of one player at a time.'
                Owner: '&4This player owns the terrain.'
              Moderator:
                Error:
                  Contains: '&4<who> already is a moderator of <terrain>&4!'
                  Does Not Contain: '&4<who> is not a moderator of <terrain>&4!'
                Granted: '&aGranted moderator role for &f<who>&a in terrain &f<terrain>&a.'
                Revoked: '&7Revoked moderator role of &f<who>&7 in terrain &f<terrain>&7.'
              Member:
                Error:
                  Contains: '&4<who> already is a member of <terrain>&4!'
                  Does Not Contain: '&4<who> is not a member of <terrain>&4!'
                Granted: '&aGranted member role for &f<who>&a in terrain &f<terrain>&a.'
                Revoked: '&7Revoked member role of &f<who>&7 in terrain &f<terrain>&7.'
                        
            List:
              GUI Items:
                Next Page:
                  Display Name: '&6Next Page'
                  Lore: '&7Click to go to page <var0>.'
                Previous Page:
                  Display Name: '&6Previous Page'
                  Lore: '&7Click to go to page <var0>.'
                        
            Terrain List:
              No Terrains:
                Default: '&4You have no terrains!'
                Everyone: '&4No one has claimed a terrain yet.'
                Other: '&4<other> has not claimed terrains yet.'
              Chat:
                Header:
                  Default: '&7Your terrains (Page &f<page>&7/&f<total>&7):'
                  Other: '&7<other>''s terrains (Page &f<page>&7/&f<total>&7):'
                Entry: '&f<name>'
                Alternate Entry: '&9<name>'
                Separator: '&7, '
                Entry Hover: |-
                  &7ID: &f<id>
                  &7Description: &f<desc>
                  &7Area: &f<area> blocks
                  &7Owner: &f<owner>
                  &7Click to see more info.
                Footer: '&7Use &f&n/<label> list <arg> --chat <next>&7 to see more terrains.'
              GUI:
                Title:
                  Default: '&2Your terrains:'
                  Other: '&2<other>''s terrains:'
                Terrain Item:
                  Display Name: '&2<var0>'
                  Lore: >-
                    &8ID: &7<var1>
                    <line>&8Description: &7<var2>
                    <line>&8Area: &7<var3> blocks
                    <line>&8Owner: &7<var4>
                        
            Matcher:
              No Permission: '&4You don''t have permission to do that in this terrain.'
              Name:
                Multiple: '&4More than one terrain was found with that name. Please specify the terrain''s ID instead of name.'
                Not Found: '&4No terrain matching the specified name or ID was found.'
              Location:
                Multiple: '&4More than one terrain was found in the location, please specify the terrain''s name or ID. Ex: &7&n/<label> <args>'
                Not Found: '&4No terrain was found in this location, please specify the terrain''s name or ID. Ex: &7&n/<label> <args>'
                        
            Flags:
              Allow: '&a&lALLOW'
              Deny: '&c&lDENY'
              Undefined: '&7&lundefined'
              Set: '&7Flag <flag>&7 set in the terrain &f<name>&7 with value ''&f<state>&7''.'
              Unset: '&7Flag <flag>&7 removed from terrain &f<name>&7.'
              Error:
                Default: '&4Unable to set flag <flag>&4: &f<message>'
                Message Location: '&4The only accepted values for message location are: &7ActionBar&4, &7BossBar&4, &7Chat&4, &7Title&4 or &7NONE&4.'
                Not Owner: '&4Only the owner is allowed to edit this flag.'
                Unknown: '&4Something went wrong while setting this flag. Please contact an administrator.'
              Management GUI Title: '&2Flags of <terrain>&2:'
              Values:
                Armor Stands:
                  Display Name: '&x&B&3&A&2&9&3&lArmor Stands'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to use armor stands.
                Build:
                  Display Name: '&c&lBuild'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to build.
                Build Vehicles:
                  Display Name: '&8&lBuild Vehicles'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to place or break vehicles,
                    <line>such as default minecarts and boats.
                Buttons:
                  Display Name: '&2&lButtons'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to use buttons and levers.
                Containers:
                  Display Name: '&6&lContainers'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to use containers.
                Dispensers:
                  Display Name: '&7Dispensers'
                  Lore: >-
                    State: <var0>
                    <line>Allows dispensers to fire.
                Doors:
                  Display Name: '&x&8&B&5&F&2&B&lDoors'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to use doors and gates.
                Effects:
                  Display Name: '&b&lEffects'
                  Lore: >-
                    State: <var0>
                    <line>A list of effects to set to the players.
                Enemy Harm:
                  Display Name: '&8&lEnemy Harm'
                  Lore: >-
                    State: <var0>
                    <line>Prevents non-members from harming enemy
                    <line>entities.
                Enter:
                  Display Name: '&2&lEnter'
                  Lore: >-
                    State: <var0>
                    <line>Prevents players from entering.
                Enter Vehicles:
                  Display Name: '&8&lEnter Vehicles'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to enter vehicles, such as
                    <line>horses, minecarts, boats, llamas, etc.
                Entity Harm:
                  Display Name: '&d&lEntity Harm'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to harm entities inside.
                    <line>Enemy entities are controlled by
                    <line>'Prevent Enemy Harm' flag.
                Entity Interactions:
                  Display Name: 'Entity Interactions'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to interact with entities.
                    <line>Examples of entity interactions: talking
                    <line>with villagers, curing zombies, breeding
                    <line>animals, etc.
                Explosion Damage:
                  Display Name: '&4&lExplosion Damage'
                  Lore: >-
                    State: <var0>
                    <line>Allows blocks being damaged from
                    <line>explosives.
                Farmland Trample:
                  Display Name: '&x&2&E&1&5&0&3&lFarmland Trample'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to trample on farmland.
                Fire Damage:
                  Display Name: '&6&lFire Damage'
                  Lore: >-
                    State: <var0>
                    <line>Allows blocks being damaged by fire.
                Interactions:
                  Display Name: '&a&lInteractions'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to interact with blocks.
                    <line>Examples of interactions: using flint and
                    <line>steel, eating cake, using flower pots,
                    <line>changing repeater levels, etc.
                Item Drop:
                  Display Name: '&8&lItem Drop'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to drop items.
                Item Frames:
                  Display Name: '&e&lItem Frames'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to use item frames.
                Item Pickup:
                  Display Name: '&8&lItem Pickup'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to pick up items.
                Leaf Decay:
                  Display Name: '&x&1&F&5&C&1&4&lLeaf Decay'
                  Lore: >-
                    State: <var0>
                    <line>Prevents leaves from decaying naturally.
                Leave:
                  Display Name: '&4&lLeave'
                  Lore: >-
                    State: <var0>
                    <line>Prevents players from leaving.
                Leave Message:
                  Display Name: '&7&lLeave Message'
                  Lore: >-
                    State: <var0>
                    <line>The message shown when someone leaves the
                    <line>terrain.
                Liquid Flow:
                  Display Name: '&1&lLiquid Flow'
                  Lore: >-
                    State: <var0>
                    <line>Prevents all liquid from flowing within the
                    <line>terrain. Liquids from outside are prevented
                    <line>from going in depending on the state of the
                    <line>'Build' flag.
                Message Location:
                  Display Name: '&7&lMessage Location'
                  Lore: >-
                    State: <var0>
                    <line>The location where the enter and leave
                    <line>messages will be. It can be: NONE
                    <line>ACTIONBAR, BOSSBAR, and TITLE. By default,
                    <line>it is displayed in chat.
                    <line>The description of the terrain is used
                    <line>as enter message, and you can set a leave
                    <line>message using the 'Leave Message' flag.
                Mob Spawn:
                  Display Name: '&x&4&D&7&E&3&A&lMob Spawn'
                  Lore: >-
                    State: <var0>
                    <line>Prevents mobs from spawning within the
                    <line>terrain. Spawner mobs are controlled by
                    <line>'Prevent Spawners' flag.
                Mods Can Edit Flags:
                  Display Name: 'Mods Can Edit Flags'
                  Lore: >-
                    State: <var0>
                    <line>Allows moderators to edit terrain flags,
                    <line>except those who control moderator
                    <line>permissions.
                Mods Can Manage Mods:
                  Display Name: 'Mods Can Manage Mods'
                  Lore: >-
                    State: <var0>
                    <line>Allows moderators to grant or revoke the
                    <line>moderation role to other players.
                Outside Dispensers:
                  Display Name: '&7Outside Dispensers'
                  Lore: >-
                    State: <var0>
                    <line>Allows dispensers from outside the terrain
                    <line>to fire into the terrain.
                Outside Pistons:
                  Display Name: '&x&8&F&B&F&4&5&lOutside Piston Pull'
                  Lore: >-
                    State: <var0>
                    <line>Allows pistons from outside the terrain
                    <line>pushing or pulling blocks.
                Outside Projectiles:
                  Display Name: '&x&A&0&6&A&4&2&lOutside Projectiles'
                  Lore: >-
                    State: <var0>
                    <line>Allows projectiles from outside to land
                    <line>within.
                Pistons:
                  Display Name: '&e&lPistons'
                  Lore: >-
                    State: <var0>
                    <line>Prevents pistons within from moving.
                Pressure Plates:
                  Display Name: '&e&lPressure Plates'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to trigger pressure plates.
                Projectiles:
                  Display Name: '&x&A&0&6&A&4&2&lProjectiles'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to shoot projectiles.
                PvP:
                  Display Name: '&x&1&D&A&A&E&3&lPvP'
                  Lore: >-
                    State: <var0>
                    <line>Enables player versus player combat.
                Sign Click:
                  Display Name: '&x&8&E&6&6&1&B&lSign Click'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to click on signs.
                Sign Edit:
                  Display Name: '&x&3&E&2&F&2&3&lSign Edit'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to edit signs.
                Spawners:
                  Display Name: '&0&lSpawners'
                  Lore: >-
                    State: <var0>
                    <line>Prevents mobs from mob spawners within the
                    <line>terrain from spawning.
                Vulnerability:
                  Display Name: '&x&F&F&C&C&0&0&lVulnerability'
                  Lore: >-
                    State: <var0>
                    <line>Prevents everyone from losing any health
                    <line>saturation.
            """.replace("#VERSION#", TerrainerVersion.VERSION_STRING));
    private static final @NotNull ConfigurationLoader loader = new ConfigurationLoader();

    static {
        loader.registerConfiguration(CONFIG, TerrainerVersion.VERSION, TerrainerVersion.VERSION);
        loader.registerConfiguration(LANG_EN_US, TerrainerVersion.VERSION, TerrainerVersion.VERSION);
    }

    public static @NotNull ConfigurationLoader loader() {
        return loader;
    }
}
