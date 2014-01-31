package mazepvp;

import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandCreateLeaveSign implements CommandExecutor {
	 
	private MazePvP main;
 
	public CommandCreateLeaveSign(MazePvP main) {
		this.main = main;
	}
 
	@SuppressWarnings("deprecation")
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
        if (!maze.hasWaitArea) {
        	sender.sendMessage("The maze doesn't have a waiting place set");
			return true;
        }
        Block block = ((Player)sender).getTargetBlock(null, 10);
        if (block.getType() != Material.WALL_SIGN && block.getType() != Material.SIGN_POST) {
        	sender.sendMessage("There's no sign where you are looking");
        	return true;
        }
        Location loc = block.getLocation().clone();
        int xOffs = 0, zOffs = 0;
        int signNum = (int)Math.ceil(MazePvP.theMazePvP.leaveSignText.size()/4.0);
        Material type = world.getBlockAt(new Location(world, loc.getX()+1, loc.getY(), loc.getZ())).getType();
        if (type == Material.WALL_SIGN || type == Material.SIGN_POST) xOffs = 1;
        else {
            type = world.getBlockAt(new Location(world, loc.getX()-1, loc.getY(), loc.getZ())).getType();
            if (type == Material.WALL_SIGN || type == Material.SIGN_POST) xOffs = -1;
            else {
            	type = world.getBlockAt(new Location(world, loc.getX(), loc.getY(), loc.getZ()+1)).getType();
                if (type == Material.WALL_SIGN || type == Material.SIGN_POST) zOffs = 1;
                else {
                    type = world.getBlockAt(new Location(world, loc.getX(), loc.getY(), loc.getZ()-1)).getType();
                    if (type == Material.WALL_SIGN || type == Material.SIGN_POST) zOffs = -1;	
                }
            }
        }
        if (xOffs == 0 && zOffs == 0 && signNum > 1) {
        	sender.sendMessage("There must be "+signNum+" signs next to each other");
        	return true;
        }
        for (int i = 0; i < signNum-1; i++) {
        	loc.setX(loc.getX()+xOffs);
        	loc.setZ(loc.getZ()+zOffs);
        	type = world.getBlockAt(loc).getType();
        	if (type != Material.WALL_SIGN && type != Material.SIGN_POST) {
        		sender.sendMessage("There must be "+signNum+" signs next to each other");
            	return true;
        	}
        }
        Location startLoc = block.getLocation().clone();
        maze.addSign((int)Math.round(startLoc.getX()), (int)Math.round(startLoc.getY()), (int)Math.round(startLoc.getZ()), (int)Math.round(loc.getX()), (int)Math.round(loc.getY()), (int)Math.round(loc.getZ()), maze.leaveSigns);
        loc = block.getLocation().clone();
        Iterator<String> it = MazePvP.theMazePvP.leaveSignText.iterator();
        for (int i = 0; i < MazePvP.theMazePvP.leaveSignText.size(); i++) {
        	String str = it.next();
        	block = world.getBlockAt(loc);
    		Sign sign = (Sign)block.getState();
        	sign.setLine(i%4, maze.parseText(str, null));
    		sign.update();
        	if ((i+1)%4 == 0) {
        		loc.setX(loc.getX()+xOffs);
            	loc.setZ(loc.getZ()+zOffs);
        	}
        }
    	sender.sendMessage("Leave sign created");
    	return true;
	}
}