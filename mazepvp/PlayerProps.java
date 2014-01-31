package mazepvp;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class PlayerProps
{
	public Location prevLocation;
	public int deathCount = 0;
	public ItemStack[] savedInventory;
	public ItemStack[] savedArmor;
	
	public PlayerProps(Location prevLocation, ItemStack[] savedInventory, ItemStack[] savedArmor) {
		this.prevLocation = prevLocation;
		this.savedInventory = savedInventory;
		this.savedArmor = savedArmor;
	}
}
