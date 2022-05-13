package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.enchanting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.implementation.operations.CraftingOperation;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * This is a super class of the {@link AutoEnchanter} and {@link AutoDisenchanter} which is
 * used to streamline some methods and combine common attributes to reduce redundancy.
 *
 * @author TheBusyBiscuit
 * @author Rothes
 *
 * @see AutoEnchanter
 * @see AutoDisenchanter
 *
 */
abstract class AbstractEnchantmentMachine extends AContainer {

    private final ItemSetting<Boolean> useLevelLimit = new ItemSetting<>(this, "use-enchant-level-limit", false);
    private final IntRangeSetting levelLimit = new IntRangeSetting(this, "enchant-level-limit", 0, 10, Short.MAX_VALUE);
    private final ItemSetting<Boolean> useIgnoredLores = new ItemSetting<>(this, "use-ignored-lores", false);
    private final ItemSetting<List<String>> ignoredLores = new ItemSetting<>(this, "ignored-lores", Collections.singletonList("&7- &c无法被使用在 " + this.getItemName() + "上"));

    @ParametersAreNonnullByDefault
    protected AbstractEnchantmentMachine(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        addItemSetting(useLevelLimit);
        addItemSetting(levelLimit);
        addItemSetting(useIgnoredLores);
        addItemSetting(ignoredLores);
    }

    protected boolean isEnchantmentLevelAllowed(int enchantmentLevel) {
        return !useLevelLimit.getValue() || levelLimit.getValue() >= enchantmentLevel;
    }

    protected void showEnchantmentLevelWarning(@Nonnull BlockMenu menu) {
        if (!useLevelLimit.getValue()) {
            throw new IllegalStateException("自动附/祛魔机等级限制未被启用, 无法展示警告信息.");
        }

        String notice = ChatColors.color(Slimefun.getLocalization().getMessage("messages.above-limit-level"));
        notice = notice.replace("%level%", String.valueOf(levelLimit.getValue()));
        ItemStack progressBar = new CustomItemStack(Material.BARRIER, " ", notice);
        menu.replaceExistingItem(22, progressBar);
    }

    protected boolean hasIgnoredLore(@Nonnull ItemStack item) {
        if (useIgnoredLores.getValue() && item.hasItemMeta()) {
            ItemMeta itemMeta = item.getItemMeta();

            if (itemMeta.hasLore()) {
                List<String> itemLore = itemMeta.getLore();
                List<String> ignoredLore = ignoredLores.getValue();

                // Check if any of the lines are found on the item
                for (String lore : ignoredLore) {
                    if (itemLore.contains(ChatColors.color(lore))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 检测当前输入栏位置的物品是否与 recipe 预期值匹配
     */
    private boolean inputChanged(Block b) {
        BlockMenu inv = BlockStorage.getInventory(b);

        MachineProcessor<CraftingOperation> processor = getMachineProcessor();
        List<ItemStack> recipeInputItems = new ArrayList<>(Arrays.asList(processor.getOperation(b).getIngredients()));

        List<ItemStack> currentInputItems = new ArrayList<>();
        for (int slot : getInputSlots()) {
            currentInputItems.add(inv.getItemInSlot(slot));
        }

        for (ItemStack currentItem : currentInputItems) {
            recipeInputItems.remove(currentItem);
        }

        return recipeInputItems.size() != 0;
    }

    @Override
    protected void tick(Block b) {
        BlockMenu inv = BlockStorage.getInventory(b);
        MachineProcessor<CraftingOperation> processor = getMachineProcessor();
        CraftingOperation currentOperation = processor.getOperation(b);

        if (currentOperation != null) {
            if (takeCharge(b.getLocation())) {

                if (!currentOperation.isFinished()) {

                    for (int slot : getOutputSlots()) {
                        if (inv.getItemInSlot(slot) != null) {
                            inv.replaceExistingItem(22, new CustomItemStack(Material.BARRIER, "&b暂停工作", "&a请清空右侧输出栏内物品"));
                            return;
                        }
                    }

                    if (inputChanged(b)) {
                        inv.replaceExistingItem(22, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "));
                        processor.endOperation(b);
                        return;
                    }

                    processor.updateProgressBar(inv, 22, currentOperation);
                    currentOperation.addProgress(1);
                } else {
                    inv.replaceExistingItem(22, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "));

                    for (int inputSlot : getInputSlots()) {
                        inv.consumeItem(inputSlot);
                    }

                    for (ItemStack output : currentOperation.getResults()) {
                        inv.pushItem(output.clone(), getOutputSlots());
                    }

                    processor.endOperation(b);
                }
            }
        } else {
            MachineRecipe next = findNextRecipe(inv);

            if (next != null) {
                processor.startOperation(b, new CraftingOperation(next));
            }
        }
    }
}