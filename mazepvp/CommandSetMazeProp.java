package mazepvp;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class CommandSetMazeProp implements CommandExecutor {
	 
	private MazePvP main;
 
	public CommandSetMazeProp(MazePvP main) {
		this.main = main;
	}
 
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 2 && args.length != 3) {
        	return false;
        }
		Maze maze = null;
		String propName, propValue;
		MazeConfig configProps;
		if (args.length == 3) {
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
	        propValue = args[2];
			configProps = maze.configProps;
		} else {
	        propName = args[0];
	        propValue = args[1];
			configProps = MazePvP.theMazePvP.rootConfig;
		}
        if (MazePvP.propHasIntValue(propName)) {
        	try {
        		int value = Integer.parseInt(propValue);
        		if (propName.equals("playerLives")) configProps.playerMaxDeaths = value;
        		else if (propName.equals("playerNum.min")) {
        			configProps.minPlayers = value;
        			if (maze != null) maze.updateSigns();
        		} else if (propName.equals("playerNum.max")) {
        			configProps.maxPlayers = value;
        			if (maze != null) maze.updateSigns();
        		} else if (propName.equals("boss.hp")) {
        			configProps.bossMaxHp = value;
        			if (maze != null) maze.mazeBossHp = value;
        			if (maze != null) maze.updateBossHpStr();
        		} else if (propName.equals("boss.attack")) configProps.bossStrength = value;
        	} catch (NumberFormatException e) {
            	sender.sendMessage("The value for "+propName+" must be an integer");
    			return true;
        	}
        } else if (MazePvP.propHasDoubleValue(propName)) {
        	try {
        		double value = Double.parseDouble(propValue);
        		if (propName.equals("probabilities.groundReappear")) configProps.groundReappearProb = value;
        		else if (propName.equals("probabilities.chestAppear")) configProps.chestAppearProb = value;
        		else if (propName.equals("probabilities.enderChestAppear")) configProps.enderChestAppearProb = value;
        		else if (propName.equals("probabilities.mobAppear")) configProps.spawnMobProb = value;
        	} catch (NumberFormatException e) {
        		sender.sendMessage("The value for "+propName+" must be a number");
    			return true;
        	}
        } else if (MazePvP.propHasStringValue(propName)) {
        	String value = propValue;
        	if (propName.equals("boss.name")) configProps.bossName = value;
        } else if (MazePvP.propHasItemValue(propName)) {
        	sender.sendMessage("You can't set that value directly");
        	sender.sendMessage("Use the command /mpadditem or /mpremoveitem instead");
			return true;
        } else if (propName.startsWith("blocks.")) {
    		try {
    			String[] parts = propName.split("\\.");
    			if (parts.length != 2) throw new Exception();
    			int place = MazeConfig.getBlockTypeIndex(parts[1]);
    			if (place < 0) throw new Exception();
    			
    			String idStr = "", dataStr = "";
    			int strPlace = 0;
    			boolean readingData = false;
    			propValue += " ";
    	        for (int i = 0; i < MazeConfig.blockTypeDimensions[place][1]; i++) {
    	        	for (int j = 0; j < MazeConfig.blockTypeDimensions[place][0];) {
    	        		if (strPlace >= propValue.length()) {
	        				configProps.blockTypes[place][i][j][0] = 1;
	        				configProps.blockTypes[place][i][j][1] = 0;
	        				j++;
    	        		} else if (propValue.charAt(strPlace) >= '0' && propValue.charAt(strPlace) <= '9') {
    	        			if (readingData) dataStr += propValue.charAt(strPlace);
    	        			else idStr += propValue.charAt(strPlace);
    	        		} else if (propValue.charAt(strPlace) == ':' && !readingData) readingData = true;
    	        		else {
    	        			if (idStr.length() > 0) {
    	        				int id, data;
    	        				if (dataStr.length() == 0) data = 0;
    	        				else data = Integer.parseInt(dataStr);
    	        				id = Integer.parseInt(idStr);
    	        				idStr = "";
    	        				dataStr = "";
    	        				configProps.blockTypes[place][i][j][0] = id;
    	        				configProps.blockTypes[place][i][j][1] = data;
    	        				readingData = false;
    	        				j++;
    	        			}
    	        		}
    	        		strPlace++;
    	        	}
    	        }
            	sender.sendMessage("Property "+propName+" set");
    		} catch (Exception e) {
            	sender.sendMessage("Property "+propName+" not found");
    			return true;
    		}
    	} else {
    		sender.sendMessage("Property "+propName+" not found");
			return true;
    	}
        if (maze == null) {
        	FileConfiguration config = MazePvP.theMazePvP.getConfig();
        	MazePvP.writeConfigToYml(configProps, config);
        	MazePvP.theMazePvP.saveConfig();
        }
        if (!propName.startsWith("blocks.")) sender.sendMessage("Property "+propName+" set to "+propValue);
    	return true;
	}
}