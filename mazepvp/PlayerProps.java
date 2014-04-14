package mazepvp;

import org.bukkit.GameMode;
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
	public GameMode savedGM;
	
	public PlayerProps(Location prevLocation, ItemStack[] savedInventory, ItemStack[] savedArmor, ItemStack[] savedEnderChest, boolean spectating, GameMode savedGM) {
		this.prevLocation = prevLocation;
		this.savedInventory = savedInventory;
		this.savedArmor = savedArmor;
		this.savedEnderChest = savedEnderChest;
		this.spectating = spectating;
		this.savedGM = savedGM;
	}
}
