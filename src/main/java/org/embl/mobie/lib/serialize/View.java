/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.serialize;

import org.embl.mobie.lib.serialize.display.Display;
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.serialize.display.SpotDisplay;
import org.embl.mobie.lib.serialize.transformation.Transformation;
import org.embl.mobie.lib.transform.viewer.ViewerTransform;

import java.util.*;

public class View
{
	// Serialisation (do not change names of fields!)
	//
	@Deprecated // use uiSelectionGroups instead
	private String uiSelectionGroup; // https://github.com/mobie/mobie-viewer-fiji/issues/1223

	private String[] uiSelectionGroups; // https://github.com/mobie/mobie-viewer-fiji/issues/1223

	private List< Display< ? > > sourceDisplays;

	private List< Transformation > sourceTransforms;

	private ViewerTransform viewerTransform = null;

	private Boolean isExclusive;

	private String description; // TODO add to JSON spec?

	// Runtime
	//
	public static String DEFAULT = "default";

	private transient String name;

	private transient boolean overlayNames = false; // TODO add to JSON spec?

	public View() // deserialization
	{
        isExclusive = false;
	}

	public View( String name,
				 String uiSelectionGroup,
				 List< Display< ? > > sourceDisplays,
				 List< Transformation > sourceTransforms,
				 ViewerTransform viewerTransform,
				 boolean isExclusive,
				 String description
	) {
		this.name = name;
		this.uiSelectionGroups = new String[]{ uiSelectionGroup };
		this.sourceDisplays = sourceDisplays;
		this.sourceTransforms = sourceTransforms;
		this.viewerTransform = viewerTransform;
		this.isExclusive = isExclusive;
		this.description = description;
	}

	public View( String name,
				 String[] uiSelectionGroups,
				 List< Display< ? > > sourceDisplays,
				 List< Transformation > sourceTransforms,
				 ViewerTransform viewerTransform,
				 boolean isExclusive,
				 String description
	) {
		this.name = name;
		this.uiSelectionGroups = uiSelectionGroups;
		this.sourceDisplays = sourceDisplays;
		this.sourceTransforms = sourceTransforms;
		this.viewerTransform = viewerTransform;
		this.isExclusive = isExclusive;
		this.description = description;
	}

	// map of data source name to the
	// object that requested opening
	// of this data in this view
	public Map< String, Object > getSources()
	{
		final Map< String, Object > sources = new HashMap<>();

		for ( Display< ? > display : displays() )
		{
			for ( String source : display.getSources() )
			{
				sources.put( source, display );
			}

			if ( display instanceof RegionDisplay )
			{
				// TODO:
				//   https://github.com/mobie/mobie.github.io/issues/88
				final RegionDisplay regionDisplay = ( RegionDisplay ) display;
				sources.put( regionDisplay.tableSource, display );
			}

			if ( display instanceof SpotDisplay )
			{
				final SpotDisplay< ? > spotDisplay = ( SpotDisplay< ? > ) display;
				for ( String source : spotDisplay.sources )
					sources.put( source, display );
			}
		}

		List< Transformation > transformations = transformations();
		for ( Transformation transformation : transformations )
		{
			final List< String > transformationSources = transformation.getSources();
			for ( String source : transformationSources )
			{
				sources.put( source, transformation );
			}
		}

		return sources;
	}

	public Boolean isExclusive()
	{
		return isExclusive;
	}

	public List< Transformation > transformations()
	{
		if ( sourceTransforms == null )
			return new ArrayList<>();
		else
			return sourceTransforms;
	}

	public List< Display< ? > > displays()
	{
		if ( sourceDisplays == null )
			return new ArrayList<>();
		else
			return sourceDisplays;
	}

	public Set< String > getUiSelectionGroups()
	{
		HashSet< String > set = new HashSet<>();

		if ( uiSelectionGroup != null && uiSelectionGroups != null )
		{
			System.err.println("The view " + name + " has both the uiSelectionGroup and uiSelectionGroups fields set;\n" +
					"The uiSelectionGroup field is deprecated, please remove it.");
		}

		if ( uiSelectionGroup != null )
			set.add( uiSelectionGroup );

		if ( uiSelectionGroups != null )
			set.addAll( Arrays.asList( uiSelectionGroups ) );

		if ( set.isEmpty() )
			set.add("views"); // default

		return set;
	}

	public ViewerTransform getViewerTransform()
	{
		return viewerTransform;
	}

	public String getName()
	{
		return name;
	}

	public void setName( String name )
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}

	public boolean overlayNames()
	{
		return overlayNames;
	}

	public void overlayNames( boolean overlayNames )
	{
		this.overlayNames = overlayNames;
	}

	public String getDescription()
	{
		return description;
	}

	public void setViewerTransform( ViewerTransform viewerTransform )
	{
		this.viewerTransform = viewerTransform;
	}

	public void setExclusive( boolean exclusive )
	{
		isExclusive = exclusive;
	}

	public void setUiSelectionGroup( String uiSelectionGroup )
	{
		this.uiSelectionGroups = new String[]{ uiSelectionGroup };
	}

	public void setDescription( String description )
	{
		this.description = description;
	}

}
