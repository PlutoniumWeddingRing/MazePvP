package mazepvp;

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
		if (propName.equals("playerLives")) printStr = Integer.toString(configProps.playerMaxDeaths);
		else if (propName.equals("playerNum.min")) printStr = Integer.toString(configProps.minPlayers);
		else if (propName.equals("playerNum.max")) printStr = Integer.toString(configProps.maxPlayers);
		else if (propName.equals("boss.attack")) printStr = Integer.toString(configProps.bosses.get(0).strength);
		else if (propName.equals("boss.hp")) printStr = Integer.toString(configProps.bosses.get(0).maxHp);
		else if (propName.equals("probabilities.groundReappear")) printStr = Double.toString(configProps.groundReappearProb);
     	else if (propName.equals("probabilities.chestAppear")) printStr = Double.toString(configProps.chestAppearProb);
        else if (propName.equals("probabilities.enderChestAppear")) printStr = Double.toString(configProps.enderChestAppearProb);
        else if (propName.equals("probabilities.mobAppear")) printStr = Double.toString(configProps.spawnMobProb);
        else if (propName.equals("boss.name")) printStr = configProps.bosses.get(0).name;
        else if (MazePvP.propHasItemValue(propName)) {
        	String itemPropName = MazePvP.getItemValue(propName);
    		boolean needWeigh = CommandAddItem.isWeighedProp(itemPropName);
    		ItemStack[] items = null;
    		double[] itemWeighs = null;
    		if (itemPropName.equals("startItems")) {
    			items = configProps.startItems;
    		} else if (itemPropName.equals("chestItems")) {
    			items = configProps.chestItems;
    			itemWeighs = configProps.chestWeighs;
    		} else if (itemPropName.equals("boss.drops")) {
    			items = configProps.bosses.get(0).dropItems;
    			itemWeighs = configProps.bosses.get(0).dropWeighs;
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
        	}
        	for (int i = 0; i < items.length; i++) {
				sender.sendMessage(itemPropName+".item"+(i+1)+".id is "+items[i].getTypeId());
				sender.sendMessage(itemPropName+".item"+(i+1)+".amount is "+items[i].getAmount());
				if (needWeigh) sender.sendMessage(itemPropName+".item"+(i+1)+".weigh is "+itemWeighs[i]);
        	}
			return true;
        } else {
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
        sender.sendMessage(propName+" is "+printStr);
    	return true;
	}
}