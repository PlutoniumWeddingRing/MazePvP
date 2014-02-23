package mazepvp;

import org.bukkit.inventory.ItemStack;


public class BossConfig
{
	public String name = "boss";
	public int maxHp = 0;
	public int strength = 0;
	public double[] dropWeighs;
	public ItemStack[] dropItems;
	
	public BossConfig() {
		
	}
	
	public BossConfig clone() {
		BossConfig copy = new BossConfig();
		copy.name = name;
		copy.maxHp = maxHp;
		copy.strength = strength;
		copy.dropWeighs = dropWeighs.clone();
		copy.dropItems = MazePvP.cloneISArray(dropItems);
		return copy;
	}
}
