package mazepvp;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class BossConfig
{
	public String name = "boss";
	public int maxHp = 0;
	public int strength = 0;
	public int mazeFloor = 0;
	public double[] dropWeighs;
	public ItemStack[] dropItems;
	
	public BossConfig() {
		
	}
	
	public void loadDefaultValues() {
		name = "boss";
		maxHp = 0;
		strength = 0;
		mazeFloor = 0;
		dropWeighs = new double[]{2.0, 1.0};
		dropItems = new ItemStack[]{new ItemStack(Material.ENDER_PEARL, 4), new ItemStack(Material.ENDER_PEARL, 2)};
	}
	
	public BossConfig clone() {
		BossConfig copy = new BossConfig();
		copy.name = name;
		copy.maxHp = maxHp;
		copy.strength = strength;
		copy.mazeFloor = mazeFloor;
		copy.dropWeighs = dropWeighs.clone();
		copy.dropItems = MazePvP.cloneISArray(dropItems);
		return copy;
	}
}
