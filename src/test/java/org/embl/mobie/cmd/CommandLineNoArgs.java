package org.embl.mobie.cmd;

class CommandLineNoArgs
{
	public static void main( String[] args ) throws Exception
	{
		final ProjectCmd cmd = new ProjectCmd();
		cmd.call();
	}
}