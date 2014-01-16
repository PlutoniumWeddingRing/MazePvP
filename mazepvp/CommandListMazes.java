package mazepvp;

import java.util.Iterator;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandListMazes implements CommandExecutor {

	private MazePvP main;
	
	public CommandListMazes(MazePvP main) {
		this.main = main;
	}
 
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Iterator<Maze> it = main.mazes.iterator();
		World world;
		if (sender instanceof Player) world = ((Player)sender).getWorld();
		else world = main.getServer().getWorlds().get(0);
		boolean found = false;
		while (it.hasNext()) {
			Maze maze = it.next();
			if (maze.mazeWorld == world) {
				if (!found) {
					sender.sendMessage("The following mazes exist:");
					found = true;
				}
				sender.sendMessage(maze.name+" at "+maze.mazeX+", "+maze.mazeY+", "+maze.mazeZ);
			}
		}
		if (!found) sender.sendMessage("There aren't any mazes");
		return true;
	}

}
