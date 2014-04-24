package mazepvp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.bukkit.Bukkit;
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

	public static PlayerProps readFromFile(BufferedReader reader) throws Exception {
		String str;
		String pars[];
		
		Location savedLoc = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
		if ((str = reader.readLine()) == null) throw new Exception("Malformed input");
		pars = str.split("\\s");
        if (pars.length != 3)  throw new Exception("Malformed input");
        savedLoc.setX(Double.parseDouble(pars[0]));
        savedLoc.setY(Double.parseDouble(pars[1]));
        savedLoc.setZ(Double.parseDouble(pars[2]));
        
        int len;
		if ((str = reader.readLine()) == null) throw new Exception("Malformed input");
		len = Integer.parseInt(str);
        ItemStack savedInventory[] = new ItemStack[len];
		for (int i = 0; i < len; i++) {
			if ((str = reader.readLine()) == null) throw new Exception("Malformed input");
			if (str.length() == 0) savedInventory[i] = null;
			else savedInventory[i] = MazePvP.fromBase64(str);
		}
		
		if ((str = reader.readLine()) == null) throw new Exception("Malformed input");
		len = Integer.parseInt(str);
        ItemStack savedArmor[] = new ItemStack[len];
		for (int i = 0; i < len; i++) {
			if ((str = reader.readLine()) == null) throw new Exception("Malformed input");
			if (str.length() == 0) savedArmor[i] = null;
			else savedArmor[i] = MazePvP.fromBase64(str);
		}
		
		if ((str = reader.readLine()) == null) throw new Exception("Malformed input");
		len = Integer.parseInt(str);
        ItemStack savedEnderChest[] = new ItemStack[len];
		for (int i = 0; i < len; i++) {
			if ((str = reader.readLine()) == null) throw new Exception("Malformed input");
			if (str.length() == 0) savedEnderChest[i] = null;
			else savedEnderChest[i] = MazePvP.fromBase64(str);
		}
		

		if ((str = reader.readLine()) == null) throw new Exception("Malformed input");
		pars = str.split("\\s");
        if (pars.length != 2)  throw new Exception("Malformed input");
		boolean spectating = !pars[0].equals("0");
		@SuppressWarnings("deprecation")
		GameMode savedGM = GameMode.getByValue(Integer.parseInt(pars[1]));
		return new PlayerProps(savedLoc, savedInventory, savedArmor, savedEnderChest, spectating, savedGM);
	}
}
