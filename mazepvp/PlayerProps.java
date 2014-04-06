package mazepvp;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class PlayerProps
{
	public Location prevLocation;
	public int deathCount = 0;
	public ItemStack[] savedInventory;
	public ItemStack[] savedArmor;
	public ItemStack[] savedEnderChest;
	public boolean spectating;
	
	public PlayerProps(Location prevLocation, ItemStack[] savedInventory, ItemStack[] savedArmor, ItemStack[] savedEnderChest, boolean spectating) {
		this.prevLocation = prevLocation;
		this.savedInventory = savedInventory;
		this.savedArmor = savedArmor;
		this.savedEnderChest = savedEnderChest;
		this.spectating = spectating;
	}
}
