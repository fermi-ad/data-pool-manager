// $Id: NodeFlags.java,v 1.1 2023/10/04 19:40:29 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

public interface NodeFlags
{
	public static int OBSOLETE = 0;
	public static int OUT_OF_SERVICE = 1;
	public static int OPERATIONAL = 2;
	public static int EVENT_STRING_SUPPORT = 3;
	public static int HARDWARE_CLOCK = 4;
}
