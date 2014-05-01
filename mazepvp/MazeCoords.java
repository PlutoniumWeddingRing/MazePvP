package mazepvp;

public class MazeCoords
{
	public final int x, z, y;
	public int type; //1: wall, 10: falltrap, 20: chest
	
	public MazeCoords(int x, int y, int z, int type) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.type = type;
	}
	
	public MazeCoords(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
		type = 0;
	}
}
