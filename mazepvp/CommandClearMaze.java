package mazepvp;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandClearMaze implements CommandExecutor {
	 
	private MazePvP main;
 
	public CommandClearMaze(MazePvP main) {
		this.main = main;
	}
 
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1) {
        	return false;
        }
		World world;
		if (sender instanceof Player) world = ((Player)sender).getWorld();
		else {
        	sender.sendMessage("Only players can use this command");
			return true;
		}
        String mazeName = args[0];
        Maze maze = main.findMaze(mazeName, world);
        if (maze == null) {
        	sender.sendMessage("There's no maze with that name");
			return true;
        }
    	sender.sendMessage("Clearing maze...");
        maze.cleanUpMaze();
    	sender.sendMessage("Maze cleared");
    	return true;
	}
}