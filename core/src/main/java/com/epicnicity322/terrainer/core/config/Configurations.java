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
            # Bypass permission: 'terrainer.bypass.name-length'
            Max Name Length: 26
                        
            # Protect against potential actions when the server reloads.
            Kick Players On Disable: true
                        
            # The permission for flying.
            # When flying players enter terrains that have the flag FLY denied, they will have their ability to fly removed.
            # Players who have this permission will have their ability to fly granted back when they leave the terrain.
            Fly Permission: 'essentials.fly'
                        
            # If TRUE:
            #   The ability to fly will ONLY be granted back if the player has the 'Fly Permission'.
            # If FALSE:
            #   Flying players that entered the terrain WITHOUT the 'Fly Permission', will always have their flight returned when they leave.
            #   Flying players that entered the terrain WITH the 'Fly Permission', will ONLY have their flight returned if they still have the 'Fly Permission'.
            # By leaving it false, you can control players that have the fly permission, while also not interfering with admins who can fly using alternate ways.
            Strict Fly Return: false
                        
            # List of prohibited terrain names.
            # Bypass permission: 'terrainer.bypass.name-blacklist'
            Blacklisted Names:
            - ''
                        
            # Whether to alert if dangerous flags are allowed/unset for global world-terrains.
            Alert Dangerous Flags: true
                        
            # Settings for how limits are calculated.
            Limits:
              # Set to false to make so when there's two or more terrains claiming the same block, the block will only
              #count as one towards the used block limit.
              # Set to true to make so the used block limit is simply the sum of the areas of all terrains the player owns.
              Nested Terrains Count Towards Block Limit: false
              
              # Set to false to use the sum of terrains from all worlds as used limit.
              # Set to true to make so the limit doesn't count terrains from different worlds.
              Per World Block Limit: false
              # Check above.
              Per World Claim Limit: false
              
              # If the player has more than one limit permission, add the value of each permission to the player's total limit.
              # Set to false to only use the greatest value as limit.
              Sum If Theres Multiple Block Limit Permissions: true
              # Check above.
              Sum If Theres Multiple Claim Limit Permissions: true
                        
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
                        
            # Markers that show up when a player selects a position.
            Markers:
              Enabled: true
              # How long in ticks the marker should be shown.
              Show Time: 1200
              Created Color: 55FF55 # Hex
              Selection Color: FFFF55
              Terrain Color: FFFFFF
              # Air blocks can only be colored by regular color codes
              Selection Block: GLOWSTONE
              Selection Edge Block: GOLD_BLOCK
              Terrain Block: DIAMOND_BLOCK
              Terrain Edge Block: GLASS
              
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
              
            # The commands to execute when a player joins the server in a terrain, and the TerrainEnterEvent is cancelled.
            # An example of this happening is if the player had permission to enter a terrain which ENTRY flag is denied,
            #and when they left the server their permission to the terrain was revoked. So when they join later,
            #TerrainEnterEvent will be cancelled because the player is not allowed, then these commands will execute.
            # This is not exclusive to ENTRY flag denied, any hooking plugin might cancel TerrainEnterEvent in any occasion.
            # It is suggested to teleport the player to a place where entry is always allowed, either that or kick the
            #player from the server.
            # Use %p for the player's name.
            Commands When TerrainEnterEvent Cancelled on Join or Create:
            - 'tp %p 0 64 0'
            #- 'spawn %p'
            #- 'kick %p You tried to join in a terrain where ENTRY is denied.'
                        
            Protections And Performance:
              # There are multiple checks for when the players move. Disabling them can improve performance, but at a
              #cost of features related to terrain entering/leaving.
              Disable Enter Leave Events: false
              # Entity Move Event checks whether players riding entities enters terrains. Disabling it can improve
              #performance, but at a cost of the accuracy of entering/leaving terrains.
              Disable Entity Move Event: false
              # Piston events can be really expensive because every time a piston moves, Terrainer has to check whether
              #each block is within a terrain. Disabling it can improve performance, but at a cost of Pistons and
              #Outside Pistons flags.
              Disable Piston Events: false
              # Block From To Event checks whether water, lava, and dragon eggs are in terrains. Disabling it can improve
              #performance, but at a cost of Liquid Flow flag and potentially flooding terrains from outside.
              Disable Block From To Event: false
              # Creature Spawn Event is responsible for handling Mob Spawn and Spawner Spawn flags. Disabling it can
              #improve performance, but at a cost of losing those flags.
              Disable Creature Spawn Event: false
              # Players will be able to enter terrains with ENTER flag denied IF there's a higher priority terrain on the
              #same location with ENTER flag allowed. Disable it to protect the player from leaving a terrain with ENTER
              #allowed into a terrain with ENTER denied. This will also affect FLIGHT and GLIDE protections.
              Allow Higher Priority Entrances: false
                        
            Input:
              # Whether an anvil should be used to get inputs from the player. If disabled, the input will be get from chat.
              Anvil GUI:
                Enabled: true
                Material: FEATHER
                Glowing: true
              # The max time in ticks to get inputs from chat, if the player can't answer the input within this time, it will be cancelled.
              Chat Interval: 400
                        
            Teleport:
              # The delay to wait before teleporting the player.
              # If the player moves, the teleportation will be cancelled.
              # Set to 0 to disable, or grant the permission 'terrainer.teleport.nodelay'
              Movement Check Delay: 3 #seconds
                        
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
                Anvils:
                  Material: ANVIL
                Armor Stands:
                  Material: ARMOR_STAND
                Block Form:
                  Material: POWDER_SNOW_BUCKET
                Block Spread:
                  Material: SCULK_CATALYST
                Build:
                  Material: BRICKS
                Build Boats:
                  Material: OAK_BOAT
                Build Minecarts:
                  Material: MINECART
                Buttons:
                  Material: OAK_BUTTON
                Cauldrons:
                  Material: CAULDRON
                Cauldrons Change Level Naturally:
                  Material: CAULDRON
                Command Blacklist:
                  Material: BARRIER
                Containers:
                  Material: CHEST
                Dispensers:
                  Material: DISPENSER
                Doors:
                  Material: OAK_DOOR
                Eat:
                  Material: COOKED_BEEF
                Effects:
                  Material: BEACON
                Enemy Harm:
                  Material: BONE
                Enter:
                  Material: LIME_WOOL
                Enter Console Commands:
                  Material: COMMAND_BLOCK
                Enter Player Commands:
                  Material: COMMAND_BLOCK
                Enter Vehicles:
                  Material: SADDLE
                Entity Harm:
                  Material: PIG_SPAWN_EGG
                Entity Interactions:
                  Material: LEAD
                Explosion Damage:
                  Material: TNT
                Fire Damage:
                  Material: FIRE_CHARGE
                Fire Spread:
                  Material: CAMPFIRE
                Fly:
                  Material: FEATHER
                Frost Walk:
                  Material: PACKED_ICE
                Glide:
                  Material: ELYTRA
                Interactions:
                  Material: BRUSH
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
                Leave Console Commands:
                  Material: COMMAND_BLOCK
                Leave Message:
                  Material: PAPER
                Leave Player Commands:
                  Material: COMMAND_BLOCK
                Lighters:
                  Material: FLINT_AND_STEEL
                Liquid Flow:
                  Material: WATER_BUCKET
                Message Location:
                  Material: PAPER
                Mob Spawn:
                  Material: ZOMBIE_HEAD
                Mods Can Edit Flags:
                  Material: WRITABLE_BOOK
                Mods Can Manage Mods:
                  Material: WRITABLE_BOOK
                Outside Dispensers:
                  Material: DISPENSER
                Outside Pistons:
                  Material: STICKY_PISTON
                Outside Projectiles:
                  Material: BOW
                Pistons:
                  Material: PISTON
                Plant:
                  Material: WHEAT_SEEDS
                Plant Grow:
                  Material: OAK_SAPLING
                Plow:
                  Material: IRON_HOE
                Potions:
                  Material: SPLASH_POTION
                Prepare:
                  Material: CRAFTING_TABLE
                Pressure Plates:
                  Material: OAK_PRESSURE_PLATE
                Projectiles:
                  Material: ARROW
                PvP:
                  Material: DIAMOND_SWORD
                Show Borders:
                  Material: GLOWSTONE
                Sign Click:
                  Material: OAK_SIGN
                Sign Edit:
                  Material: DARK_OAK_SIGN
                Spawners:
                  Material: SPAWNER
                Sponges:
                  Material: SPONGE
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
                        
            Placeholder Values:
              Console Owner: 'Console'
              Flag Undefined: 'Undefined'
              Infinite Limit: 'Infinite'
              No One Top: 'No one'
              Roles:
                Member: 'Member'
                Moderator: 'Moderator'
                Owner: 'Owner'
                Unrelated: 'Unrelated'
              Terrain Not Found: '&aWilderness'
              Unknown Terrain: 'Unknown'
                        
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
                No Block Limit: '&4This terrain has an area of &6<area>&4 blocks and you have &6<free>&c blocks left! To increase your block limit, buy in the &7&n/tr shop&4.'
                No Claim Limit: '&4You can''t create more than &6<max>&4 terrains! To increase your claim limit, buy in the &7&n/tr shop&4.'
                Not Selected: '&eYou need to make a selection before creating a terrain. You can select with the command &f&n/<label> pos1&e and &f&n/<label> pos2&e or with the selection wand: &f&n/<label> wand&e.'
                Overlap: '&4Unable to claim because this terrain would overlap &7<other>&4 terrain!'
                Overlap Several: '&4Unable to claim because this terrain would overlap other terrains!'
                Too Small: '&4Area too small! Terrains must have at least &6<min>&4 blocks.'
                Unknown: '&4An unknown error occurred while creating this terrain.'
                World No Longer Exists: '&4The world in your selections no longer exists.'
              Success: '&2Terrain ''&a<name>&2'' claimed successfully! Used block limit: &7<used>&f/&7<max>'
              Define: '&2Terrain ''&a<name>&2'' defined successfully with all protection flags!'
              # <owner> for the terrain owner's name.
              # <number> for the next available terrain name number.
              Default Name: '<owner>_<number>'
                        
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
              Cauldrons: '<cooldown=2000> &4You''re not allowed to use cauldrons here.'
              Command Blacklist: '<cooldown=2000> &4You''re not allowed to use this command here.'
              Containers: '<cooldown=2000> &4You''re not allowed to open containers here.'
              Doors: '<cooldown=2000> &4You''re not allowed to open doors or gates here.'
              Eat: '<cooldown=2000> &4You''re not allowed to consume items here.'
              Enemy Harm: '<cooldown=2000> &4You''re not allowed to harm enemy entities here.'
              Enter: '<cooldown=2000> &4You''re not allowed to enter.'
              Enter Flying: '<cooldown=2000> &4You''re not allowed to enter while flying.'
              Enter Gliding: '<cooldown=2000> &4You''re not allowed to enter while gliding.'
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
                Blacklisted: '&4You can''t name the terrain that.'
                Name Length: '&4Terrain names must have at least 1 character and &7<max>&4 characters max!'
                Same: '&7Nothing changed. Terrain is already named &f<name>&7.'
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
                Suggest: '<cooldown=30000> &ePositions were selected successfully. Now to create a terrain, use the command &7&n/<label> claim [name]&e.'
                Suggest Resize: '<cooldown=30000> &ePositions were selected successfully. Confirm the new size of &f<terrain>&e with the command &7&n/<label> confirm&e.'
                        
            Selector Wand:
              Display Name: '&6&l&nClaim Selection Wand'
              Lore: |-
                &7Click with the selection wand on the ground
                &7to mark the two diagonals of your terrain.
                &7Once marked, use &f&n/tr claim&7 to claim and
                &7protect your terrain!
                        
            Info Wand:
              Display Name: '&7&l&nTerrain Info Wand'
              Lore: |-
                &7Click with the info wand on the ground to
                &7check information about any existing terrains
                &7in the location.
                        
            Info:
              Error:
                No Terrains: '<cooldown=1000> &7No terrains could be found.'
                No Relating Terrains: '<cooldown=1000> &7No terrains that you have relations could be found.'
              Global Terrain: '&7No terrains found in location, showing info of global world terrain.'
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
                Lore: |-
                  Please type in what do you want to input.
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
                Header In This World:
                  You: '&8Your limits in this world:'
                  Other: '&8<other>''s limits in the world <world>:'
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
                        
            Resize:
              Cancelled: '&7The resize operation of &f<terrain>&7 was cancelled because you claimed a new terrain.'
              Confirmation Description: 'Resize terrain <terrain>'
              Error:
                World Terrain: '&4Global world terrains can not be resized!'
              Select: '&eTo resize the terrain, grab your selection wand and mark the new diagonals, then type &7&n/<label> confirm&e to confirm.'
              Success: '&2Terrain ''&a<terrain>&2'' resized successfully! Used block limit: &7<used>&f/&7<max>'
                        
            Priority:
              Error:
                No Terrains: '<cooldown=1000> &7No terrains could be found.'
              Here: '&7Priority of terrains in the current location:'
              Overlapping: '&7Terrain &f<terrain>&7 has priority &f<priority>&7 and is overlapping the following terrains:'
              Priority: '&8- &f<terrain>&7 = &f<priority>'
              Removed: '<noprefix> &7Some terrains are not in the list because you don''t own them.'
              Same:
                Here: '&7All terrains in this location have priority &f<priority>&7: &f<terrains>'
              Select: '&cSelect the terrain to edit priority:'
              Set: '&aPriority set to &f<new>&a for terrain: &f<terrain>&a.'
              Single: '&7Terrain &f<terrain>&7 has &f<priority>&7 priority.'
              Unknown: 'Unknown' # Unknown priority
                        
            Teleport:
              Above: '<noprefix> &eYou were teleported ABOVE the terrain, because the location was obstructed.'
              Delay: '<noprefix> &7You will be teleported in <delay> second(s)...'
              Error:
                Already Teleporting: '<noprefix> &4You are already in the process of teleporting to a terrain.'
                Default: '<noprefix> &4You could not be teleported to &7<terrain>&4.'
                Moved: '<noprefix> &4The teleportation was cancelled because you moved'
                Other: '&4<player> could not be teleported to &7<terrain>&4.'
              Select: '&cSelect the terrain to teleport to:'
              Success:
                Default: '<noprefix> &aYou were teleported to &7<terrain>&a.'
                Other: '&a<player> was teleported to &7<terrain>&a.'
                        
            Wand:
              Bought: '&aYou''ve bought a &7<type>&a for &6<price>$&a.'
              Cooldown: '&4You must wait <remaining> seconds to buy this again.'
              Given: '&aGave &7<player>&a a &7<type>&a for &6<price>$&a.'
              Received: '&aYou''ve received a &7<type>&a from &7<player>&a.'
                        
            Shop:
              Blocks:
                Option 1:
                  Display Name: '&7&l<var0> BLOCKS'
                  Lore: |-
                    &8Buy &7<var0>&8 blocks for
                    &8only &6<var1>$
                Option 2:
                  Display Name: '&6&l<var0> BLOCKS'
                  Lore: |-
                    &8Buy &7<var0>&8 blocks for
                    &8only &6<var1>$
                    &82% off
                Option 3:
                  Display Name: '&b&l<var0> BLOCKS'
                  Lore: |-
                    &8Buy &7<var0>&8 blocks for
                    &8only &6<var1>$
                    &85% off
              Claims:
                Option 1:
                  Display Name: '&4&l<var0> CLAIMS'
                  Lore: |-
                    &8Buy &7<var0>&8 claim for
                    &8only &6<var1>$
                Option 2:
                  Display Name: '&4&l<var0> CLAIMS'
                  Lore: |-
                    &8Buy &7<var0>&8 claims for
                    &8only &6<var1>$
                    &82% off
                Option 3:
                  Display Name: '&4&l<var0> CLAIMS'
                  Lore: |-
                    &8Buy &7<var0>&8 claims for
                    &8only &6<var1>$
                    &85% off
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
              Location:
                Multiple: '&4More than one terrain was found in the location, please specify the terrain''s name or ID. Ex: &7&n/<label> <args>'
                Not Found: '&4No terrain was found in this location, please specify the terrain''s name or ID. Ex: &7&n/<label> <args>'
              Name:
                Multiple: '&4More than one terrain was found with that name. Please specify the terrain''s ID instead of name.'
                Not Found: '&4No terrain matching the specified name or ID was found.'
              No Permission: '&4You don''t have permission to do that in this terrain.'
              Only World Terrain: '&4No terrain was found in this location. &eIf you want to edit the world''s global terrain, use &7&n/<label> <args> --t <world>'
                        
            Flags:
              Allow: '&a&lALLOW'
              Deny: '&c&lDENY'
              Undefined: '&7&lUndefined'
              Set: '&7Flag <flag>&7 set for the terrain &f<name>&7 with value: &f<state>&7.'
              Unset: '&7Flag <flag>&7 removed from terrain &f<name>&7.'
              Default Alert: '&7Terrain &f<name>&7 will now use the default value of <flag>&7: &f<state>&7.'
              Error:
                Default: '&4Unable to set flag <flag>&4: &f<message>'
                Message Location: '&4The only accepted values for message location are: &7ActionBar&4, &7BossBar&4, &7Chat&4, &7Title&4 or &7NONE&4.'
                Not Owner: '&4Only the owner is allowed to edit this flag.'
                Unknown: '&4Something went wrong while setting this flag. Please contact an administrator.'
                Unknown Effect: '&4Invalid potion effect: &f<value>'
              Management GUI Title: '&2Flags of <terrain>&2:'
              Select: '&cSelect the terrain to edit flags:'
              Values:
                Anvils:
                  Display Name: '&x&5&B&5&B&5&B&lAnvils'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether anvils can be used.
                Armor Stands:
                  Display Name: '&x&B&3&A&2&9&3&lArmor Stands'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether armor stands can be used.
                    &7Breaking armor stands is handled by 'Build'
                    &7flag.
                Block Form:
                  Display Name: '&x&D&8&E&7&F&0&lBlock Form'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether blocks can form, such as: ice, snow,
                    &7cobblestone, obsidian, concrete, etc.
                    &7Blocks forming from outside is handled by
                    &7'Build' flag.
                Block Spread:
                  Display Name: '&x&1&D&2&7&3&3&lBlock Spread'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether blocks can spread, such as: grass,
                    &7mushrooms and sculk.
                    &7Blocks spreading from outside is handled by
                    &7'Build' flag.
                Build:
                  Display Name: '&c&lBuild'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can build.
                Build Boats:
                  Display Name: '&x&6&6&4&2&2&8&lBuild Boats'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can place and break boats.
                Build Minecarts:
                  Display Name: '&8&lBuild Minecarts'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can place and break minecarts.
                    &7TNT minecarts are handled by 'Build' flag.
                Buttons:
                  Display Name: '&x&6&6&4&2&2&8&lButtons'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can use buttons and levers.
                Cauldrons:
                  Display Name: '&x&5&B&5&B&5&B&lCauldrons'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can use cauldrons.
                Cauldrons Change Level Naturally:
                  Display Name: '&x&5&B&5&B&5&B&lCauldrons Change Level Naturally'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether cauldrons can fill or empty naturally.
                    &7Possible causes: rain, dripstone, entities
                    &7extinguishing fire.
                Command Blacklist:
                  Display Name: '&c&lCommand Blacklist'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7A list of commands, separated by comma, that
                    &7are not allowed to be executed in the terrain.
                    &7Add &2*&7 to the list to make it a whitelist.
                Containers:
                  Display Name: '&6&lContainers'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can use containers.
                Dispensers:
                  Display Name: '&8&lDispensers'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether dispensers can fire.
                Doors:
                  Display Name: '&x&8&B&5&F&2&B&lDoors'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can use doors and gates.
                Eat:
                  Display Name: '&x&A&4&5&5&3&5&lEat'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can eat food.
                Effects:
                  Display Name: '&b&lEffects'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7A list of effects to set to the players.
                    &7Example:
                    &7  &2speed=0,haste=2
                Enemy Harm:
                  Display Name: '&7&lEnemy Harm'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can harm enemy entities.
                    &7Entities inside the terrain will not be harmed
                    &7by entities that spawned outside.
                Enter:
                  Display Name: '&2&lEnter'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether players can enter.
                Enter Console Commands:
                  Display Name: '&d&lEnter Console Commands'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7A list of commands to execute on console when
                    &7a player enters the terrain.
                    &7Use &2%p&7 for the player's name.
                Enter Player Commands:
                  Display Name: '&d&lEnter Player Commands'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7A list of commands to execute as the player
                    &7when a player enters the terrain.
                    &7Use &2%p&7 for the player's name.
                Enter Vehicles:
                  Display Name: '&x&D&7&6&5&2&B&lEnter Vehicles'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether vehicles can be mounted, such as:
                    &7horses, minecarts, boats, llamas, etc.
                Entity Harm:
                  Display Name: '&d&lEntity Harm'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether entities can be harmed.
                    &7Enemy entities are controlled by 'Enemy Harm'
                    &7flag.
                    &7Entities inside the terrain will not be harmed
                    &7by entities that spawned outside.
                Entity Interactions:
                  Display Name: '&x&A&4&6&6&3&C&lEntity Interactions'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can interact with entities.
                    &7Examples of entity interactions: talking
                    &7with villagers, curing zombies, breeding
                    &7animals, using leads, etc.
                Explosion Damage:
                  Display Name: '&4&lExplosion Damage'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether blocks can be damaged by explosives.
                    &7Explosives from outside will be handled by
                    &7'Build' flag.
                Fire Damage:
                  Display Name: '&6&lFire Damage'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether blocks can be damaged by fire.
                    &7Fire from outside will be handled by 'Build'
                    &7flag.
                Fire Spread:
                  Display Name: '&6&lFire Spread'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether fire can spread.
                    &7Fire from outside will be handled by 'Build'
                    &7flag.
                Fly:
                  Display Name: '&f&lFly'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether players with fly permission can fly.
                Frost Walk:
                  Display Name: '&x&B&E&D&3&E&5&lFrost Walk'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can form ice using frost walk
                    &7enchanted boots.
                Glide:
                  Display Name: '&8&lGlide'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether players can glide with elytras.
                Interactions:
                  Display Name: '&x&6&6&4&2&2&8&lInteractions'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can right click interactable
                    &7blocks.
                    &7Examples of interactions: using flower pots,
                    &7changing repeater levels, filling cauldrons,
                    &7waxing blocks, using bells, etc.
                Item Drop:
                  Display Name: '&8&lItem Drop'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can drop items.
                Item Frames:
                  Display Name: '&e&lItem Frames'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can put items on or rotate
                    &7item frames.
                    &7Taking items off is handled by 'Build' flag.
                Item Pickup:
                  Display Name: '&8&lItem Pickup'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can pick up items.
                Leaf Decay:
                  Display Name: '&x&1&F&5&C&1&4&lLeaf Decay'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether leaves can decay naturally.
                Leave:
                  Display Name: '&4&lLeave'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether players can leave.
                Leave Console Commands:
                  Display Name: '&d&lLeave Console Commands'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7A list of commands to execute on console when
                    &7a player leaves the terrain.
                    &7Use &2%p&7 for the player's name.
                Leave Message:
                  Display Name: '&7&lLeave Message'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7The message shown when someone leaves the
                    &7terrain.
                    &7The location of the message will be the same
                    &7as the one set in 'Message Location' flag.
                Leave Player Commands:
                  Display Name: '&d&lLeave Player Commands'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7A list of commands to execute as the player
                    &7when a player leaves the terrain.
                    &7Use &2%p&7 for the player's name.
                Lighters:
                  Display Name: '&8&lLighters'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can use lighters such as
                    &7fireballs or flint and steel to light fire,
                    &7campfires and candles.
                Liquid Flow:
                  Display Name: '&1&lLiquid Flow'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether liquids such as lava and water can
                    &7flow.
                    &7Liquids flowing from outside is handled by
                    &7'Build' flag.
                Message Location:
                  Display Name: '&7&lMessage Location'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7The location where the enter and leave
                    &7messages will be.
                    &7Locations:
                        
                    &2NONE &7| &2ACTIONBAR &7| &2BOSSBAR &7| &2CHAT &7| &2TITLE
                        
                    &7The description of the terrain is used as
                    &7enter message.
                    &7To set a leave message, use the
                    &7'Leave Message' flag.
                Mob Spawn:
                  Display Name: '&x&4&D&7&E&3&A&lMob Spawn'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether mobs can spawn within the terrain.
                    &7Spawner mobs are controlled by 'Spawners'
                    &7flag.
                Mods Can Edit Flags:
                  Display Name: '&x&6&7&2&6&3&0&lMods Can Edit Flags'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether moderators can edit terrain flags,
                    &7except those who control moderator
                    &7permissions.
                Mods Can Manage Mods:
                  Display Name: '&x&6&7&2&6&3&0&lMods Can Manage Mods'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether moderators can grant or revoke the
                    &7moderator role to/from other players.
                Outside Dispensers:
                  Display Name: '&7&lOutside Dispensers'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether dispensers from outside the terrain
                    &7can fire into the terrain.
                Outside Pistons:
                  Display Name: '&x&8&F&B&F&4&5&lOutside Pistons'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether pistons from outside the terrain
                    &7can push or pull blocks within.
                Outside Projectiles:
                  Display Name: '&x&A&0&6&A&4&2&lOutside Projectiles'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether projectiles from outside can land
                    &7within.
                Pistons:
                  Display Name: '&e&lPistons'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether pistons can move.
                Plant:
                  Display Name: '&a&lPlant'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can plant on farmland.
                Plant Grow:
                  Display Name: '&a&lPlant Grow'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether crops, grass, saplings can grow using
                    &7bone meal, or naturally.
                    &7Outside plants that grow blocks inside will be
                    &7handled by 'Build' flag.
                Plow:
                  Display Name: '&f&lPlow'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can use hoes to make
                    &7farmland.
                Potions:
                  Display Name: '&5&lPotions'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can drink or throw potions.
                Prepare:
                  Display Name: '&x&A&C&6&8&3&B&lPrepare'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can use prepare blocks, such
                    &7as: cartography tables, crafting tables,
                    &7enchanting tables, grindstones, looms, smithing
                    &7tables, and stonecutters.
                Pressure Plates:
                  Display Name: '&x&8&E&6&6&1&B&lPressure Plates'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can step on pressure plates.
                Projectiles:
                  Display Name: '&x&D&5&D&5&D&5&lProjectiles'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can shoot projectiles.
                PvP:
                  Display Name: '&x&2&A&C&5&A&A&lPvP'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether players can engage in combat.
                Show Borders:
                  Display Name: '&6&lShow Borders'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether particles will show in the borders
                    &7of the terrain.
                Sign Click:
                  Display Name: '&x&8&E&6&6&1&B&lSign Click'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can click on signs.
                Sign Edit:
                  Display Name: '&x&3&E&2&F&2&3&lSign Edit'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can edit signs.
                Spawners:
                  Display Name: '&x&1&D&2&7&3&3&lSpawners'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether mobs with spawn reason 'Spawners'
                    &7will spawn.
                Sponges:
                  Display Name: '&e&lSponges'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether sponges will soak up water.
                    &7Sponges outside the terrain are handled by
                    &7'Build' flag.
                Trample:
                  Display Name: '&x&A&C&6&8&3&B&lTrample'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether everyone can trample on farmland.
                Vulnerability:
                  Display Name: '&x&F&F&C&C&0&0&lVulnerability'
                  Lore: |-
                    &7Value: &f<var0>
                        
                    &7Whether players will lose health or
                    &7saturation.
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
