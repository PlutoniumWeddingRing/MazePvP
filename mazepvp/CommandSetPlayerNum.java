package mazepvp;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandSetPlayerNum implements CommandExecutor {
	 
	private MazePvP main;
 
	public CommandSetPlayerNum(MazePvP main) {
		this.main = main;
	}
 
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 3) {
        	return false;
        }
		World world;
		if (sender instanceof Player) world = ((Player)sender).getWorld();
		else world = main.getServer().getWorlds().get(0);
        String mazeName = args[0];
        Maze maze = main.findMaze(mazeName, world);
        if (maze == null) {
        	sender.sendMessage("There's no maze with that name");
			return true;
        }
        int minNum, maxNum;
        try {
        	minNum = Integer.parseInt(args[1]);
        	maxNum = Integer.parseInt(args[2]);
    	} catch(NumberFormatException e) {
    		sender.sendMessage("minplayers and maxplayers should be numbers");
			return true;
		}
        if (minNum > maxNum) {
        	sender.sendMessage("The minimum number cannot be bigger than the maximum");
			return true;
        }
        if (minNum < 1 || maxNum < 1) {
        	sender.sendMessage("At least one player must be allowed");
			return true;
        }
        maze.configProps.minPlayers = minNum;
        maze.configProps.maxPlayers = maxNum;
        maze.updateSigns();
        sender.sendMessage("Player numbers for \""+maze.name+"\" changed");
    	return true;
	}
}