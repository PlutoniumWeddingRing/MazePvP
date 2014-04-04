package mazepvp;

import java.util.List;

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
 
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length >= 3 && MazePvP.propIsCommand(args[1])) {
			String[] newArgs = new String[3];
			for (int i = 0; i < 3; i++) newArgs[i] = args[i];
			for (int i = 3; i < args.length; i++) newArgs[2] += " "+args[i];
			args = newArgs;
		} else if (args.length >= 2 && MazePvP.propIsCommand(args[0])) {
			String[] newArgs = new String[2];
			for (int i = 0; i < 2; i++) newArgs[i] = args[i];
			for (int i = 2; i < args.length; i++) newArgs[1] += " "+args[i];
			args = newArgs;
		}
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
        		} else if (propName.matches("^boss.*") && !propName.matches("boss[0-9]+\\.drops.*")) {
        			String propEnd = "";
        			if (propName.matches(".*\\.attack$")) propEnd = "attack";
        			else if (propName.matches(".*\\.hp$")) propEnd = "hp";
        			else if (propName.matches(".*\\.name$")) propEnd = "name";
        			else {
        				sender.sendMessage("Property "+propName+" not found");
        				return true;
        			}
        			String numStr = propName.replaceAll("^boss", "").replaceAll("\\."+propEnd+"$", "");
        			int bNum = -1;
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
        			if (propEnd.equals("hp")) {
            			configProps.bosses.get(bNum).maxHp = value;
            			if (maze != null) maze.bosses.get(bNum).hp = value;
            			if (maze != null) maze.updateBossHpStr(bNum);
            		} else if (propEnd.equals("attack")) configProps.bosses.get(bNum).strength = value;
        		}
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
            if (propName.matches("boss[0-9]*\\.name")) {
    			String numStr = propName.replaceAll("^boss", "").replaceAll("\\.name$", "");
    			int bNum = -1;
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
        		configProps.bosses.get(bNum).name = value;
        	}
        } else if (MazePvP.propIsCommand(propName)) {
            List<String> commands = configProps.getCommandProp(propName);
        	if (propValue.startsWith("[")) {
        		commands.clear();
        		sender.sendMessage("Property "+propName+" cleared");
        	} else {
        		sender.sendMessage("Property "+propName+" set to:");
        		String[] lines = propValue.split(";");
        		commands.clear();
        		for (int i = 0; i < lines.length; i++) {
        			commands.add(lines[i]);
            		sender.sendMessage(lines[i]);
        		}
        	}
            if (maze == null) {
            	FileConfiguration config = MazePvP.theMazePvP.getConfig();
            	MazePvP.writeConfigToYml(configProps, config);
            	MazePvP.theMazePvP.saveConfig();
            }
        	return true;
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
    	        if (maze != null) {
	        		for (int xx = 0; xx < maze.mazeSize*2+1; xx++) {
	        			for (int zz = 0; zz < maze.mazeSize*2+1; zz++) {
	        				int sx, ex, sy, ey, sz, ez;
	        				int blockType;
	        				if (place == 0 && ((xx%2 == 0 && zz%2 != 0) || (xx%2 != 0 && zz%2 == 0)) && xx > 0 && zz > 0 && xx < maze.mazeSize*2 && zz < maze.mazeSize*2) {
	        					sy = -Maze.MAZE_PASSAGE_DEPTH+maze.mazeY;
	        					ey = Maze.MAZE_PASSAGE_HEIGHT+2+maze.mazeY;
	        					if (xx%2 == 0) {
	        						sz = maze.mazeToBlockCoord(zz)+maze.mazeZ;
	        						ez = sz+Maze.MAZE_PASSAGE_WIDTH-1;
	        						sx = ex = maze.mazeToBlockCoord(xx)+maze.mazeX;
	        						blockType = 1;
	        					} else {
	        						sx = maze.mazeToBlockCoord(xx)+maze.mazeX;
	        						ex = sx+Maze.MAZE_PASSAGE_WIDTH-1;
	        						sz = ez = maze.mazeToBlockCoord(zz)+maze.mazeZ;
	        						blockType = 2;
	        					}
	        				} else if (place == 1 && ((xx%2 == 0 && zz%2 != 0) || (xx%2 != 0 && zz%2 == 0)) && (xx == 0 || zz == 0 || xx == maze.mazeSize*2 || zz == maze.mazeSize*2)) {
	        					sy = -Maze.MAZE_PASSAGE_DEPTH+maze.mazeY;
	        					ey = Maze.MAZE_PASSAGE_HEIGHT+2+maze.mazeY;
	        					if (xx%2 == 0) {
	        						sz = maze.mazeToBlockCoord(zz)+maze.mazeZ;
	        						ez = sz+Maze.MAZE_PASSAGE_WIDTH-1;
	        						sx = ex = maze.mazeToBlockCoord(xx)+maze.mazeX;
	        						blockType = 1;
	        					} else {
	        						sx = maze.mazeToBlockCoord(xx)+maze.mazeX;
	        						ex = sx+Maze.MAZE_PASSAGE_WIDTH-1;
	        						sz = ez = maze.mazeToBlockCoord(zz)+maze.mazeZ;
	        						blockType = 2;
	        					}
	        				} else if (place == 2 && xx%2 == 0 && zz%2 == 0 && xx > 0 && zz > 0 && xx < maze.mazeSize*2 && zz < maze.mazeSize*2) {
	        					sy = -Maze.MAZE_PASSAGE_DEPTH+maze.mazeY;
	        					ey = Maze.MAZE_PASSAGE_HEIGHT+2+maze.mazeY;
        						sx = ex = maze.mazeToBlockCoord(xx)+maze.mazeX;
        						sz = ez = maze.mazeToBlockCoord(zz)+maze.mazeZ;
        						blockType = 1;
	        				} else if (place == 3 && xx%2 == 0 && zz%2 == 0 && (xx == 0 || zz == 0 || xx == maze.mazeSize*2 || zz == maze.mazeSize*2)) {
	        					sy = -Maze.MAZE_PASSAGE_DEPTH+maze.mazeY;
	        					ey = Maze.MAZE_PASSAGE_HEIGHT+2+maze.mazeY;
        						sx = ex = maze.mazeToBlockCoord(xx)+maze.mazeX;
        						sz = ez = maze.mazeToBlockCoord(zz)+maze.mazeZ;
        						blockType = 1;
	        				} else if (place == 4 && xx%2 != 0 && zz%2 != 0) {
	        					sy = maze.mazeY;
	        					ey = maze.mazeY;
        						sx = maze.mazeToBlockCoord(xx)+maze.mazeX;
        						ex = sx+Maze.MAZE_PASSAGE_WIDTH-1;
        						sz = maze.mazeToBlockCoord(zz)+maze.mazeZ;
        						ez = sz+Maze.MAZE_PASSAGE_WIDTH-1;
        						blockType = 0;
	        				} else if (place == 5 && xx%2 != 0 && zz%2 != 0) {
	        					sy = -Maze.MAZE_PASSAGE_DEPTH+maze.mazeY;
	        					ey = -Maze.MAZE_PASSAGE_DEPTH+maze.mazeY;
        						sx = maze.mazeToBlockCoord(xx)+maze.mazeX;
        						ex = sx+Maze.MAZE_PASSAGE_WIDTH-1;
        						sz = maze.mazeToBlockCoord(zz)+maze.mazeZ;
        						ez = sz+Maze.MAZE_PASSAGE_WIDTH-1;
        						blockType = 0;
	        				} else if (place == 6 && xx%2 != 0 && zz%2 != 0) {
	        					sy = -Maze.MAZE_PASSAGE_DEPTH+1+maze.mazeY;
	        					ey = -Maze.MAZE_PASSAGE_DEPTH+1+maze.mazeY;
        						sx = maze.mazeToBlockCoord(xx)+maze.mazeX;
        						ex = sx+Maze.MAZE_PASSAGE_WIDTH-1;
        						sz = maze.mazeToBlockCoord(zz)+maze.mazeZ;
        						ez = sz+Maze.MAZE_PASSAGE_WIDTH-1;
        						blockType = 0;
	        				} else if (place == 7 && xx%2 != 0 && zz%2 != 0) {
	        					sy = Maze.MAZE_PASSAGE_HEIGHT+2+maze.mazeY;
	        					ey = Maze.MAZE_PASSAGE_HEIGHT+2+maze.mazeY;
        						sx = maze.mazeToBlockCoord(xx)+maze.mazeX;
        						ex = sx+Maze.MAZE_PASSAGE_WIDTH-1;
        						sz = maze.mazeToBlockCoord(zz)+maze.mazeZ;
        						ez = sz+Maze.MAZE_PASSAGE_WIDTH-1;
        						blockType = 0;
	        				} else continue;
	        				for (int xxx = sx; xxx <= ex; xxx++) {
	        					for (int yyy = sy; yyy <= ey; yyy++) {
	        						if (blockType != 0 && yyy-maze.mazeY >= 1 && yyy-maze.mazeY <= Maze.MAZE_PASSAGE_HEIGHT+1 && maze.maze[xx][zz] != 1) continue;
	        						for (int zzz = sz; zzz <= ez; zzz++) {
	        							if (yyy-maze.mazeY == 0 && (maze.mazeWorld.getBlockAt(xxx, yyy, zzz) == null || maze.mazeWorld.getBlockAt(xxx, yyy, zzz).isEmpty())) continue;
	        							int bId = 0, bData = 0;
	        							if (blockType == 0) {
	        								bId = maze.configProps.blockTypes[place][zzz-sz][xxx-sx][0];
	        		    					bData = (byte)maze.configProps.blockTypes[place][zzz-sz][xxx-sx][1];
	        							} else if (blockType == 1) {
	        								bId = maze.configProps.blockTypes[place][maze.configProps.blockTypes[place].length-1-(yyy-sy)][zzz-sz][0];
	        		    					bData = (byte)maze.configProps.blockTypes[place][maze.configProps.blockTypes[place].length-1-(yyy-sy)][zzz-sz][1];
	        							} else if (blockType == 2) {
	        								bId = maze.configProps.blockTypes[place][maze.configProps.blockTypes[place].length-1-(yyy-sy)][xxx-sx][0];
	        		    					bData = (byte)maze.configProps.blockTypes[place][maze.configProps.blockTypes[place].length-1-(yyy-sy)][xxx-sx][1];
	        							}
	        							maze.mazeWorld.getBlockAt(xxx, yyy, zzz).setTypeId(bId);
	        	  	        			maze.mazeWorld.getBlockAt(xxx, yyy, zzz).setData((byte) bData);
	        						}
	        					}
	        				}
	        			}
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