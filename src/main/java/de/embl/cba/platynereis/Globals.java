package de.embl.cba.platynereis;

import jdk.nashorn.internal.runtime.ScriptObject;

import java.util.concurrent.atomic.AtomicBoolean;

public class Globals
{
	// TODO: replace this with a listener model
	public static AtomicBoolean showSegmentsIn3D = new AtomicBoolean( false );
	public static AtomicBoolean showVolumesIn3D = new AtomicBoolean( false );
}
