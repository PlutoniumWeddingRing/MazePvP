package mazepvp;

import java.io.File;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandRemoveItem implements CommandExecutor {

	private MazePvP main;
 
	public CommandRemoveItem(MazePvP main) {
		this.main = main;
	}
 
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1 || args.length > 4) {
        	return false;
        }
		Maze maze = null;
		MazeConfig configProps = null;
		String propName = null;
		String itemIdStr = null;
		String itemAmountStr = null;
		int removeIndex = -1;
		if (args.length == 2 || args.length == 4) {
			World world;
			if (sender instanceof Player) world = ((Player)sender).getWorld();
			else world = main.getServer().getWorlds().get(0);
	        String mazeName = args[0];
	        maze = main.findMaze(mazeName, world);
	        if (maze == null) {
	        	sender.sendMessage("There's no maze with that name");
				return true;
	        }
        	propName = args[1];
        	if (!CommandAddItem.validItemProp(propName)) {
        		try {
        			if (args.length != 2) throw new Exception();
        			if (!propName.contains(".item")) throw new Exception();
        			String[] parts = propName.split("\\.item");
        			if (parts.length != 2 || !CommandAddItem.validItemProp(parts[0])) throw new Exception();
        			propName = parts[0];
        			removeIndex = Integer.parseInt(parts[1])-1;
        		} catch (Exception e) {
	        		CommandAddItem.sendInvalidItemMsg(sender, propName, true);
					return true;
        		}
        	}
	        if (args.length == 4) {
	        	itemIdStr = args[2];
	        	itemAmountStr = args[3];
	        }
	        configProps = maze.configProps;
		} else {
			propName = args[0];
        	if (!CommandAddItem.validItemProp(propName)) {
        		try {
        			if (args.length != 1) throw new Exception();
        			if (!propName.contains(".item")) throw new Exception();
        			String[] parts = propName.split("\\.item");
        			if (parts.length != 2 || !CommandAddItem.validItemProp(parts[0])) throw new Exception();
        			propName = parts[0];
        			removeIndex = Integer.parseInt(parts[1])-1;
        		} catch (Exception e) {
	        		CommandAddItem.sendInvalidItemMsg(sender, propName, true);
					return true;
        		}
        	}
	        if (args.length == 3) {
	        	itemIdStr = args[1];
	        	itemAmountStr = args[2];
	        }
			configProps = MazePvP.theMazePvP.rootConfig;
		}
		int itemId = 0, itemAmount = 0;
		if (removeIndex < 0) {
			if (itemIdStr != null) {
				try {
					itemId = Integer.parseInt(itemIdStr);
					itemAmount = Integer.parseInt(itemAmountStr);
				} catch (NumberFormatException e) {
	        		sender.sendMessage("The item id and amount have to be integers");
	        		return true;
				}
			} else {
				if (sender instanceof Player) {
					ItemStack item = ((Player)sender).getInventory().getItemInHand();
					if (item == null || item.getTypeId() == 0) {
						sender.sendMessage("You don't have any item in hand");
		        		return true;
					}
					itemId = item.getTypeId();
					itemAmount = item.getAmount();
				} else {
					sender.sendMessage("You have to be a player to use the command without specifying item id");
	        		return true;
				}
			}
		}
		boolean needWeigh = CommandAddItem.isWeighedProp(propName);
		ItemStack[] items = null;
		double[] itemWeighs = null;
		int bNum = -1;
		if (propName.equals("startItems")) {
			items = configProps.startItems;
		} else if (propName.equals("chestItems")) {
			items = configProps.chestItems;
			itemWeighs = configProps.chestWeighs;
		} else if (propName.matches("boss[0-9]+\\.drops")) {
			String numStr = propName.replaceAll("^boss", "").replaceAll("\\.drops$", "");
			try {
				bNum = Integer.parseInt(numStr);
			} catch (NumberFormatException e) {
				CommandAddItem.sendInvalidItemMsg(sender, propName, false);
        		return true;
			}
			if (configProps.bosses.size() < bNum) {
				if (configProps.bosses.size() == 1) sender.sendMessage("There's only 1 boss");
				else sender.sendMessage("There are only "+configProps.bosses.size()+" bosses");
				return true;
			}
			bNum--;
			items = configProps.bosses.get(bNum).dropItems;
			itemWeighs = configProps.bosses.get(bNum).dropWeighs;
		}
		if (removeIndex < 0) {
			for (int i = 0; i < items.length; i++) {
				if (items[i].getTypeId() == itemId && items[i].getAmount() == itemAmount) {
					removeIndex = i;
					break;
				}
				if (i+1 == items.length) {
					sender.sendMessage("There's no item with id "+itemId+" and amount "+itemAmount+" in "+propName);
	        		return true;
				}
			}
		}
		if (removeIndex < 0 || removeIndex >= items.length) {
			sender.sendMessage("There's no property item"+(removeIndex+1)+" in "+propName);
    		return true;
		}
		itemId = items[removeIndex].getTypeId();
		itemAmount = items[removeIndex].getAmount();
		ItemStack[] newItems = new ItemStack[items.length-1];
		double[] newItemWeighs = null;
		if (needWeigh) newItemWeighs = new double[itemWeighs.length-1];
		for (int i = 0; i < items.length; i++) {
			if (i < removeIndex) {
				newItems[i] = items[i];
				if (needWeigh) newItemWeighs[i] = itemWeighs[i];
			} else if (i > removeIndex) {
				newItems[i-1] = items[i];
				if (needWeigh) newItemWeighs[i-1] = itemWeighs[i];
			}
		}
		if (propName.equals("startItems")) {
			configProps.startItems = newItems;
		} else if (propName.equals("chestItems")) {
			configProps.chestItems = newItems;
			configProps.chestWeighs = newItemWeighs;
		} else if (propName.matches("boss[0-9]+\\.drops")) {
			configProps.bosses.get(bNum).dropItems = newItems;
			configProps.bosses.get(bNum).dropWeighs = newItemWeighs;
		}
        if (maze == null) {
        	FileConfiguration config = new YamlConfiguration();
        	MazePvP.writeConfigToYml(configProps, config);
        	try {
        	config.save(new File(MazePvP.theMazePvP.getDataFolder(), "config.yml"));
        	} catch (Exception e) {
        		 sender.sendMessage("Couldn't overwrite configuration: "+e.getMessage());
        	    return true;
        	}
        }
        sender.sendMessage(propName+".item"+(removeIndex+1)+" (id: "+itemId+", amount: "+itemAmount+") removed");
    	return true;
	}
}