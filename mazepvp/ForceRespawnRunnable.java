package mazepvp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ForceRespawnRunnable extends BukkitRunnable
{
	public String playername;
	
	public ForceRespawnRunnable(String playername) {
		this.playername = playername;
	}

	public void run() {
		Player player = Bukkit.getPlayer(playername);
		if (player == null || !player.isDead()) return; 
		try {
            Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object packet = Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".Packet205ClientCommand").newInstance();
            packet.getClass().getField("a").set(packet, 1);
            Object con = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
            con.getClass().getMethod("a", packet.getClass()).invoke(con, packet);
        } catch (Throwable e) {
            e.printStackTrace();
        }
	}
}
