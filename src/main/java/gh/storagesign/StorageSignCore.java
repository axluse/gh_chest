package gh.storagesign;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;

/**
 * 看板チェストのコアプログラム
 */
public class StorageSignCore extends JavaPlugin implements Listener
{
    // コンフィグファイル
	FileConfiguration config;

    /**
     * サーバー起動時処理
     */
    @Override public void onEnable() {
		config = this.getConfig();
		config.options().copyDefaults(true);
		config.options().header("StorageSign Configuration");
		this.saveConfig();

		//鯖別レシピが実装されたら
		//ShapedRecipe storageSignRecipe = new ShapedRecipe(new NamespacedKey(this,"ssr"),StorageSign.EmptySign());

        // レシピの追加
        {
            ShapedRecipe storageSignRecipe = new ShapedRecipe( StorageSign.EmptySign() );
            storageSignRecipe.shape( "CCC", "CSC", "CHC" );
            storageSignRecipe.setIngredient( 'C', Material.CHEST );
            storageSignRecipe.setIngredient( 'S', Material.SIGN );

            if (config.getBoolean( "hardrecipe" ))
            {
                storageSignRecipe.setIngredient( 'H', Material.ENDER_CHEST );
            }

            else
            {
                storageSignRecipe.setIngredient( 'H', Material.CHEST );
            }

            getServer().addRecipe( storageSignRecipe );
        }

		getServer().getPluginManager().registerEvents(this, this);

		if(config.getBoolean("no-bud"))
        {
            new SignPhysicsEvent(this);
        }
	}

	/** サーバー終了時処理 */
	@Override public void onDisable(){}

    /**
     * 指定ItemStackを見て、看板チェストであるか判断する
     * @param item
     * @return
     */
	public boolean isStorageSign(ItemStack item)
    {
        //  アイテムなければfalse
		if (item == null) return false;
		// 看板以外のアイテムだったらfalse
		if (item.getType() != Material.SIGN) return false;
		// アイテムに名前がついていない場合false
		if (!item.getItemMeta().hasDisplayName()) return false;
		// アイテムの名前が'StorageSign'以外ならばfalse
		if (!item.getItemMeta().getDisplayName().matches("StorageSign")) return false;

		return item.getItemMeta().hasLore();
	}

    /**
     * 指定ブロックを見て、看板チェストであるか判断する
     * @param block
     * @return
     */
	public boolean isStorageSign(Block block)
    {
        // 壁がけの看板か地面に直置きされている看板ならば
		if (block.getType() == Material.SIGN_POST || block.getType() == Material.WALL_SIGN)
		{
			Sign sign = (Sign) block.getState();
			// 名前がStorageSign?
			if (sign.getLine(0).matches("StorageSign")) return true;
		}
		return false;
	}

	public boolean isHorseEgg(ItemStack item)
    {
		if(item.getType() != Material.MONSTER_EGG) return false;
		if(item.getItemMeta().hasLore()) return true;
		return false;
	}

    /**
     * プレイヤーに提供する挙動
     * @param event
     */
	@EventHandler public void onPlayerInteract(PlayerInteractEvent event)
    {
		Player player = event.getPlayer();
		Block block = null;
		// TODO:手持ちがブロックだと叩いた看板を取得できないことがあるとかあるらしい
		if (event.isCancelled() && event.getAction() == Action.RIGHT_CLICK_AIR)
		{
		    // 叩いたブロックを取得
			try
            {
				block = player.getTargetBlock((Set) null, 3);
			}
			catch (IllegalStateException ex)
            {
				return;
			}
		}
		else
        {
			block = event.getClickedBlock();
		}
		if (block == null) return;
		if(player.getGameMode() == GameMode.SPECTATOR) return;
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)
		{

			if (!isStorageSign(block)) return;
			if(event.getHand() == EquipmentSlot.OFF_HAND) return;//一応
			event.setUseItemInHand(Result.DENY);
			event.setUseInteractedBlock(Result.DENY);
			if (!player.hasPermission("storagesign.use"))
			{
				player.sendMessage(ChatColor.RED + config.getString("no-permisson"));
				event.setCancelled(true);
				return;
			}
			Sign sign = (Sign) block.getState();
			StorageSign storageSign = new StorageSign(sign);
			ItemStack itemMainHand = event.getItem();
			Material mat;


			//アイテム登録
			if (storageSign.GetMaterial() == null || storageSign.GetMaterial() == Material.AIR)
			{
				if(itemMainHand == null) return;//申し訳ないが素手はNG
				mat = itemMainHand.getType();
				if (isStorageSign(itemMainHand)) storageSign.SetMaterial(Material.PORTAL);
				else if (isHorseEgg(itemMainHand))
				{
					storageSign.SetMaterial(Material.PORTAL);
					storageSign.SetDamage((short) 1);
				}
				else if (mat == Material.POTION || mat == Material.SPLASH_POTION || mat == Material.LINGERING_POTION)
				{
					storageSign.SetMaterial(mat);
					PotionMeta potionMeta = (PotionMeta)itemMainHand.getItemMeta();
					PotionData potion = potionMeta.getBasePotionData();
					if(potion.isExtended()) storageSign.SetDamage((short) 1);
					if(potion.isUpgraded()) storageSign.SetDamage((short) 2);
					storageSign.SetPotion(potion.getType());
				}
				else if (mat == Material.ENCHANTED_BOOK)
				{
					EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta)itemMainHand.getItemMeta();
					if(enchantMeta.getStoredEnchants().size() == 1) {
						Enchantment ench = enchantMeta.getStoredEnchants().keySet().toArray(new Enchantment[0])[0];
						storageSign.SetMaterial(mat);
						storageSign.SetDamage((short) enchantMeta.getStoredEnchantLevel(ench));
						storageSign.SetEnchant(ench);
					}
				}
				else if(mat == Material.FIREWORK)
				{
					storageSign.SetMaterial(mat);
					FireworkMeta fireworkMeta = (FireworkMeta)itemMainHand.getItemMeta();
					storageSign.SetDamage((short) fireworkMeta.getPower());
				}
				else
				{
					storageSign.SetMaterial(mat);
					storageSign.SetDamage(itemMainHand.getDurability());
				}

				for (int i=0; i<4; i++) sign.setLine(i, storageSign.GetSignText(i));
				sign.update();
				return;
			}

			if (isStorageSign(itemMainHand)) {
				//看板合成
				StorageSign itemSign = new StorageSign(itemMainHand);
				if (storageSign.GetContents().isSimilar(itemSign.GetContents()) && config.getBoolean("manual-import")) {
					storageSign.AddAmount(itemSign.GetAmount() * itemSign.GetStackSize());
					itemSign.SetAmount(0);
					player.getInventory().setItemInMainHand(itemSign.GetStorageSign());
				}//空看板収納
				else if (itemSign.IsEmpty() && storageSign.GetMaterial() == Material.PORTAL && storageSign.GetDamage() == 0 && config.getBoolean("manual-import")) {
					if (player.isSneaking()) {
						storageSign.AddAmount(itemMainHand.getAmount());
						player.getInventory().clear(player.getInventory().getHeldItemSlot());
					} else for (int i=0; i<player.getInventory().getSize(); i++) {
						ItemStack item = player.getInventory().getItem(i);
						if (storageSign.IsSimilar(item)) {
							storageSign.AddAmount(item.getAmount());
							player.getInventory().clear(i);
						}
					}
				}//中身分割機能
				else if (itemSign.IsEmpty() && storageSign.GetAmount() > itemMainHand.getAmount() && config.getBoolean("manual-export")) {
					itemSign.SetMaterial(storageSign.GetMaterial());
					itemSign.SetDamage(storageSign.GetDamage());
					itemSign.SetEnchant(storageSign.GetEnchant());
					itemSign.SetPotion(storageSign.GetPotion());

					int limit = config.getInt("divide-limit");

					if (limit > 0 && storageSign.GetAmount() > limit * (itemSign.GetStackSize() + 1)) itemSign.SetAmount(limit);
					else itemSign.SetAmount(storageSign.GetAmount() / (itemSign.GetStackSize() + 1));
					player.getInventory().setItemInMainHand(itemSign.GetStorageSign());
					storageSign.SetAmount(storageSign.GetAmount() - (itemSign.GetStackSize() * itemSign.GetAmount()));//余りは看板に引き受けてもらう
				}
				for (int i=0; i<4; i++) sign.setLine(i, storageSign.GetSignText(i));
				sign.update();
				return;
			}

            //ここから搬入
            if (storageSign.IsSimilar(itemMainHand)) {
                if (!config.getBoolean("manual-import")) return;
                if (player.isSneaking()) {
                    storageSign.AddAmount(itemMainHand.getAmount());
                    player.getInventory().clear(player.getInventory().getHeldItemSlot());
                } else for (int i=0; i<player.getInventory().getSize(); i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    if (storageSign.IsSimilar(item)) {
                        storageSign.AddAmount(item.getAmount());
                        player.getInventory().clear(i);
                    }
                }

                player.updateInventory();
            } else if (config.getBoolean("manual-export"))/*放出*/ {
                if (storageSign.IsEmpty()) return;
                ItemStack item = storageSign.GetContents();

                int max = item.getMaxStackSize();

                if (player.isSneaking()) storageSign.AddAmount(-1);
                else if (storageSign.GetAmount() > max) {
                    item.setAmount(max);
                    storageSign.AddAmount(-max);
                } else {
                    item.setAmount(storageSign.GetAmount());
                    storageSign.SetAmount(0);
                }

                Location loc = player.getLocation();
                loc.setY(loc.getY() + 0.5);
                player.getWorld().dropItem(loc, item);
            }

            for (int i=0; i<4; i++) sign.setLine(i, storageSign.GetSignText(i));
            sign.update();
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.isCancelled())return;
        Sign sign = (Sign) event.getBlock().getState();

        if (sign.getLine(0).matches("StorageSign"))/*変更拒否*/ {
            event.setLine(0, sign.getLine(0));
            event.setLine(1, sign.getLine(1));
            event.setLine(2, sign.getLine(2));
            event.setLine(3, sign.getLine(3));
            sign.update();
        } else if (event.getLine(0).equalsIgnoreCase("storagesign"))/*書き込んで生成禁止*/ {
            if (event.getPlayer().hasPermission("storagesign.create")) {
                event.setLine(0, "StorageSign");
                sign.update();
            } else {
                event.getPlayer().sendMessage(ChatColor.RED + config.getString("no-permisson"));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Block block = event.getBlock();
        Map<Location, StorageSign> breakSignMap = new HashMap<>();
        if (isStorageSign(block)) breakSignMap.put(block.getLocation(), new StorageSign((Sign)block.getState()));

        for (int i=0; i<5; i++) {
            int[] x = {0, 0, 0,-1, 1};
            int[] y = {1, 0, 0, 0, 0};
            int[] z = {0,-1, 1, 0, 0};
            block = event.getBlock().getRelative(x[i], y[i], z[i]);
            if (i==0 && block.getType() == Material.SIGN_POST && isStorageSign(block)) breakSignMap.put(block.getLocation(), new StorageSign((Sign)block.getState()));
            else if(block.getType() == Material.WALL_SIGN && block.getData() == i+1 && isStorageSign(block)) breakSignMap.put(block.getLocation(), new StorageSign((Sign)block.getState()));
        }
        if (breakSignMap.isEmpty()) return;
        if (!event.getPlayer().hasPermission("storagesign.break")) {
            event.getPlayer().sendMessage(ChatColor.RED + config.getString("no-permisson"));
            event.setCancelled(true);
            return;
        }

        for (Location loc : breakSignMap.keySet()) {
            StorageSign sign = breakSignMap.get(loc);
            Location loc2 = loc;
            loc2.add(0.5, 0.5, 0.5);//中心にドロップさせる
            loc.getWorld().dropItem(loc2, sign.GetStorageSign());
            loc.getBlock().setType(Material.AIR);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled() || !isStorageSign(event.getItemInHand())) return;
        if (!event.getPlayer().hasPermission("storagesign.place")) {
            event.getPlayer().sendMessage(ChatColor.RED + config.getString("no-permisson"));
            event.setCancelled(true);
            return;
        }
        StorageSign storageSign = new StorageSign(event.getItemInHand());
        Sign sign = (Sign)event.getBlock().getState();
        for (int i=0; i<4; i++) sign.setLine(i, storageSign.GetSignText(i));
        sign.update();
        event.getPlayer().closeInventory();
    }

    @EventHandler
    public void onItemMove(InventoryMoveItemEvent event) {
        if (event.isCancelled()) return;
        BlockState[] blockInventory =new BlockState[2];
        Boolean flag = false;
        Sign sign = null;
        StorageSign storageSign = null;

        if (config.getBoolean("auto-import")) {
            if (event.getDestination().getHolder() instanceof Minecart);//何もしない
            else if (event.getDestination().getHolder() instanceof DoubleChest) {
                DoubleChest lc = (DoubleChest)event.getDestination().getHolder();
                blockInventory[0] = (BlockState) lc.getLeftSide();
                blockInventory[1] = (BlockState) lc.getRightSide();
            } else {
                blockInventory[0] = (BlockState) event.getDestination().getHolder();
            }

            importLoop:
                for (int j=0; j<2; j++) {
                    if (blockInventory[j] == null) break;
                    for (int i=0; i<5; i++) {
                        int[] x = {0, 0, 0,-1, 1};
                        int[] y = {1, 0, 0, 0, 0};
                        int[] z = {0,-1, 1, 0, 0};
                        Block block = blockInventory[j].getBlock().getRelative(x[i], y[i], z[i]);
                        if (i==0 && block.getType() == Material.SIGN_POST && isStorageSign(block)) {
                            sign = (Sign) block.getState();
                            storageSign = new StorageSign(sign);
                            if (storageSign.IsSimilar(event.getItem())) {
                                flag = true;
                                break importLoop;
                            }
                        } else if (i != 0 && block.getType() == Material.WALL_SIGN && block.getData() == i+1 && isStorageSign(block)) {
                            sign = (Sign) block.getState();
                            storageSign = new StorageSign(sign);
                            if (storageSign.IsSimilar(event.getItem())) {
                                flag = true;
                                break importLoop;
                            }
                        }
                    }
                }
            //搬入先が見つかった(搬入するとは言ってない)
            if (flag) importSign(sign, storageSign, event.getItem(), event.getDestination());
        }

        //搬出用にリセット
        if (config.getBoolean("auto-export")) {
            blockInventory[0] = null;
            blockInventory[1] = null;
            flag = false;
            if (event.getSource().getHolder() instanceof Minecart);
            else if (event.getSource().getHolder() instanceof DoubleChest) {
                DoubleChest lc = (DoubleChest)event.getSource().getHolder();
                blockInventory[0] = (BlockState) lc.getLeftSide();
                blockInventory[1] = (BlockState) lc.getRightSide();
            } else {
                blockInventory[0] = (BlockState) event.getSource().getHolder();
            }

            exportLoop:
                for (int j=0; j<2; j++) {
                    if (blockInventory[j] == null) break;
                    for (int i=0; i<5; i++) {
                        int[] x = {0, 0, 0,-1, 1};
                        int[] y = {1, 0, 0, 0, 0};
                        int[] z = {0,-1, 1, 0, 0};
                        Block block = blockInventory[j].getBlock().getRelative(x[i], y[i], z[i]);
                        if (i==0 && block.getType() == Material.SIGN_POST && isStorageSign(block)) {
                            sign = (Sign) block.getState();
                            storageSign = new StorageSign(sign);
                            if (storageSign.IsSimilar(event.getItem())) {
                                flag = true;
                                break exportLoop;
                            }
                        } else if (i != 0 && block.getType() == Material.WALL_SIGN && block.getData() == i+1 && isStorageSign(block)) {
                            sign = (Sign) block.getState();
                            storageSign = new StorageSign(sign);
                            if (storageSign.IsSimilar(event.getItem())) {
                                flag = true;
                                break exportLoop;
                            }
                        }
                    }
                }
            if (flag) exportSign(sign, storageSign, event.getItem(), event.getSource(), event.getDestination());
        }
    }

    private void importSign(Sign sign, StorageSign storageSign, ItemStack item, Inventory inv) {
        //搬入　条件　1スタック以上アイテムが入っている
        if (inv.containsAtLeast(item, item.getMaxStackSize())) {
            inv.removeItem(item);
            storageSign.AddAmount(item.getAmount());
        }
        for (int i=0; i<4; i++) sign.setLine(i, storageSign.GetSignText(i));
        sign.update();
    }

    private void exportSign(Sign sign, StorageSign storageSign, ItemStack item, Inventory inv, Inventory dest) {
        if (!inv.containsAtLeast(item, item.getMaxStackSize()) && storageSign.GetAmount() >= item.getAmount()) {
        	int stacks = 0;
        	int amount = 0;
        	ItemStack[] contents = dest.getContents();
        	for(int i=0; i< contents.length; i++){
        		if(item.isSimilar(contents[i])){
        			stacks++;
        			amount += contents[i].getAmount();
        		}
        	}
        	if(amount == stacks * item.getMaxStackSize() && dest.firstEmpty() == -1) return;
            inv.addItem(item);
            storageSign.AddAmount(-item.getAmount());
        }
        for (int i=0; i<4; i++) sign.setLine(i, storageSign.GetSignText(i));
        sign.update();
    }

    @EventHandler
    public void onPlayerCraft(CraftItemEvent event) {
        if (isStorageSign(event.getCurrentItem()) && !event.getWhoClicked().hasPermission("storagesign.craft")) {
            ((CommandSender) event.getWhoClicked()).sendMessage(ChatColor.RED + config.getString("no-permisson"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        if (event.isCancelled() || !config.getBoolean("auto-import")) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BlockState) {
            Sign sign = null;
            StorageSign storageSign = null;
            boolean flag = false;
            for (int i=0; i<5; i++) {
                int[] x = {0, 0, 0,-1, 1};
                int[] y = {1, 0, 0, 0, 0};
                int[] z = {0,-1, 1, 0, 0};
                Block block = ((BlockState)holder).getBlock().getRelative(x[i], y[i], z[i]);
                if (i==0 && block.getType() == Material.SIGN_POST && isStorageSign(block)) {
                    sign = (Sign) block.getState();
                    storageSign = new StorageSign(sign);
                    if (storageSign.IsSimilar(event.getItem().getItemStack())) {
                        flag = true;
                        break;
                    }
                } else if (i != 0 && block.getType() == Material.WALL_SIGN && block.getData() == i+1 && isStorageSign(block)) {
                    sign = (Sign) block.getState();
                    storageSign = new StorageSign(sign);
                    if (storageSign.IsSimilar(event.getItem().getItemStack())) {
                        flag = true;
                        break;
                    }
                }
            }
            if (flag) importSign(sign, storageSign, event.getItem().getItemStack(), event.getInventory());
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (event.isCancelled() || !config.getBoolean("autocollect")) return;
        Player player = event.getPlayer();
        PlayerInventory playerInv = player.getInventory();
        ItemStack item = event.getItem().getItemStack();
        StorageSign storagesign = null;
        //ここでは、エラーを出さずに無視する
        if(!player.hasPermission("storagesign.autocollect")) return;
        if(isStorageSign(playerInv.getItemInMainHand())){
        	storagesign = new StorageSign(playerInv.getItemInMainHand());
        	if(storagesign.GetContents() != null){

        		if (storagesign.IsSimilar(item) && playerInv.containsAtLeast(item, item.getMaxStackSize()) && storagesign.GetStackSize() == 1) {
        			storagesign.AddAmount(item.getAmount());

        			playerInv.removeItem(item);//1.9,10ではバグる？
        			playerInv.setItemInMainHand(storagesign.GetStorageSign());
        			player.updateInventory();
        			//event.getItem().remove();
        			//player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.5f);
        			//event.setCancelled(true);
        			return;
        		}
        	}
        }if(isStorageSign(playerInv.getItemInOffHand())){//メインハンドで回収されなかった時
        	storagesign = new StorageSign(playerInv.getItemInOffHand());
        	if(storagesign.GetContents() != null){

        		if (storagesign.IsSimilar(item) && playerInv.containsAtLeast(item, item.getMaxStackSize()) && storagesign.GetStackSize() == 1) {
        			storagesign.AddAmount(item.getAmount());
        			playerInv.removeItem(item);
        			playerInv.setItemInOffHand(storagesign.GetStorageSign());
        			player.updateInventory();
        			//event.getItem().remove();
        			//player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.5f);
        			//event.setCancelled(true);
        			return;
        		}
        	}
        }
    }
}
