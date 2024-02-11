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
            Min Area: 25.0
                        
            # The minimum dimensions an area needs to be allowed to be claimed.
            Min Dimensions: 5.0 # Areas smaller than 5x5 will not be allowed.
                        
            # The maximum length a description or leave message of a terrain can have.
            Max Description Length: 100
                        
            # The maximum length a terrain's name can be.
            Max Name Length: 26
                        
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
                        
            # Players can buy additional limits using '/tr shop'. You can edit the prices here.
            Shop:
              Blocks:
                Enabled: true
                # The prices will increase according to how many blocks the player already has. The prices set below are
                #the default ones, and the inflation will be added to them.
                Inflation:
                  Enabled: true
                  Divide: 1000 # Divide the amount of blocks a player has by this value.
                  Multiplier: 200.0 # Multiply the divided amount of blocks by this value, this will be the inflation.
                  # If a player has 10,000 blocks, the blocks will be divided by 1,000, and the result (10) will be multiplied
                  #by 200, so if they want to buy 1,000 more blocks, the price will be 10 x 200 (or 2,000$).
                Option 1:
                  Amount: 1000
                  Price: 2000
                  Material: 'IRON_BLOCK'
                Option 2:
                  Amount: 5000
                  Price: 9800 # 2% off
                  Material: 'GOLD_BLOCK'
                Option 3:
                  Amount: 12100
                  Price: 22990 # 5% off
                  Material: 'DIAMOND_BLOCK'
              Claims:
                Enabled: true
                Inflation:
                  Enabled: true
                  Divide: 1
                  Multiplier: 100.0 # For every claim limit the player already has, the price will increase by 100.
                Option 1:
                  Amount: 1
                  Price: 2000
                  Material: 'SMALL_AMETHYST_BUD'
                Option 2:
                  Amount: 5
                  Price: 9800 # 2% off
                  Material: 'LARGE_AMETHYST_BUD'
                Option 3:
                  Amount: 15
                  Price: 28500 # 5% off
                  Material: 'AMETHYST_CLUSTER'
                        
            # Players can see borders of terrains by using '/tr info' or walking in to them, as long as they have the
            #permission 'terrainer.borders.show'.
            # They can toggle borders on and off using '/tr borders'.
            Borders:
              # Whether to show borders of terrains to players.
              Enabled: true
              # Whether to show the border every time a player enters a terrain.
              On Enter: true
              # Whether to spawn the particles asynchronously.
              Async: true
              # Terrains with area larger than this will not show borders.
              Max Area: 2500
              # The maximum amount of players viewing borders at the same time.
              Max Viewing: 20
              # The particle type, find more here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Particle.html
              Particle: CLOUD
              # Add this to the Y coordinate where the particle will spawn.
              Y OffSet: 0.5
              # Time in ticks to repeat the particles while showing the border.
              Frequency: 5
              # The total amount of time in ticks to show the border.
              Time: 200
                        
            Input:
              # Whether an anvil should be used to get inputs from the player. If disabled, the input will be get from chat.
              Anvil GUI:
                Enabled: true
                Material: FEATHER
                Glowing: true
              # The max time in ticks to get inputs from chat, if the player can't answer the input within this time, it will be cancelled.
              Chat Interval: 400
                        
            # The commands to execute when a player joins the server in a terrain, and the TerrainEnterEvent is cancelled.
            # An example of this happening is if the player had permission to enter a terrain which ENTRY flag is denied,
            #and when they left the server their permission to the terrain was revoked. So when they join later,
            #TerrainEnterEvent will be cancelled because the player is not allowed, then these commands will execute.
            # This is not exclusive to ENTRY flag denied, any hooking plugin might cancel TerrainEnterEvent in any occasion.
            # It is suggested to teleport the player to a place where entry is always allowed, either that or kick the
            #player from the server.
            # Use %p for the player's name, and %t for the terrain's ID.
            Commands When TerrainEnterEvent Cancelled on Join or Create:
            - 'tp %p 0 64 0'
            #- 'spawn %p'
            #- 'kick %p You tried to join in a terrain where ENTRY is denied.'
                        
            # The item for selecting positions to claim terrains.
            # Can be obtained/bought with '/tr wand'
            Selector Wand:
              Material: GOLDEN_SHOVEL
              Glowing: false
              # If false, all golden shovels (or the set material) will pass as a selector wand.
              # If true, the item can only be obtained by '/tr wand' command.
              Unique: true
              # Makes so the action of right clicking using this item (golden shovel by default) is cancelled, and the
              #item is not used.
              Cancel Interaction: true
              # If false, position #1 and #2 alternates by using just right-click.
              # If true, use left-click for position #1 and right-click for position #2.
              Left And Right Click: false
              # Price for buying the item with '/tr wand'. Recommended only if 'Unique' is true.
              # Buy permission: terrainer.wand.selector
              # Free Bypass: terrainer.wand.selector.free
              Price: 100
              # A cooldown to prevent players from getting a lot of wands in a short amount of time.
              # Bypass: terrainer.wand.selector.nocooldown
              Cooldown: 7200 #seconds
              # Allows selecting block by far away, without clicking at it.
              Far Selection:
                Enabled: true
                # The max distance from the player to allow the far selection.
                Max Distance: 20
                        
            # Markers that show up when a player selects a position.
            Markers:
              Enabled: true
              # How long in ticks the marker should be shown.
              Show Time: 1200
                        
            # The item for seeing information about terrains in the location.
            # Can be obtained/bought with '/tr wand info'
            Info Wand:
              Material: PAPER
              Glowing: false
              Unique: false
              Cancel Interaction: false
              # Since 'Unique' is false, the command is just going to sell a piece of paper (with custom name).
              # It's recommended to only give permission to buy if the item is unique.
              # Buy permission: terrainer.wand.info
              # Free Bypass: terrainer.wand.info.free
              Price: 50
              # Bypass: terrainer.wand.info.nocooldown
              Cooldown: 3600 #seconds
                        
            # The permission for flying.
            # When flying players enter terrains that have the flag FLY denied, they will have their ability to fly removed.
            # Players who have this permission will have their ability to fly granted back when they leave the terrain.
            Fly Permission: 'essentials.fly'
                        
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
                World Terrain Item:
                  Material: BEDROCK
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
                  Material: BONE
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
                Fire Damage:
                  Material: FIRE_CHARGE
                Fly:
                  Material: FEATHER
                Frost Walk:
                  Material: PACKED_ICE
                Glide:
                  Material: ELYTRA
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
                Trample:
                  Material: FARMLAND
                Vulnerability:
                  Material: TOTEM_OF_UNDYING""".replace("#VERSION#", TerrainerVersion.VERSION_STRING));
    public static final ConfigurationHolder LANG_EN_US = new ConfigurationHolder(DATA_FOLDER.resolve("Language").resolve("Language EN-US.yml"), """
            Version: '#VERSION#'
                        
            General:
              Player Not Found: '&cA player with name "&7<value>&c" could not be found.'
              Prefix: '&8[&cTerrains&8] '
              No Economy: '&4An economy plugin was not found.'
              No Permission: '&4You don''t have permission to do this.'
              No Permission Others: '&4You can not do this with other players.'
              Not A Number: '&cThe value "&7<value>&c" is not a number.'
              Not Enough Money: '&4This costs &6<value>$&4 and you don''t have enough money to buy it.'
              World Not Found: '&cA world with name "&7<value>&c" was not found.'
                        
            # Translations of command arguments.
            Commands:
              Confirm:
                Confirm: 'confirm'
                List: 'list'
              Priority:
                Here: '-here'
              Transfer:
                Force: '-force'
              Wand:
                Info: 'info'
                Selector: 'selector'
                        
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
                Different Worlds: '&4Terrain could not be created because the selections are in different worlds!'
                Dimensions: '&4Terrains must have at least &6<min>&4 blocks in &7width&4 and &7length&4!'
                No Block Limit: '&4This terrain has an area of &6<area>&4 blocks and you have only &6<used>&c blocks left! To increase your block limit, buy in the &7&n/tr shop&4.'
                No Claim Limit: '&4You can''t create more than &6<max>&4 terrains! To increase your claim limit, buy in the &7&n/tr shop&4.'
                Not Selected: '&eYou need to make a selection before creating a terrain. You can select with the command &f&n/<label> pos1&e and &f&n/<label> pos2&e or with the selection wand: &f&n/<label> wand&e.'
                Overlap: '&4This terrain would overlap &7<other>&4 terrain! You can only overlap your own terrains.'
                Too Small: '&4Area too small! Terrains must have at least &6<min>&4 blocks.'
                Unknown: '&4An unknown error occurred while creating this terrain.'
                World No Longer Exists: '&4The world in your selections no longer exists.'
              Success: '&2Terrain ''&a<name>&2'' claimed successfully! Used block limit: &7<used>&f/&7<max>'
              Define: '&2Terrain ''&a<name>&2'' defined successfully with all protection flags!'
                        
            Delete:
              Confirmation: '&7Are you sure you want to delete &e<name>&7? Please confirm the deletion with &f&n/<label> <label2>&7.'
              World Confirmation: '&7Are you sure you want to reset all data for the global terrain of &e<name>&7? Please confirm the deletion with &f&n/<label> <label2>&7.'
              Confirmation Description: 'Delete <name>'
              Error: '&cTerrain could not be deleted.'
              Success: '&e<name>&a was deleted successfully!'
              Select: '&cSelect the terrain to delete:'
              World Success: '&aAll data of the global terrain of &e<name>&a was deleted and the terrain was restored!'
                        
            Enter Leave Messages Format: '&6<name>: &7<message>'
                        
            Protections:
              Anvils: '<cooldown=2000> &4You''re not allowed to use anvils here.'
              Armor Stands: '<cooldown=2000> &4You''re not allowed to use armor stands here.'
              Build: '<cooldown=2000> &4You''re not allowed to build here.'
              Build Boats: '<cooldown=2000> &4You''re not allowed to break or place boats here.'
              Build Minecarts: '<cooldown=2000> &4You''re not allowed to break or place minecarts here.'
              Buttons: '<cooldown=2000> &4You''re not allowed to use buttons and levers here.'
              Command Blacklist: '<cooldown=2000> &4You''re not allowed to use this command here.'
              Containers: '<cooldown=2000> &4You''re not allowed to open containers here.'
              Doors: '<cooldown=2000> &4You''re not allowed to open doors or gates here.'
              Eat: '<cooldown=2000> &4You''re not allowed to consume items here.'
              Enemy Harm: '<cooldown=2000> &4You''re not allowed to harm enemy entities here.'
              Enter: '<cooldown=2000> &4You''re not allowed to enter.'
              Enter Vehicles: '<cooldown=2000> &4You''re not allowed to enter vehicles here.'
              Entity Harm: '<cooldown=2000> &4You''re not allowed to harm entities here.'
              Entity Interactions: '<cooldown=2000> &4You''re not allowed to interact with entities here.'
              Fly: '<cooldown=2000> &4You''re not allowed to fly here.'
              Glide: '<cooldown=2000> &4You''re not allowed to glide here.'
              Interactions: '<cooldown=2000> &4You''re not allowed to use this here.'
              Item Drop: '<cooldown=2000> &4You''re not allowed to drop items here.'
              Item Frames: '<cooldown=2000> &4You''re not allowed to use item frames here.'
              Item Pickup: '<cooldown=4000> &4You''re not allowed to pick up items here.'
              Join Loading Server Message: |-
                &cServer is loading, please try joining in a few seconds.
              # Use <default> for the default spigot shutdown message.
              Kick Message: |-
                &4<default>
              Leave: '<cooldown=2000> &4You''re not allowed to leave.'
              Lighters: '<cooldown=2000> &4You''re not allowed to use lighters here.'
              Outside Projectiles: '<cooldown=2000> &4You''re not allowed to shoot projectiles here.'
              Potions: '<cooldown=2000> &4You''re not allowed to drink potions here.'
              Prepare: '<cooldown=2000> &4You''re not allowed to use crafting blocks here.'
              Pressure Plates: '<cooldown=4000> &4You''re not allowed to use pressure plates here.'
              Projectiles: '<cooldown=2000> &4You''re not allowed to shoot projectiles here.'
              PvP: '<cooldown=2000> &4You''re not allowed to engage in combat here.'
              Sign Click: '<cooldown=2000> &4You''re not allowed to interact with signs here.'
              Sign Edit: '<cooldown=2000> &4You''re not allowed to edit signs here.'
              Trample: '<cooldown=2000> &4You''re not allowed to trample farmland here.'
                        
            Reload:
              Error: '&cSomething went wrong while reloading Terrainer, check console to see more info.'
              Success: '&aConfigurations and listeners reloaded successfully.'
                        
            Rename:
              Error:
                Name Length: '&4Terrain names must have at least 1 character and &7<max>&4 characters max!'
                Same: '&7Nothing changed. Terrain was already named &f<name>&7.'
                World Terrain: '&4Global world terrains can not be renamed!'
              Reset: '&aTerrain &7<old>&a had its name reset to default: &7<new>'
              Renamed: '&aTerrain &7<old>&a was successfully renamed to &7<new>&a.'
              Select: '&cSelect the terrain to rename:'
                        
            Select:
              Error:
                Coordinates: '&4You don''t have permission to use coordinates in commands.'
                World: '&4You don''t have permission to make selections in this world.'
              Success:
                First: '&6First position selected in world &7<world>&6 at &7<coord>&6.'
                Second: '&6Second position selected in world &7<world>&6 at &7<coord>&6.'
                Suggest: '<cooldown=30000> &ePositions were selected successfully. Now, to create a terrain, use the command &7&n/<label> claim [name]&e.'
                        
            Selector Wand:
              Display Name: '&6&l&nClaim Selection Wand'
              Lore: >-
                &7Click with the selection wand on the
                <line>&7ground to mark the two diagonals of
                <line>&7your terrain. Once marked, use &f&n/tr claim
                <line>&7to claim and protect your terrain!
                        
            Info Wand:
              Display Name: '&7&l&nTerrain Info Wand'
              Lore: >-
                &7Click with the info wand on the ground
                <line>&7to check information about any existing
                <line>&7terrains in the location.
                        
            Info:
              Error:
                No Terrains: '<cooldown=1000> &7No terrains could be found.'
                No Relating Terrains: '<cooldown=1000> &7No terrains that you have relations could be found.'
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
                &7Priority: &f<priority>
                &8----------------------------------------
                        
            Input:
              Anvil GUI:
                Display Name: 'Input'
                Lore: >-
                  Please type in what do you want to
                  <line>input.
              Ask: '&ePlease type the input in chat, you have <time> seconds.'
                        
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
              Default: 'A protected area'
              World Terrain: 'Global terrain for <world>'
              Error:
                Length: '&cThe value must be <max> characters long max!'
              Reset: '&aDescription of terrain &7<terrain>&a was reset to default.'
              Select: '&cSelect the terrain to edit description:'
              Set: '&aDescription of terrain &7<terrain>&a set to: &e<description>&a.'
                        
            Invalid Arguments:
              Error: '&4Invalid syntax! Use: &7/<label> <label2> <args>&4.'
              Flag: '<flag> <values>'
              Flag Optional: '<flag> [values]'
              Player: '<player>'
              Player Optional: '[player]'
              Priority: '[priority|-here]'
              Terrain: '<terrain>'
              Terrain Optional: '[--t <terrain>]'
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
                Notify: '&aYou were granted a moderator role in the terrain &f<terrain>&a!'
              Member:
                Error:
                  Contains: '&4<who> already is a member of <terrain>&4!'
                  Does Not Contain: '&4<who> is not a member of <terrain>&4!'
                Granted: '&aGranted member role for &f<who>&a in terrain &f<terrain>&a.'
                Revoked: '&7Revoked member role of &f<who>&7 in terrain &f<terrain>&7.'
                Notify: '&aYou were granted a member role in the terrain &f<terrain>&a!'
              Select: '&cSelect the terrain to edit roles:'
                        
            Transfer:
              Confirmation Description: 'Accept ownership of <terrain>'
              Error:
                Low Block Limit: '&7<player>&7 does not have enough block limit to accept this terrain.'
                Low Claim Limit: '&7<player>&7 does not have enough claim limit to accept this terrain.'
                Not Allowed: '&4You''re not allowed to transfer this terrain.'
                Not Online: '&4You can''t transfer the terrain to &7<player>&4 because they are not online!'
                Nothing Changed: '&7Nothing changed. <player>&7 already owns this terrain.'
                World Terrain: '&4Global world terrains can not be owned!'
              Request: '&f<player>&7 wants to transfer the terrain &f<terrain>&7 to you. To accept, use &f&n/tr confirm&7.'
              Requested: '&7A request was sent to &f<who>&7 to accept the terrain.'
              Select: '&cSelect the terrain to transfer:'
              Success: '&aTerrain &7<terrain>&a was transferred successfully to &7<who>&a.'
                        
            Priority:
              Error:
                No Terrains: '<cooldown=1000> &7No terrains could be found.'
              Same:
                Here: '&7All terrains in this location have priority &f<priority>&7: &f<terrains>'
              Removed: '&7Some terrains are not in the list because you don''t own them.'
              Select: '&cSelect the terrain to edit priority:'
              Single: '&7Terrain &f<terrain>&7 has &f<priority>&7 priority.'
                        
            Wand:
              Bought: '&aYou''ve bought a &7<type>&a for &6<price>$&a.'
              Cooldown: '&4You must wait <remaining> seconds to buy this again.'
              Given: '&aGave &7<player>&a a &7<type>&a for &6<price>$&a.'
              Received: '&aYou''ve received a &7<type>&a from &7<player>&a.'
                        
            Shop:
              Blocks:
                Option 1:
                  Display Name: '&7&l<var0> BLOCKS'
                  Lore: >-
                    &8Buy &7<var0>&8 blocks for
                    <line>&8only &6<var1>$
                Option 2:
                  Display Name: '&6&l<var0> BLOCKS'
                  Lore: >-
                    &8Buy &7<var0>&8 blocks for
                    <line>&8only &6<var1>$
                    <line>&82% off
                Option 3:
                  Display Name: '&b&l<var0> BLOCKS'
                  Lore: >-
                    &8Buy &7<var0>&8 blocks for
                    <line>&8only &6<var1>$
                    <line>&85% off
              Claims:
                Option 1:
                  Display Name: '&4&l<var0> CLAIMS'
                  Lore: >-
                    &8Buy &7<var0>&8 claim for
                    <line>&8only &6<var1>$
                Option 2:
                  Display Name: '&4&l<var0> CLAIMS'
                  Lore: >-
                    &8Buy &7<var0>&8 claims for
                    <line>&8only &6<var1>$
                    <line>&82% off
                Option 3:
                  Display Name: '&4&l<var0> CLAIMS'
                  Lore: >-
                    &8Buy &7<var0>&8 claims for
                    <line>&8only &6<var1>$
                    <line>&85% off
              Error:
                Disabled: '&4Shop is disabled.'
              Success:
                Blocks: '&aYou''ve successfully bought an additional limit of &7<amount>&a blocks for &6<price>$!'
                Claims: '&aYou''ve successfully bought an additional limit of &7<amount>&a claims for &6<price>$!'
              Title: '&6&nLimits Shop'
                        
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
                  Lore: |-
                    &8ID: &7<var1>
                    &8Description: &7<var2>
                    &8Area: &7<var3> blocks
                    &8Owner: &7<var4>
                World Terrain Item:
                  Display Name: '&6<var0>'
                  Lore: |-
                    &8World ID: &7<var1>
                    &8Description: &7<var2>
                    
                    &e&lA global terrain of a world
                        
            Matcher:
              Changed: '&4The terrain you selected could not be found because it was changed while you were selecting it.'
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
              Set: '&7Flag <flag>&7 set in the terrain &f<name>&7 with value: &f<state>&7.'
              Unset: '&7Flag <flag>&7 removed from terrain &f<name>&7.'
              Default Alert: '&7Terrain &f<name>&7 will now use the default value of <flag>&7: &f<state>&7.'
              Error:
                Default: '&4Unable to set flag <flag>&4: &f<message>'
                Message Location: '&4The only accepted values for message location are: &7ActionBar&4, &7BossBar&4, &7Chat&4, &7Title&4 or &7NONE&4.'
                Not Owner: '&4Only the owner is allowed to edit this flag.'
                Unknown: '&4Something went wrong while setting this flag. Please contact an administrator.'
              Management GUI Title: '&2Flags of <terrain>&2:'
              Select: '&cSelect the terrain to edit flags:'
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
                Fire Damage:
                  Display Name: '&6&lFire Damage'
                  Lore: >-
                    State: <var0>
                    <line>Allows blocks being damaged by fire.
                Fly:
                  Display Name: 'Fly'
                  Lore: >-
                    State: <var0>
                    <line>Allows players with fly permission to fly.
                Frost Walk:
                  Display Name: 'Frost Walk'
                  Lore: >-
                    State: <var0>
                    <line>Allows players to form ice using frost
                    <line>walk enchanted boots.
                Glide:
                  Display Name: 'Glide'
                  Lore: >-
                    State: <var0>
                    <line>Allows players to glide using elytras.
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
                    <line>messages will be. It can be: &7NONE&5&0,
                    <line>&7ACTIONBAR&5&0, &7BOSSBAR&5&0, and &7TITLE&5&0.
                    <line>The description of the terrain is used
                    <line>as enter message, to set a leave message,
                    <line>use the 'Leave Message' flag.
                Mob Spawn:
                  Display Name: '&x&4&D&7&E&3&A&lMob Spawn'
                  Lore: >-
                    State: <var0>
                    <line>Prevents mobs from spawning within the
                    <line>terrain. Spawner mobs are controlled by
                    <line>'Spawners' flag.
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
                Trample:
                  Display Name: '&x&2&E&1&5&0&3&lTrample'
                  Lore: >-
                    State: <var0>
                    <line>Allows everyone to trample on farmland.
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

    private Configurations() {
    }

    public static @NotNull ConfigurationLoader loader() {
        return loader;
    }
}
