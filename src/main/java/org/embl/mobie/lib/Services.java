package org.embl.mobie.lib;

import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;

@Plugin(type = Service.class, headless = true)
public class Services extends AbstractService implements SciJavaService
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static CommandService commandService;

	@Parameter
	CommandService cmd;

	@Override
	public void initialize() {
		commandService = cmd;
	}

}

