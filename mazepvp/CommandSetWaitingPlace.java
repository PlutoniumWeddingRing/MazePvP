package mazepvp;


import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandSetWaitingPlace implements CommandExecutor {
	 
	private MazePvP main;
 
	public CommandSetWaitingPlace(MazePvP main) {
		this.main = main;
	}
 
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1 && args.length != 4) {
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
        int posX, posY, posZ;
    	if (args.length == 4) {
	    	try {
	    		posX = Integer.parseInt(args[1]);
	    		posY = Integer.parseInt(args[2]);
	    		posZ = Integer.parseInt(args[3]);
			} catch(NumberFormatException e) {
				sender.sendMessage("coordinate is not a number");
				return true;
			}
    	} else {
    		Location playerLoc = ((Player)sender).getLocation();
    		posX = (int)Math.round(playerLoc.getX()-0.5);
    		posY = (int)Math.round(playerLoc.getY());
    		posZ = (int)Math.round(playerLoc.getZ()-0.5);
    	}
    	maze.waitX = posX;
    	maze.waitY = posY;
    	maze.waitZ = posZ;
        if (maze.canBeEntered) {
        	maze.canBeEntered = false;
        	sender.sendMessage("Removing entrances...");
        	maze.removeEntrances();
    		sender.sendMessage("Waiting place for "+mazeName+" set");
        } else sender.sendMessage("Waiting place for "+mazeName+" relocated");
    	return true;
	}
}