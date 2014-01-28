package mazepvp;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandRemoveWaitingPlace implements CommandExecutor {
	 
	private MazePvP main;
 
	public CommandRemoveWaitingPlace(MazePvP main) {
		this.main = main;
	}
 
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1) {
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
        if (!maze.canBeEntered) {
        	maze.canBeEntered = true;
        	sender.sendMessage("Restoring entrances...");
        	maze.restoreEntrances();
    		sender.sendMessage("Waiting place for "+mazeName+" removed");
        } else sender.sendMessage(mazeName+" didn't have a waiting place");
    	return true;
	}
}