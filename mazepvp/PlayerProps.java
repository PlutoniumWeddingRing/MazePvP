package mazepvp;

import java.io.IOException;
import java.io.PrintWriter;

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

	@SuppressWarnings("deprecation")
	public void writeToFile(PrintWriter writer) throws IOException {
		writer.printf("%f %f %f\n", new Object[]{prevLocation.getX(), prevLocation.getY(), prevLocation.getZ(), prevLocation.getPitch(), prevLocation.getYaw()});
		writer.printf("%d\n", new Object[]{savedInventory.length});
		for (int i = 0; i < savedInventory.length; i++) writer.printf("%s\n", new Object[]{(savedInventory[i]==null)?"":MazePvP.toBase64(savedInventory[i])});
		writer.printf("%d\n", new Object[]{savedArmor.length});
		for (int i = 0; i < savedArmor.length; i++) writer.printf("%s\n", new Object[]{(savedArmor[i]==null)?"":MazePvP.toBase64(savedArmor[i])});
		writer.printf("%d\n", new Object[]{savedEnderChest.length});
		for (int i = 0; i < savedEnderChest.length; i++) writer.printf("%s\n", new Object[]{(savedEnderChest[i]==null)?"":MazePvP.toBase64(savedEnderChest[i])});
		writer.printf("%d %d\n", new Object[]{spectating?1:0, savedGM.getValue()});
	}
}
