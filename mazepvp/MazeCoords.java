package mazepvp;

public class MazeCoords
{
	public final int x, z, y;
	public int type; //1: wall, 10: falltrap, 20: chest
	
	public MazeCoords(int x, int z, int y, int type) {
		this.x = x;
		this.z = z;
		this.y = y;
		this.type = type;
	}
	
	public MazeCoords(int x, int z, int y) {
		this.x = x;
		this.z = z;
		this.y = y;
		type = 0;
	}
}
