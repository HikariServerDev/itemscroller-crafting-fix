package fi.dy.masa.itemscroller.recipes;

import java.util.HashSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import java.util.Arrays;
import javax.annotation.Nonnull;

import fi.dy.masa.itemscroller.compat.carpet.StackingShulkerBoxes;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtList;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import fi.dy.masa.itemscroller.recipes.CraftingHandler.SlotRange;
import fi.dy.masa.itemscroller.util.Constants;
import fi.dy.masa.itemscroller.util.InventoryUtils;

public class RecipePattern
{
    private ItemStack result = InventoryUtils.EMPTY_STACK;
    private ItemStack[] recipe = new ItemStack[9];
    // The server side recipe ids can change (reconnecting, /reload), but then the recipe book gets rebuilt
    // and returns a new list of results, so the identity of that list tells if the cached id is still valid.
    private NetworkRecipeId cachedRecipeId = null;
    private List<RecipeResultCollection> cachedRecipeBookResults = null;

    private int maxCraftAmount = 64;
    private HashSet<Item> recipeRemainders = new HashSet<Item>();

    public RecipePattern()
    {
        this.ensureRecipeSizeAndClearRecipe(9);
    }

    public void ensureRecipeSize(int size)
    {
        if (this.getRecipeLength() != size)
        {
            this.recipe = new ItemStack[size];
        }
    }

    public void clearRecipe()
    {
        Arrays.fill(this.recipe, InventoryUtils.EMPTY_STACK);
        this.result = InventoryUtils.EMPTY_STACK;
        this.resetCachedRecipeId();
        this.maxCraftAmount = 64;
        this.recipeRemainders.clear();
    }

    public void ensureRecipeSizeAndClearRecipe(int size)
    {
        this.ensureRecipeSize(size);
        this.clearRecipe();
    }

    public void initializeRecipe() {
        for (int i = 0; i < this.recipe.length; i++) {
            ItemStack remainder = this.recipe[i].getItem().getRecipeRemainder();
            if (remainder.isEmpty() == false) {
                this.recipeRemainders.add(remainder.getItem());
            }
            if (this.recipe[i].getItem() == Items.AIR) {
                continue;
            }
            int maxCount = StackingShulkerBoxes.getMaxCount(this.recipe[i]);
            if (maxCount < maxCraftAmount) {
                maxCraftAmount = maxCount;
            }
        }

        this.resetCachedRecipeId();
    }

    private void resetCachedRecipeId() {
        this.cachedRecipeId = null;
        this.cachedRecipeBookResults = null;
    }

    /**
     * @return the recipe book id of the crafting recipe matching this pattern, or null if the recipe isn't in the recipe book
     */
    @Nullable
    public NetworkRecipeId getCraftingRecipeId() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (player == null) {
            return null;
        }

        List<RecipeResultCollection> bookResults = player.getRecipeBook().getOrderedResults();

        // only search the recipe book again when it has changed
        if (bookResults != this.cachedRecipeBookResults) {
            this.cachedRecipeId = InventoryUtils.findBookRecipeIdFromPattern(this, bookResults);
            this.cachedRecipeBookResults = bookResults;
        }

        return this.cachedRecipeId;
    }

    public int getMaxCraftAmount() {
        return maxCraftAmount;
    }

    public void storeCraftingRecipe(Slot slot, HandledScreen<? extends ScreenHandler> gui, boolean clearIfEmpty)
    {
        SlotRange range = CraftingHandler.getCraftingGridSlots(gui, slot);

        if (range != null)
        {
            if (slot.hasStack())
            {
                int gridSize = range.getSlotCount();
                int numSlots = gui.getScreenHandler().slots.size();

                this.ensureRecipeSizeAndClearRecipe(gridSize);

                for (int i = 0, s = range.getFirst(); i < gridSize && s < numSlots; i++, s++)
                {
                    Slot slotTmp = gui.getScreenHandler().getSlot(s);
                    this.recipe[i] = slotTmp.hasStack() ? slotTmp.getStack().copy() : InventoryUtils.EMPTY_STACK;
                }

                this.result = slot.getStack().copy();
                this.initializeRecipe();
            }
            else if (clearIfEmpty)
            {
                this.clearRecipe();
            }
        }
    }

    public void copyRecipeFrom(RecipePattern other)
    {
        int size = other.getRecipeLength();
        ItemStack[] otherRecipe = other.getRecipeItems();

        this.ensureRecipeSizeAndClearRecipe(size);

        for (int i = 0; i < size; i++)
        {
            this.recipe[i] = InventoryUtils.isStackEmpty(otherRecipe[i]) == false ? otherRecipe[i].copy() : InventoryUtils.EMPTY_STACK;
        }

        this.result = InventoryUtils.isStackEmpty(other.getResult()) == false ? other.getResult().copy() : InventoryUtils.EMPTY_STACK;
        this.initializeRecipe();
    }

    public void readFromNBT(@Nonnull NbtCompound nbt, DynamicRegistryManager registryManager)
    {
        if (nbt.getCompound("Result").isPresent() && nbt.getList("Ingredients").isPresent())
        {
            NbtList tagIngredients = nbt.getListOrEmpty("Ingredients");
            int count = tagIngredients.size();
            int length = nbt.getInt("Length", 0);

            if (length > 0)
            {
                this.ensureRecipeSizeAndClearRecipe(length);
            }

            for (int i = 0; i < count; i++)
            {
                NbtCompound tag = tagIngredients.getCompoundOrEmpty(i);
                int slot = tag.getInt("Slot", -1);

                if (slot >= 0 && slot < this.recipe.length)
                {
                    this.recipe[slot] = decodeStack(registryManager, tag);
                }
            }

            this.result = decodeStack(registryManager, nbt.getCompoundOrEmpty("Result"));
            this.initializeRecipe();
        }
    }

    private static ItemStack decodeStack(DynamicRegistryManager registryManager, NbtCompound tag)
    {
        return ItemStack.CODEC.parse(registryManager.getOps(NbtOps.INSTANCE), tag).result().orElse(ItemStack.EMPTY);
    }

    private static NbtCompound encodeStack(DynamicRegistryManager registryManager, ItemStack stack)
    {
        NbtElement element = ItemStack.CODEC.encodeStart(registryManager.getOps(NbtOps.INSTANCE), stack).result().orElse(null);
        return element instanceof NbtCompound compound ? compound : new NbtCompound();
    }

    @Nonnull
    public NbtCompound writeToNBT(DynamicRegistryManager registryManager)
    {
        NbtCompound nbt = new NbtCompound();

        if (this.isValid())
        {
            NbtCompound tag = encodeStack(registryManager, this.result);

            nbt.putInt("Length", this.recipe.length);
            nbt.put("Result", tag);

            NbtList tagIngredients = new NbtList();

            for (int i = 0; i < this.recipe.length; i++)
            {
                if (this.recipe[i].isEmpty() == false && InventoryUtils.isStackEmpty(this.recipe[i]) == false)
                {
                    tag = new NbtCompound();
                    tag.copyFrom(encodeStack(registryManager, this.recipe[i]));

                    tag.putInt("Slot", i);
                    tagIngredients.add(tag);
                }
            }

            nbt.put("Ingredients", tagIngredients);
        }

        return nbt;
    }

    public ItemStack getResult()
    {
        return this.result;
    }

    public int getRecipeLength()
    {
        return this.recipe.length;
    }

    public ItemStack[] getRecipeItems()
    {
        return this.recipe;
    }

    public HashSet<Item> getRecipeRemainders()
    {
        return this.recipeRemainders;
    }

    public boolean isValid()
    {
        return InventoryUtils.isStackEmpty(this.getResult()) == false;
    }
}
