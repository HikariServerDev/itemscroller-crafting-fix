Unofficial Minecraft 1.21.11 port by HikariServerDev. Modified on October 6, 2026.
## Note
This is a customized version of Masa's itemscroller mod that fixes crafting features for 1.18. Masa's original mod can be found [here](https://github.com/maruohon/itemscroller)

Customizations:
* More accurate/faster crafting through recipe book protocol
* Toggleable crafting (so you can keep crafting without holding down a key, eg for crafting millions of pistons)
* Honey crafting

- Removed carpetControlQ crafting option as it causes a "slow crafting issue"
- Removed packetRateLimit as it may lead to problems.

### Minecraft 1.21.11 port
This branch is ported to Minecraft 1.21.11 (Yarn mappings, malilib 0.27.x, Java 21). Notable changes compared to the 1.21 version:
* Since 1.21.2 the client no longer knows the actual crafting recipes, only the recipe book *displays* of the recipes the player has unlocked,
  and the recipe book click packet uses a per-session recipe id. The recipe book lookup therefore matches the stored recipe pattern
  (result + ingredient layout) against the player's recipe book and re-resolves it whenever the recipe book changes.
  Recipes that aren't unlocked in the recipe book fall back to the old slot-by-slot item movement.
* Rendering was ported to the new GUI renderer (`GuiContext` from malilib).

## This is not Masa's original itemscroller. This is an unofficial 1.21.11 port. For issues with this port, please open an issue in this repository.
### What's different?
Post 1.13, Mojang has changed the crafting mechanics of the game. Before 1.13, crafting was very fast as much of the logic was handled client-side. In 1.13, most of the crafting logic was moved to the server. This broke Itemscroller's fast crafting features, since every ingredient now had to be moved one slot at a time to the crafting grid for it to work. This drastically worsened server-client desync, a compounding problem, leading to an increasing number of failed crafting attempts and accidental ingredient leaks which made afk crafting impossible. 

This customized version of the mod, fixes the problem by handling ingredient movement server-side using the recipe book protocols when it can. 

**Note: Some recipes like fireworks rockets that are not in the recipe book do not take advantage of this protocol, in those cases old itemscroller methods will be used**

Item Scroller
==============
Item Scroller is a Minecraft mod that adds various convenience features for moving items
inside inventory GUIs. Examples are scrolling the mouse wheel over slots with items in them
or Shift/Ctrl + click + dragging over slots to move items from them in various ways etc.

Item scrolling is basically what the old NEI mod did and Mouse Tweaks also does.
This mod has some different drag features compared to Mouse Tweaks, and also some special
villager trading related helper features as well as crafting helper features.

For more information and downloads of the already compiled builds,
see https://www.curseforge.com/minecraft/mc-mods/item-scroller

Compiling
=========
* Clone the repository
* Open a command prompt/terminal to the repository directory
* run 'gradlew build'
* The built jar file will be in build/libs/
