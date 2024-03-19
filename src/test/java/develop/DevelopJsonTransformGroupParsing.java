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
package develop;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.xml.transform.Source;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DevelopJsonTransformGroupParsing
{
	public interface SourceTransformer
	{
		List< Source > transform( List< Source > sources );
	}

	public static class AffineSourceTransformer implements SourceTransformer
	{
		public String[] sources;
		public double[] transform;

		@Override
		public List< Source > transform( List< Source > sources )
		{
			return null;
		}
	}

	public static class AutoGridSourceTransformer implements SourceTransformer
	{
		public String[][] sources;
		public String gridType;

		@Override
		public List< Source > transform( List< Source > sources )
		{
			return null;}
	}

	public static class SourceTransformerSupplier
	{
		AffineSourceTransformer affine;
		AutoGridSourceTransformer autoGrid;

		public SourceTransformer get()
		{
			return null;
		}
	}

	public static class View
	{
		public List< SourceTransformerSupplier > transformGroups;
	}

	public static void main( String[] args )
	{
		Gson gson = new Gson();

		Type type = new TypeToken< View >() {}.getType();
		View view = gson.fromJson( json(), type );

		final ArrayList< SourceTransformer > sourceTransformers = new ArrayList<>();
		for ( SourceTransformerSupplier transformerSupplier : view.transformGroups )
		{
			sourceTransformers.add( transformerSupplier.get() );
		}
	}

	public static String json()
	{
		return "{\n" +
				"  \"transformGroups\": [\n" +
				"    {\"affine\": {\"transform\" : [0,0,0], \"sources\": [\"imE\", \"imF\"]}},\n" +
				"    {\"affine\": {\"transform\" : [1,0,0], \"sources\": [\"imG\", \"imH\"]}},\n" +
				"    {\"autoGrid\": {\"gridType\" : \"something\", \"sources\": [ [\"imA\", \"imB\"], [\"imC\", \"imD\"] ]}}\n" +
				"  ]\n" +
				"}";
	}
}
