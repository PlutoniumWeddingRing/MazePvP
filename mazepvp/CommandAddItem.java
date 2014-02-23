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

public class CommandAddItem implements CommandExecutor {

	public static final String[] weighlessItemProps = {
    	"startItems"
    };
	public static final String[] weighedItemProps = {
    	"chestItems",
    	"boss.drops"
    };

	private MazePvP main;
 
	public CommandAddItem(MazePvP main) {
		this.main = main;
	}
 
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1 || args.length > 5) {
        	return false;
        }
		Maze maze = null;
		String propName = null;
		String itemIdStr = null;
		String itemAmountStr = null;
		String itemWeighStr = null;
		boolean mazeNotSpecified = false;
		MazeConfig configProps = null;
		if (args.length == 2 || args.length == 3 || args.length == 4 || args.length == 5) {
			World world;
			if (sender instanceof Player) world = ((Player)sender).getWorld();
			else world = main.getServer().getWorlds().get(0);
	        String mazeName = args[0];
	        maze = main.findMaze(mazeName, world);
	        if (maze == null) {
        		if (args.length == 2 || args.length == 3 || args.length == 4) {
        			mazeNotSpecified = true;
        		} else {
    	        	sender.sendMessage("There's no maze with that name");
    				return true;	
        		}
	        }
        	if (!mazeNotSpecified) {
	        	propName = args[1];
	        	if (!validItemProp(propName)) {
	    			sender.sendMessage(propName+" is not a valid item property");
	        		sender.sendMessage("Valid item properties are:");
	        		for (int i = 0; i < weighlessItemProps.length; i++) sender.sendMessage(weighlessItemProps[i]);
	        		for (int i = 0; i < weighedItemProps.length; i++) sender.sendMessage(weighedItemProps[i]);
					return true;
	        	}
		        if (args.length >= 4) {
		        	itemIdStr = args[2];
		        	itemAmountStr = args[3];
		        	if (args.length >= 5) itemWeighStr = args[4];
		        	else if (isWeighedProp(propName)) {
		        		sender.sendMessage("You have to specify the weigh of the item");
		        		return true;
		        	}
		        } else if (args.length == 3) itemWeighStr = args[2];
		        else if (isWeighedProp(propName)) {
		        	sender.sendMessage("You have to specify the weigh of the item");
	        		return true;
		        }
		        configProps = maze.configProps;
        	}
		}
		if (args.length == 1 || (args.length == 2 && mazeNotSpecified) || (args.length == 3 && mazeNotSpecified) || (args.length == 4 && mazeNotSpecified)) {
	        propName = args[0];
        	if (!validItemProp(propName)) {
	        	sender.sendMessage(propName+" is not a valid item property");
	        	sender.sendMessage("Valid item properties are:");
	        	for (int i = 0; i < weighlessItemProps.length; i++) sender.sendMessage(weighlessItemProps[i]);
	        	for (int i = 0; i < weighedItemProps.length; i++) sender.sendMessage(weighedItemProps[i]);
				return true;
        	}
	        if (args.length >= 3) {
	        	itemIdStr = args[1];
	        	itemAmountStr = args[2];
	        	if (args.length >= 4) itemWeighStr = args[3];
	        	else if (isWeighedProp(propName)) {
	        		sender.sendMessage("You have to specify the weigh of the item");
	        		return true;
	        	}
	        } else if (args.length == 2) itemWeighStr = args[1];
	        else if (isWeighedProp(propName)) {
	        	sender.sendMessage("You have to specify the weigh of the item");
        		return true;
	        }
			configProps = MazePvP.theMazePvP.rootConfig;
		}
		boolean needWeigh = isWeighedProp(propName);
		int itemId, itemAmount;
		double itemWeigh = 0.0;
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
		try {
			if (needWeigh) itemWeigh = Double.parseDouble(itemWeighStr);
		} catch (NumberFormatException e) {
    		sender.sendMessage("The item weigh has to be a number");
    		return true;
		}
		ItemStack[] items = null;
		double[] itemWeighs = null;
		if (propName.equals("startItems")) {
			items = configProps.startItems;
		} else if (propName.equals("chestItems")) {
			items = configProps.chestItems;
			itemWeighs = configProps.chestWeighs;
		} else if (propName.equals("boss.drops")) {
			items = configProps.bosses.get(0).dropItems;
			itemWeighs = configProps.bosses.get(0).dropWeighs;
		}
		ItemStack[] newItems = new ItemStack[items.length+1];
		double[] newItemWeighs = null;
		if (needWeigh) newItemWeighs = new double[itemWeighs.length+1];
		for (int i = 0; i < items.length; i++) {
			newItems[i] = items[i];
			if (needWeigh) newItemWeighs[i] = itemWeighs[i];
		}
		newItems[items.length] = new ItemStack(itemId, itemAmount);
		if (needWeigh) newItemWeighs[itemWeighs.length] = itemWeigh;
		if (propName.equals("startItems")) {
			configProps.startItems = newItems;
		} else if (propName.equals("chestItems")) {
			configProps.chestItems = newItems;
			configProps.chestWeighs = newItemWeighs;
		} else if (propName.equals("boss.drops")) {
			configProps.bosses.get(0).dropItems = newItems;
			configProps.bosses.get(0).dropWeighs = newItemWeighs;
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
        sender.sendMessage(propName+".item"+newItems.length+".id set to "+itemId);
        sender.sendMessage(propName+".item"+newItems.length+".amount set to "+itemAmount);
        if (needWeigh) sender.sendMessage(propName+".item"+newItems.length+".weigh set to "+itemWeigh);
    	return true;
	}

	public static boolean validItemProp(String propName) {
		for (int i = 0; i < weighlessItemProps.length; i++) {
			if (weighlessItemProps[i].equals(propName)) return true;
		}
		for (int i = 0; i < weighedItemProps.length; i++) {
			if (weighedItemProps[i].equals(propName)) return true;
		}
		return false;
	}

	public static boolean isWeighedProp(String propName) {
		for (int i = 0; i < weighedItemProps.length; i++) {
			if (weighedItemProps[i].equals(propName)) return true;
		}
		return false;
	}
}