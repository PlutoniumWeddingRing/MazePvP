package mazepvp;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CommandDeleteMaze implements CommandExecutor {
	 
	private MazePvP main;
 
	public CommandDeleteMaze(MazePvP main) {
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
        if (!maze.canBeEntered && !maze.playerInsideMaze.isEmpty()) {
	        maze.stopFight(false);
        }
        File mazeFile = new File(world.getWorldFolder(), mazeName+".maze");
        mazeFile.delete();
        mazeFile = new File(world.getWorldFolder(), mazeName+".yml");
        mazeFile.delete();
        main.mazes.remove(maze);
        Collection<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class);
        Iterator<LivingEntity> it = entities.iterator();
        while (it.hasNext()) {
        	LivingEntity en = it.next();
        	if (maze.isInsideMaze(en.getLocation())) en.damage(en.getHealth()+10);
        }
		sender.sendMessage("Deleting maze...");
    	for (int xx = 0; xx <= maze.mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH); xx++) {
    		for (int zz = 0; zz <= maze.mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH); zz++) {
    			for (int yy = -Maze.MAZE_PASSAGE_DEPTH; yy <= Maze.MAZE_PASSAGE_HEIGHT+2; yy++) {
    				if (yy == -Maze.MAZE_PASSAGE_DEPTH || yy == -Maze.MAZE_PASSAGE_DEPTH+1 || yy == -Maze.MAZE_PASSAGE_DEPTH+2 || yy == 0 || yy == Maze.MAZE_PASSAGE_HEIGHT+2 || maze.blockToMazeCoord(xx)%2 == 0 || maze.blockToMazeCoord(zz)%2 == 0) {
    					world.getBlockAt(maze.mazeX+xx, maze.mazeY+yy, maze.mazeZ+zz).setType(Material.AIR);
    				}
    			}
    		}
    	}
		sender.sendMessage("Maze "+mazeName+" deleted");
    	return true;
	}
}