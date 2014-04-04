package mazepvp;

import java.util.Iterator;
import java.util.List;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandGetMazeProp implements CommandExecutor {
	 
	private MazePvP main;
 
	public CommandGetMazeProp(MazePvP main) {
		this.main = main;
	}
 
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1 && args.length != 2) {
        	return false;
        }
		Maze maze = null;
		String propName;
		MazeConfig configProps;
		if (args.length == 2) {
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
			configProps = maze.configProps;
		} else {
	        propName = args[0];
			configProps = MazePvP.theMazePvP.rootConfig;
		}
		String printStr = "";
		int bNum = -1;
		if (propName.equals("playerLives")) printStr = Integer.toString(configProps.playerMaxDeaths);
		else if (propName.equals("playerNum.min")) printStr = Integer.toString(configProps.minPlayers);
		else if (propName.equals("playerNum.max")) printStr = Integer.toString(configProps.maxPlayers);
		else if (propName.matches("^boss.*")) {
			String propEnd = "";
			if (propName.matches(".*\\.attack$")) propEnd = "attack";
			else if (propName.matches(".*\\.hp$")) propEnd = "hp";
			else if (propName.matches(".*\\.name$")) propEnd = "name";
			else if (propName.matches(".*\\.drops.*")) propEnd = "";
			else {
				sender.sendMessage("Property "+propName+" not found");
				return true;
			}
			String modPropName = propName;
			if (propName.matches(".*\\.drops.*")) {;
				String[] splitProps = propName.split("drops");
				if (splitProps.length > 0) modPropName = splitProps[0];
				else propEnd = "drops";
			}
			String numStr = modPropName.replaceAll("^boss", "").replaceAll("\\."+propEnd+"$", "");
			try {
				bNum = Integer.parseInt(numStr);
			} catch (NumberFormatException e) {
				sender.sendMessage("Property "+propName+" not found");
        		return true;
			}
			if (configProps.bosses.size() < bNum) {
				if (configProps.bosses.size() == 1) sender.sendMessage("There's only 1 boss");
				else sender.sendMessage("There are only "+configProps.bosses.size()+" bosses");
				return true;
			}
			bNum--;
			if (propEnd.equals("attack")) printStr = Integer.toString(configProps.bosses.get(bNum).strength);
			else if (propEnd.equals("hp")) printStr = Integer.toString(configProps.bosses.get(bNum).maxHp);
			else if (propEnd.equals("name")) printStr = configProps.bosses.get(bNum).name;
		} else if (propName.equals("probabilities.groundReappear")) printStr = Double.toString(configProps.groundReappearProb);
     	else if (propName.equals("probabilities.chestAppear")) printStr = Double.toString(configProps.chestAppearProb);
        else if (propName.equals("probabilities.enderChestAppear")) printStr = Double.toString(configProps.enderChestAppearProb);
        else if (propName.equals("probabilities.mobAppear")) printStr = Double.toString(configProps.spawnMobProb);
        else if (MazePvP.propIsCommand(propName)) {
        	List<String> commands = configProps.getCommandProp(propName);
        	if (commands.isEmpty()) {
        		sender.sendMessage(propName+" is cleared");
        		return true;
        	}
        	printStr = "";
        	Iterator<String> it = commands.iterator();
        	while (it.hasNext()) {
        		printStr += it.next();
        		if (it.hasNext()) printStr += "\n";
        	}
        }
        else if (!MazePvP.propHasItemValue(propName)) {
        	if (propName.startsWith("blocks.")) {
        		try {
        			String[] parts = propName.split("\\.");
        			if (parts.length != 2) throw new Exception();
        			int place = MazeConfig.getBlockTypeIndex(parts[1]);
        			if (place < 0) throw new Exception();
        	        sender.sendMessage(propName+" is the following:");
        	        for (int i = 0; i < configProps.blockTypes[place].length; i++) {
        	        	String msg = "";
        	        	for (int j = 0; j < configProps.blockTypes[place][i].length; j++) {
        	        		msg += configProps.blockTypes[place][i][j][0];
        	        		if (configProps.blockTypes[place][i][j][1] != 0)
        	        			msg += ":"+configProps.blockTypes[place][i][j][1];
        	        		if (j+1 < configProps.blockTypes[place][i].length) msg += " ";
        	        	}
            	        sender.sendMessage(msg);
        	        }
        			return true;
        		} catch (Exception e) {}
        	}
        	sender.sendMessage("Property "+propName+" not found");
			return true;
        }
		if (MazePvP.propHasItemValue(propName)) {
        	String itemPropName = MazePvP.getItemValue(propName, bNum);
    		boolean needWeigh = CommandAddItem.isWeighedProp(itemPropName);
    		ItemStack[] items = null;
    		double[] itemWeighs = null;
    		if (itemPropName.equals("startItems")) {
    			items = configProps.startItems;
    		} else if (itemPropName.equals("chestItems")) {
    			items = configProps.chestItems;
    			itemWeighs = configProps.chestWeighs;
    		} else if (itemPropName.matches("boss[0-9]+\\.drops")) {
    			items = configProps.bosses.get(bNum).dropItems;
    			itemWeighs = configProps.bosses.get(bNum).dropWeighs;
    		}
        	if (propName.contains(".item")) {
        		String[] parts = propName.split("\\.item");
        		if (parts.length == 2) {
        			try {
        				int itemPlace = Integer.parseInt(parts[1])-1;
        				if (itemPlace >= 0 && itemPlace < items.length) {
        					sender.sendMessage(propName+".id is "+items[itemPlace].getTypeId());
        					sender.sendMessage(propName+".amount is "+items[itemPlace].getAmount());
        					if (needWeigh) sender.sendMessage(propName+".weigh is "+itemWeighs[itemPlace]);
            				return true;
        				} else {
        					sender.sendMessage(propName+" is not set");
            				return true;
        				}
        			} catch (Exception e) {}
        		}
        	} else {
	        	for (int i = 0; i < items.length; i++) {
					sender.sendMessage(itemPropName+".item"+(i+1)+".id is "+items[i].getTypeId());
					sender.sendMessage(itemPropName+".item"+(i+1)+".amount is "+items[i].getAmount());
					if (needWeigh) sender.sendMessage(itemPropName+".item"+(i+1)+".weigh is "+itemWeighs[i]);
	        	}
        	}
			return true;
		}
		if (printStr.contains("\n")) {
			sender.sendMessage(propName+" is:");
			String[] lines = printStr.split("\n");
			for (int i = 0; i < lines.length; i++) sender.sendMessage(lines[i]);
		} else sender.sendMessage(propName+" is "+printStr);
    	return true;
	}
}