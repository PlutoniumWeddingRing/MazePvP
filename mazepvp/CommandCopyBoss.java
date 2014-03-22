package mazepvp;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class CommandCopyBoss implements CommandExecutor {
	 
	private MazePvP main;
 
	public CommandCopyBoss(MazePvP main) {
		this.main = main;
	}
 
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1 && args.length != 2) {
        	return false;
        }
		Maze maze = null;
		String bossNumStr;
		MazeConfig configProps;
		if (args.length == 2) {
			World world;
			if (sender instanceof Player) world = ((Player)sender).getWorld();
			else world = main.getServer().getWorlds().get(0);
	        String mazeName = args[0];
	        maze = main.findMaze(mazeName, world);
	        if (maze == null) {
	        	sender.sendMessage("There's no maze with that name");
				return true;
	        }
	        bossNumStr = args[1];
			configProps = maze.configProps;
		} else {
	        bossNumStr = args[0];
			configProps = MazePvP.theMazePvP.rootConfig;
		}
		int bNum = -1;
		try {
			bNum = Integer.parseInt(bossNumStr);
			if (bNum <= 0) throw new NumberFormatException();
		} catch (NumberFormatException e) {
			sender.sendMessage("Not a valid boss number");
	    	return true;
		}
		bNum--;
		if (bNum >= configProps.bosses.size()) {
			if (configProps.bosses.size() == 1) sender.sendMessage("There's only 1 boss");
			else sender.sendMessage("There are only "+configProps.bosses.size()+" bosses");
	    	return true;
		}
		BossConfig bossConfig = configProps.bosses.get(bNum).clone();
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
        sender.sendMessage(configProps.bosses.get(bNum).name+" copied to boss"+configProps.bosses.size());
    	return true;
	}
}