package mazepvp;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class CommandAddBoss implements CommandExecutor {
	 
	private MazePvP main;
 
	public CommandAddBoss(MazePvP main) {
		this.main = main;
	}
 
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 0 && args.length != 1) {
        	return false;
        }
		Maze maze = null;
		MazeConfig configProps;
		if (args.length == 1) {
			World world;
			if (sender instanceof Player) world = ((Player)sender).getWorld();
			else world = main.getServer().getWorlds().get(0);
	        String mazeName = args[0];
	        maze = main.findMaze(mazeName, world);
	        if (maze == null) {
	        	sender.sendMessage("There's no maze with that name");
				return true;
	        }
			configProps = maze.configProps;
		} else {
			configProps = MazePvP.theMazePvP.rootConfig;
		}
		BossConfig bossConfig = new BossConfig();
		bossConfig.loadDefaultValues();
		configProps.bosses.add(bossConfig);
		if (maze != null) {
			Boss boss = new Boss();
			boss.hp = bossConfig.maxHp;
			maze.bosses.add(boss);
			maze.updateBossHpStr(configProps.bosses.size()-1);
		}
        if (maze == null) {
        	FileConfiguration config = MazePvP.theMazePvP.getConfig();
        	MazePvP.writeConfigToYml(configProps, config);
        	MazePvP.theMazePvP.saveConfig();
        }
        sender.sendMessage("Added new boss to boss"+configProps.bosses.size());
    	return true;
	}
}