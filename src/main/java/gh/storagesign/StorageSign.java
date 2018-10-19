package gh.storagesign;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.util.NumberConversions;

/**
 * ストレージサイン
 */
public class StorageSign
{
    // マテリアル
    protected Material material;
    // ダメージ値
    protected short damage;
    // エンチャント
    protected Enchantment enchant;
    // ポーションの種類
    protected PotionType potionType;
    // スタック数
    protected int storageAmount;
    // ストレージ容量
    protected int storageCapacity;
    // 空かどうか
    protected boolean isEmpty;

    /**
     * コンストラクター
     * @param item
     */
    public StorageSign(ItemStack item)
    {
        // アイテムスタック情報からメタデータ（いわゆる実体データにあたる）を取得し、getLore(設定されている情報を取得）しているようだ。
    	String[] itemName = item.getItemMeta().getLore().get(0).split(" ");

    	// アイテム名がEmptyと一致する場合は「なにも入っていない」とする。
    	if(itemName[0].matches("Empty"))
        {
            this.isEmpty = true;
        }

        // アイテム名がEmpty以外ならばアイテムは入っているということ。
    	else
        {
            // アイテム名を分割して配列化しておく
            String[] divName = itemName[0].split(":");

            // アイテムマテリアルを取得
            this.material = this.GetMaterial(divName[0]);

            // エンチャント本だったら
    		if(this.material == Material.ENCHANTED_BOOK)
    		{
    		    // ダメージ値の取得
                this.damage = NumberConversions.toShort(divName[2]);
                // エンチャントの取得
                this.enchant = Enchantment.getByName(divName[1]);

                // エンチャントがない場合は旧仕様で取得する(非推奨）
    			if(this.enchant == null)
    			{
                    this.enchant = Enchantment.getById(NumberConversions.toInt(divName[1]));
                }
    		}

    		// ポーション系だったら
    		else if
            (
             this.material == Material.POTION ||
             this.material == Material.SPLASH_POTION ||
             this.material == Material.LINGERING_POTION
            )
    		{
    		    // ポーションの取得
    			PotionInfo potionInfo = new PotionInfo( this.material, divName);
    			// マテリアルの取得
                this.material = potionInfo.getMaterial();
                // ダメージ値の取得
                this.damage = potionInfo.getDamage();
                // ポーションの取得
                this.potionType = potionInfo.getPotionType();
    		}

    		// エンチャ本とポーション以外のアイテム
            // TODO: 今回は察したからいいけど、わかり辛っ！？
    		else if(itemName[0].contains(":"))
    		{
                this.damage = NumberConversions.toShort(divName[1]);
            }
            this.storageAmount = NumberConversions.toInt(itemName[1]);
    	}

        this.storageCapacity = item.getAmount();
    }

    /**
     * コンストラクター
     * @param sign
     * TODO: 完全に別機能。。。同じコンストラクタでやんなよ。。。。これ書いたやつギアナ高地に置き去りにしたい.
     * TODO: とりあえず、やれることはやろう。
     */
    public StorageSign(Sign sign) {
        // 看板から情報を取得する
        String[] signLines = sign.getLine(1).trim().split(":");

        // マテリアルを取得
        this.material = this.GetMaterial(signLines[0]);

        // 中身が入っていないか判定する
        this.isEmpty = (this.material == null || this.material == Material.AIR) ? true : false;

        // 行数が２行なら
        if(signLines.length == 2)
        {
            // ダメージ値取得
            this.damage = NumberConversions.toShort(signLines[1]);
        }

        // 取得したマテリアルがエンチャ本だったら
        if(this.material == Material.ENCHANTED_BOOK)
        {
            // ダメージ値の更新
            this.damage = NumberConversions.toShort(signLines[2]);

            // エンチャントの取得
            this.enchant = Enchantment.getByName(signLines[1]);

            // 非推奨も一応、ね。
            if (this.enchant == null)
            {
                this.enchant = Enchantment.getById(NumberConversions.toInt(signLines[1]));
            }
        }

        // ポーション系統ならば
        else if(this.material == Material.POTION || this.material == Material.SPLASH_POTION || this.material == Material.LINGERING_POTION)
        {
            // ポーションを取得
			PotionInfo potionInfo = new PotionInfo( this.material, signLines);

			// マテリアル取得
            this.material = potionInfo.getMaterial();

            // ダメージ値取得
            this.damage = potionInfo.getDamage();

            // ポーションの種類を取得
            this.potionType = potionInfo.getPotionType();
        }

        this.storageAmount = NumberConversions.toInt(sign.getLine(2));

        // 空かどうか
        this.isEmpty = (storageAmount == 0) ? true : false;

        this.storageCapacity = 1;
    }

    /**
     * マテリアルを返す
     * @param str
     * @return nullなら空っぽって言う意味
     */
	protected Material GetMaterial(String str)
    {
    	//後ろのせいで判別不能なアイテムたち
        if (str.matches("EmptySign")) return Material.PORTAL;
        if (str.matches("HorseEgg")){
        	damage = 1;
        	return Material.PORTAL;
        }

        if (str.startsWith("REDSTONE_TORCH")) return Material.REDSTONE_TORCH_ON;
        if (str.startsWith("RS_COMPARATOR")) return Material.REDSTONE_COMPARATOR;
        if (str.startsWith("STAINGLASS_P")) return Material.STAINED_GLASS_PANE;
        if (str.startsWith("BROWN_MUSH_B")) return Material.HUGE_MUSHROOM_1;
        if (str.startsWith("RED_MUSH_BLO")) return Material.HUGE_MUSHROOM_2;
        if (str.startsWith("ENCHBOOK")) return Material.ENCHANTED_BOOK;
        if (str.startsWith("SPOTION")) return Material.SPLASH_POTION;
        if (str.startsWith("LPOTION")) return Material.LINGERING_POTION;

        Material mat = Material.matchMaterial(str);
        if (mat == null)
        {
            for (Material m : Material.values())
            {
                if(m.toString().startsWith(str))
                {
                    return m;
                }
            }
        }
        return mat;
    }

    /**
     * 看板用のショートネームを取得する
     * @return 15文字以内で生成されるエイリアス的なのを出力している。
     */
    protected String GetShortName()
    {
        // 空っぽなら何も表示させない
        if (material == null || material == Material.AIR) return "";

        else if (material == Material.PORTAL)
        {
        	if(damage == 0) return "EmptySign";
        	if(damage == 1) return "HorseEgg";
        }

        else if (material == Material.STAINED_GLASS_PANE) return damage == 0 ? "STAINGLASS_PANE" : "STAINGLASS_P:" + damage;
        else if (material == Material.REDSTONE_COMPARATOR) return "RS_COMPARATOR";
        else if (material == Material.REDSTONE_TORCH_ON) return "REDSTONE_TORCH";
        else if (material == Material.HUGE_MUSHROOM_1) return damage == 0 ? "BROWN_MUSH_BLOC" : "BROWN_MUSH_B:" + damage;
        else if (material == Material.HUGE_MUSHROOM_2) return damage == 0 ? "RED_MUSH_BLOCK" : "RED_MUSH_BLO:" + damage;
        else if (material == Material.ENCHANTED_BOOK) return "ENCHBOOK:" + enchant.getId() + ":" + damage;

        else if (material == Material.POTION || material == Material.SPLASH_POTION || material == Material.LINGERING_POTION)
        {
        	String prefix = "";
        	if(material == Material.SPLASH_POTION) prefix = "S";
        	else if(material == Material.LINGERING_POTION) prefix = "L";

        	return prefix + "POTION:" + PotionInfo.getShortType( potionType ) +":" + damage;
        }

        int limit = 15;
        if (damage != 0)
        {
            limit -= String.valueOf(damage).length() + 1;
        }
        if (material.toString().length() > limit)
        {
            if (damage != 0)
            {
                return material.toString().substring(0, limit) + ":" + damage;
            }

            else
            {
                return material.toString().substring(0, limit);
            }
        }
        else
        {
            if (damage != 0)
            {
                return material.toString() + ":" + damage;
            }
            else
            {
                return material.toString();
            }
        }
    }

    /**
     * 看板チェストの取得
     * @return
     */
    public ItemStack GetStorageSign()
    {
        //
        ItemStack item = new ItemStack(Material.SIGN, this.storageCapacity );
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("StorageSign");
        List<String> list = new ArrayList<>();
        //IDとMaterial名が混ざってたり、エンチャ本対応したり
        if (isEmpty)
        {
            list.add("Empty");
        }
        else if (this.material == Material.PORTAL)
        {
        	if(this.damage == 0) list.add("EmptySign " + this.storageAmount);
        	if(this.damage == 1) list.add("HorseEgg " + this.storageAmount);
        }
        else if (this.material == Material.ENCHANTED_BOOK)
        {
            list.add(this.material.toString() + ":" + this.enchant.getName() + ":" + damage + " " + storageAmount);
        }
        else if (this.material == Material.POTION || this.material == Material.SPLASH_POTION || this.material == Material.LINGERING_POTION)
        {
        	list.add(this.material.toString() + ":" + this.potionType.toString() + ":" + this.damage + " " + this.storageAmount);
        }
        else if (this.damage != 0)
        {
            list.add(this.material.toString() + ":" + this.damage + " " + this.storageAmount);
        }
        else
        {
            list.add(this.material.toString() + " " + this.storageAmount);
        }
        meta.setLore(list);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * なんにも入っていない看板チェストを作り出す
     * @return
     */
    public static ItemStack EmptySign()
    {
        ItemStack emptySign = new ItemStack(Material.SIGN);
        ItemMeta meta = emptySign.getItemMeta();
        List<String> list = new ArrayList<>();
        meta.setDisplayName("StorageSign");
        list.add("Empty");
        meta.setLore(list);
        emptySign.setItemMeta(meta);
        return emptySign;
    }

    /**
     * 馬の卵でなんかできるっぽいぞ
     * @return
     */
    private ItemStack EmptyHorseEgg()
    {
        ItemStack emptyHorseEgg = new ItemStack(Material.MONSTER_EGG);
        ItemMeta meta = emptyHorseEgg.getItemMeta();
        List<String> list = new ArrayList<>();
        meta.setDisplayName("HorseEgg");
        list.add("Empty");
        meta.setLore(list);
        emptyHorseEgg.setItemMeta(meta);
        return emptyHorseEgg;
	}

    /**
     * 看板チェストの看板に記載されている行数を指定し、その行の値を返す
     * @param i 行数
     * @return メッセージ
     */
    public String GetSignText(int i)
    {
        String[] sign = new String[4];
        sign[0] = "StorageSign";
        sign[1] = GetShortName();
        sign[2] = String.valueOf(storageAmount);
        sign[3] = String.valueOf(storageAmount / 3456) + "LC " + String.valueOf(storageAmount % 3456 / 64) + "s " + String.valueOf(storageAmount % 64);
        return sign[i];
    }

    public ItemStack GetContents()
    {
        if (material == null)
        {
            return null;
        }
        if (material == Material.PORTAL)
        {
        	if(damage == 0) return EmptySign();
        	if(damage == 1) return EmptyHorseEgg();
        }
        if (material == Material.ENCHANTED_BOOK)
        {
            ItemStack item = new ItemStack( material, 1);
            EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta)item.getItemMeta();
            enchantMeta.addStoredEnchant( enchant, damage, true);
            item.setItemMeta(enchantMeta);
            return item;
        }
        else if(material == Material.POTION || material == Material.SPLASH_POTION || material == Material.LINGERING_POTION)
        {
        	ItemStack item = new ItemStack( material, 1);
        	PotionMeta potionMeta = (PotionMeta)item.getItemMeta();
        	potionMeta.setBasePotionData(new PotionData( potionType, damage == 1, damage == 2));
            item.setItemMeta(potionMeta);
            return item;
        }
        else if(material == Material.FIREWORK)
        {
        	ItemStack item = new ItemStack( material, 1);
        	FireworkMeta fireworkMeta = (FireworkMeta)item.getItemMeta();
        	fireworkMeta.setPower(damage);
        	item.setItemMeta(fireworkMeta);
        	return item;
        }

        return new ItemStack( material, 1, damage);
    }

	//回収可能か判定、エンチャ本は本自身の合成回数を問わない
    public boolean IsSimilar(ItemStack item) {
        if(item == null)
        {
            return false;
        }
        if (material == Material.ENCHANTED_BOOK && item.getType() == Material.ENCHANTED_BOOK)
        {
            EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta)item.getItemMeta();
            if(enchantMeta.getStoredEnchants().size() == 1)
            {
                Enchantment itemEnch = enchantMeta.getStoredEnchants().keySet().toArray(new Enchantment[0])[0];
                return itemEnch == enchant && enchantMeta.getStoredEnchantLevel( itemEnch ) == damage;
            }
            return false;
        }
        return GetContents().isSimilar(item);
    }

    public Material GetMaterial()
    {
        return material;
    }
    public void SetMaterial(Material mat)
    {
        this.material = mat;
    }

    public short GetDamage()
    {
        return damage;
    }

    public void SetDamage(short damage)
    {
        this.damage = damage;
    }

    public int GetAmount()
    {
        return storageAmount;
    }

    public void SetAmount(int amount)
    {
        this.storageAmount = amount;
        isEmpty = amount == 0;
    }

    public void AddAmount(int amount)
    {
        if(this.storageAmount < -amount) this.storageAmount = 0;
        else this.storageAmount += amount;
        if(this.storageAmount < 0) this.storageAmount = Integer.MAX_VALUE;
        isEmpty = this.storageAmount == 0;
    }

    public int GetStackSize()
    {
        return storageCapacity;
    }

    public boolean IsEmpty()
    {
        return isEmpty;
    }

	public void SetEnchant(Enchantment enchantment)
    {
		this.enchant = enchantment;
	}

	public Enchantment GetEnchant()
    {
		return enchant;
	}

	public void SetPotion(PotionType potionType)
    {
		this.potionType = potionType;
	}

	public PotionType GetPotion()
    {
		return potionType;
	}
}
