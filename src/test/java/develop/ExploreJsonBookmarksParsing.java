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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ExploreJsonBookmarksParsing
{
	public static class Bookmark
	{
		public HashMap< String, LayerProperties > layers;
		public double[] view;
		public double[] position;
	}

	public static class LayerProperties
	{
		public Storage storage;
		public String type;
		public String color;
		public double maxValue;
		public double minValue;
		public String colorByColumn;
		public String colorMap;
		public double colorMapMinValue;
		public double colorMapMaxValue;
		public ArrayList< String > tables;
		public ArrayList< Double > selectedLabelIds;
		public boolean showSelectedSegmentsIn3d;
		public boolean showImageIn3d;
	}

	public static class Storage
	{
		public String local;
		public String remote;
	}

	public static void main( String[] args )
	{
		String json = getJsonString();
		
		Gson gson = new Gson();
		Type type = new TypeToken< Map< String, Bookmark > >() {}.getType();
		final Map< String, Bookmark > bookmarks = gson.fromJson( json, type );
		final Bookmark bookmark = bookmarks.get( "figure 2B: Epithelial cell segmentation" );
	}

	public static String getJsonString()
	{
		return "{\n" +
				"  \"figure 1B: Epithelial cell\": {\n" +
				"    \"position\": [\n" +
				"      133.10314559345912,\n" +
				"      147.86196442469097,\n" +
				"      54.51589254469087\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      35.24583885675167,\n" +
				"      -75.58494534746872,\n" +
				"      0.0,\n" +
				"      7386.806479095813,\n" +
				"      75.58494534746872,\n" +
				"      35.24583885675167,\n" +
				"      0.0,\n" +
				"      -14653.112956413184,\n" +
				"      0.0,\n" +
				"      0.0,\n" +
				"      83.39875970238356,\n" +
				"      -4546.557822295637\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 1C: Adult eye\": {\n" +
				"    \"position\": [\n" +
				"      233.17061242240516,\n" +
				"      155.4613397748416,\n" +
				"      75.68242384443185\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      11.86040032641921,\n" +
				"      74.88362052558196,\n" +
				"      0.0,\n" +
				"      -13549.004781783864,\n" +
				"      -74.88362052558196,\n" +
				"      11.86040032641921,\n" +
				"      0.0,\n" +
				"      16238.825933345837,\n" +
				"      0.0,\n" +
				"      0.0,\n" +
				"      75.81705427489416,\n" +
				"      -5738.018436268833\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 1D: Muscles\": {\n" +
				"    \"position\": [\n" +
				"      112.4372783127234,\n" +
				"      152.60438747748637,\n" +
				"      108.0387320192992\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      162.5205891508259,\n" +
				"      0.0,\n" +
				"      0.0,\n" +
				"      -17374.372713899185,\n" +
				"      0.0,\n" +
				"      162.5205891508259,\n" +
				"      0.0,\n" +
				"      -24134.354959842007,\n" +
				"      0.0,\n" +
				"      0.0,\n" +
				"      162.5205891508259,\n" +
				"      -17558.518378884706\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 1E: Nephridia\": {\n" +
				"    \"position\": [\n" +
				"      90.01039441808689,\n" +
				"      139.10020328862146,\n" +
				"      196.3890269963441\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      109.74645975191238,\n" +
				"      53.526924900739324,\n" +
				"      0.0,\n" +
				"      -16455.928263365946,\n" +
				"      -53.526924900739324,\n" +
				"      109.74645975191238,\n" +
				"      0.0,\n" +
				"      -9818.775239394663,\n" +
				"      0.0,\n" +
				"      0.0,\n" +
				"      122.10412408025984,\n" +
				"      -23979.9101203631\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 2B: Epithelial cell segmentation\": {\n" +
				"    \"layers\": {\n" +
				"      \"sbem-6dpf-1-whole-raw\": {},\n" +
				"      \"sbem-6dpf-1-whole-segmented-cells\": {\n" +
				"        \"maxValue\": 1000,\n" +
				"        \"minValue\": 0,\n" +
				"        \"selectedLabelIds\": [\n" +
				"          4193,\n" +
				"          4707,\n" +
				"          4690,\n" +
				"          3000,\n" +
				"          4036,\n" +
				"          3156,\n" +
				"          4356\n" +
				"        ],\n" +
				"        \"showSelectedSegmentsIn3d\": true\n" +
				"      }\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      123.52869410491485,\n" +
				"      149.1222916293258,\n" +
				"      54.60245703388086\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      36.55960993152054,\n" +
				"      -74.95830868923713,\n" +
				"      0.0,\n" +
				"      7198.793896571635,\n" +
				"      74.95830868923713,\n" +
				"      36.55960993152054,\n" +
				"      0.0,\n" +
				"      -14710.354798757155,\n" +
				"      0.0,\n" +
				"      0.0,\n" +
				"      83.39875970238346,\n" +
				"      -4553.7771933283475\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 2C: Muscle segmentation\": {\n" +
				"    \"layers\": {\n" +
				"      \"sbem-6dpf-1-whole-raw\": {},\n" +
				"      \"sbem-6dpf-1-whole-segmented-cells\": {\n" +
				"        \"maxValue\": 1000,\n" +
				"        \"minValue\": 0,\n" +
				"        \"selectedLabelIds\": [\n" +
				"          1375,\n" +
				"          5370,\n" +
				"          5583,\n" +
				"          5781,\n" +
				"          6544,\n" +
				"          7037,\n" +
				"          7462,\n" +
				"          8264,\n" +
				"          8230,\n" +
				"          8231,\n" +
				"          8985,\n" +
				"          9187,\n" +
				"          10181,\n" +
				"          11299\n" +
				"        ],\n" +
				"        \"showSelectedSegmentsIn3d\": true\n" +
				"      }\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      112.4385016688483,\n" +
				"      154.89179764379648,\n" +
				"      108.0387320192992\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      162.5205891508259,\n" +
				"      0.0,\n" +
				"      0.0,\n" +
				"      -17292.571534457347,\n" +
				"      0.0,\n" +
				"      162.5205891508259,\n" +
				"      0.0,\n" +
				"      -24390.10620770031,\n" +
				"      0.0,\n" +
				"      0.0,\n" +
				"      162.5205891508259,\n" +
				"      -17558.518378884706\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 2D: Nephridia segmentation\": {\n" +
				"    \"layers\": {\n" +
				"      \"sbem-6dpf-1-whole-raw\": {},\n" +
				"      \"sbem-6dpf-1-whole-segmented-cells\": {\n" +
				"        \"maxValue\": 1000,\n" +
				"        \"minValue\": 0,\n" +
				"        \"selectedLabelIds\": [\n" +
				"          23027,\n" +
				"          23028,\n" +
				"          23367,\n" +
				"          23556,\n" +
				"          23557,\n" +
				"          23790,\n" +
				"          24916\n" +
				"        ],\n" +
				"        \"showSelectedSegmentsIn3d\": true\n" +
				"      },\n" +
				"      \"sbem-6dpf-1-whole-segmented-cilia\": {\n" +
				"        \"maxValue\": 1000,\n" +
				"        \"minValue\": 0,\n" +
				"        \"selectedLabelIds\": [\n" +
				"          74,\n" +
				"          75,\n" +
				"          76,\n" +
				"          77,\n" +
				"          78,\n" +
				"          79,\n" +
				"          80,\n" +
				"          81,\n" +
				"          82,\n" +
				"          83,\n" +
				"          84,\n" +
				"          85,\n" +
				"          86,\n" +
				"          87,\n" +
				"          88,\n" +
				"          89,\n" +
				"          90,\n" +
				"          91,\n" +
				"          92,\n" +
				"          93,\n" +
				"          95,\n" +
				"          96,\n" +
				"          97,\n" +
				"          98,\n" +
				"          99,\n" +
				"          100,\n" +
				"          101,\n" +
				"          102,\n" +
				"          103,\n" +
				"          104,\n" +
				"          105,\n" +
				"          107,\n" +
				"          108,\n" +
				"          109,\n" +
				"          110,\n" +
				"          111,\n" +
				"          112,\n" +
				"          113,\n" +
				"          114,\n" +
				"          115,\n" +
				"          116,\n" +
				"          117,\n" +
				"          118,\n" +
				"          119,\n" +
				"          120,\n" +
				"          121,\n" +
				"          122,\n" +
				"          123,\n" +
				"          124,\n" +
				"          125,\n" +
				"          126,\n" +
				"          127,\n" +
				"          128,\n" +
				"          129,\n" +
				"          130,\n" +
				"          131,\n" +
				"          133,\n" +
				"          137,\n" +
				"          138,\n" +
				"          139,\n" +
				"          140,\n" +
				"          141,\n" +
				"          142,\n" +
				"          143,\n" +
				"          144,\n" +
				"          145,\n" +
				"          146,\n" +
				"          147,\n" +
				"          148,\n" +
				"          149,\n" +
				"          150,\n" +
				"          164,\n" +
				"          165,\n" +
				"          167,\n" +
				"          168,\n" +
				"          170,\n" +
				"          172,\n" +
				"          173,\n" +
				"          174,\n" +
				"          175,\n" +
				"          176,\n" +
				"          177,\n" +
				"          182,\n" +
				"          183,\n" +
				"          184\n" +
				"        ],\n" +
				"        \"showSelectedSegmentsIn3d\": true\n" +
				"      }\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      83.30399191428275,\n" +
				"      134.014171679122,\n" +
				"      196.13224525293464\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      49.66422153411607,\n" +
				"      111.54766791295017,\n" +
				"      0.0,\n" +
				"      -19052.196227198943,\n" +
				"      -111.54766791295017,\n" +
				"      49.66422153411607,\n" +
				"      0.0,\n" +
				"      3678.656514894519,\n" +
				"      0.0,\n" +
				"      0.0,\n" +
				"      122.10412408025985,\n" +
				"      -23948.556010504282\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 4B: Expression overlay\": {\n" +
				"    \"layers\": {\n" +
				"      \"prospr-6dpf-1-whole-glt1\": {\n" +
				"        \"color\": \"green\"\n" +
				"      },\n" +
				"      \"prospr-6dpf-1-whole-mhcl4\": {\n" +
				"        \"color\": \"red\"\n" +
				"      },\n" +
				"      \"prospr-6dpf-1-whole-paraxis\": {\n" +
				"        \"color\": \"magenta\"\n" +
				"      },\n" +
				"      \"prospr-6dpf-1-whole-pcdh15\": {\n" +
				"        \"color\": \"cyan\"\n" +
				"      },\n" +
				"      \"sbem-6dpf-1-whole-raw\": {}\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      135.86902198707617,\n" +
				"      125.49452126592294,\n" +
				"      202.8612557190891\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -2.4123128429247633,\n" +
				"      2.9689911141924665,\n" +
				"      0.42216296192752084,\n" +
				"      188.52595960198138,\n" +
				"      -2.8459979251482688,\n" +
				"      -2.4373246522315175,\n" +
				"      0.8787078037625795,\n" +
				"      815.2980765871299,\n" +
				"      0.9452119405874039,\n" +
				"      0.23858616512209418,\n" +
				"      3.7231752690278146,\n" +
				"      -913.6542888480263\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 4C: Expression overlay\": {\n" +
				"    \"layers\": {\n" +
				"      \"prospr-6dpf-1-whole-mhcl4\": {\n" +
				"        \"color\": \"red\"\n" +
				"      },\n" +
				"      \"prospr-6dpf-1-whole-segmented-prospr6-ref\": {\n" +
				"        \"color\": \"blue\"\n" +
				"      },\n" +
				"      \"sbem-6dpf-1-whole-raw\": {}\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      103.4376445449625,\n" +
				"      153.09461413596443,\n" +
				"      122.4673472530251\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -3.6520973471261806,\n" +
				"      4.471613105933289,\n" +
				"      0.12177861317601404,\n" +
				"      -142.7294394903588,\n" +
				"      -1.865354415296328,\n" +
				"      -1.665249762719989,\n" +
				"      5.205320832164245,\n" +
				"      109.40680290735682,\n" +
				"      4.065784881805228,\n" +
				"      3.2526279054441813,\n" +
				"      2.497553570251781,\n" +
				"      -1224.3837858833558\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 4D: Expression overlay\": {\n" +
				"    \"layers\": {\n" +
				"      \"prospr-6dpf-1-whole-calmodulin\": {\n" +
				"        \"color\": \"cyan\"\n" +
				"      },\n" +
				"      \"prospr-6dpf-1-whole-twist\": {\n" +
				"        \"color\": \"magenta\"\n" +
				"      },\n" +
				"      \"sbem-6dpf-1-whole-raw\": {}\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      158.802576235763,\n" +
				"      144.87167821937132,\n" +
				"      91.47488659012399\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -2.7911887607381565,\n" +
				"      3.3264092332182793,\n" +
				"      0.15163711598178808,\n" +
				"      343.47448991749434,\n" +
				"      -3.328436827001935,\n" +
				"      -2.7928901139341997,\n" +
				"      -1.847653600673921e-16,\n" +
				"      1311.1750208538347,\n" +
				"      0.0974704593216939,\n" +
				"      -0.11616077006843682,\n" +
				"      4.342318860345338,\n" +
				"      -395.86327963234544\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 4E: Expression overlay\": {\n" +
				"    \"layers\": {\n" +
				"      \"prospr-6dpf-1-whole-glt1\": {\n" +
				"        \"color\": \"green\"\n" +
				"      },\n" +
				"      \"prospr-6dpf-1-whole-mitf\": {\n" +
				"        \"color\": \"cyan\"\n" +
				"      },\n" +
				"      \"prospr-6dpf-1-whole-olig\": {\n" +
				"        \"color\": \"magenta\"\n" +
				"      },\n" +
				"      \"sbem-6dpf-1-whole-raw\": {}\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      201.5353707550322,\n" +
				"      184.40883596303107,\n" +
				"      55.08612898912396\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -2.993816332579886,\n" +
				"      3.716423506126069,\n" +
				"      0.04869816935451629,\n" +
				"      245.33595821000614,\n" +
				"      -0.9866786530216581,\n" +
				"      -0.7344082526480016,\n" +
				"      -4.611314473761342,\n" +
				"      866.3014830562887,\n" +
				"      -3.5833839348102585,\n" +
				"      -2.9027490872211263,\n" +
				"      1.2290316245677486,\n" +
				"      1189.7685955241589\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 4F: Expression overlay\": {\n" +
				"    \"layers\": {\n" +
				"      \"prospr-6dpf-1-whole-cal2\": {\n" +
				"        \"color\": \"magenta\"\n" +
				"      },\n" +
				"      \"prospr-6dpf-1-whole-segmented-prospr6-ref\": {\n" +
				"        \"color\": \"blue\"\n" +
				"      },\n" +
				"      \"sbem-6dpf-1-whole-raw\": {}\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      152.53919573222748,\n" +
				"      138.6890283460845,\n" +
				"      129.4285526661694\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -2.875574943913148,\n" +
				"      3.5501633269433537,\n" +
				"      -0.11216932458395215,\n" +
				"      200.78710026302286,\n" +
				"      -3.5510393216417873,\n" +
				"      -2.8766567072753837,\n" +
				"      -0.011780834164108333,\n" +
				"      1294.1581820690833,\n" +
				"      -0.0797580184367246,\n" +
				"      0.07974587090420004,\n" +
				"      4.568637884313116,\n" +
				"      -590.2058423872585\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 4G: Expression overlay\": {\n" +
				"    \"layers\": {\n" +
				"      \"prospr-6dpf-1-whole-segmented-glands\": {\n" +
				"        \"color\": \"cyan\"\n" +
				"      },\n" +
				"      \"sbem-6dpf-1-whole-raw\": {}\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      140.40425446521544,\n" +
				"      139.66165344962474,\n" +
				"      191.84386140627672\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -1.8687724918856328,\n" +
				"      2.299342738818519,\n" +
				"      0.047694908329288105,\n" +
				"      366.1036243542131,\n" +
				"      -0.9675879478058556,\n" +
				"      -0.8418130290781947,\n" +
				"      2.671457813367352,\n" +
				"      20.919681477345335,\n" +
				"      2.0863905190780634,\n" +
				"      1.6691124152624248,\n" +
				"      1.2816398902907205,\n" +
				"      -771.9238505489849\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 4H left: Expression overlay\": {\n" +
				"    \"layers\": {\n" +
				"      \"prospr-6dpf-1-whole-dach\": {\n" +
				"        \"color\": \"magenta\"\n" +
				"      },\n" +
				"      \"prospr-6dpf-1-whole-glt1\": {\n" +
				"        \"color\": \"green\"\n" +
				"      },\n" +
				"      \"sbem-6dpf-1-whole-raw\": {}\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      192.77360495262258,\n" +
				"      175.17112516946474,\n" +
				"      57.192502177868164\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -2.7039276338083513,\n" +
				"      3.4009922651749314,\n" +
				"      -0.027456569377272116,\n" +
				"      325.0605456208178,\n" +
				"      -1.4863737156676018,\n" +
				"      -1.1501014902725006,\n" +
				"      3.917484733767262,\n" +
				"      503.94743741843087,\n" +
				"      3.0591167725920765,\n" +
				"      2.4472934180738037,\n" +
				"      1.8791717317351737,\n" +
				"      -1125.8866432472726\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 4H right: Expression overlay\": {\n" +
				"    \"layers\": {\n" +
				"      \"prospr-6dpf-1-whole-barh1\": {\n" +
				"        \"color\": \"cyan\"\n" +
				"      },\n" +
				"      \"prospr-6dpf-1-whole-glt1\": {\n" +
				"        \"color\": \"green\"\n" +
				"      },\n" +
				"      \"sbem-6dpf-1-whole-raw\": {}\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      208.84273690626745,\n" +
				"      170.83152895127307,\n" +
				"      61.136309953766585\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -3.958820448658808,\n" +
				"      4.979392775442618,\n" +
				"      -0.04019916322526412,\n" +
				"      296.5912448434393,\n" +
				"      -2.1761997571089364,\n" +
				"      -1.6838635919079685,\n" +
				"      5.735589398708651,\n" +
				"      725.4877340334158,\n" +
				"      4.478852866752061,\n" +
				"      3.5830822934018567,\n" +
				"      2.7512953324334686,\n" +
				"      -1715.6833616510596\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 5C: Gene clustering full body\": {\n" +
				"    \"layers\": {\n" +
				"      \"sbem-6dpf-1-whole-raw\": {},\n" +
				"      \"sbem-6dpf-1-whole-segmented-cells\": {\n" +
				"        \"colorByColumn\": \"clusters\",\n" +
				"        \"colorMap\": \"glasbey\",\n" +
				"        \"tables\": [\n" +
				"          \"gene_clusters\"\n" +
				"        ]\n" +
				"      }\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      34.3200812765686,\n" +
				"      282.12761791060416,\n" +
				"      105.03347558347836\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -0.9529921731518778,\n" +
				"      1.1849892416847392,\n" +
				"      0.00814080433223175,\n" +
				"      376.53351985923825,\n" +
				"      -0.5079154826309126,\n" +
				"      -0.4178950423193335,\n" +
				"      1.3710745617220128,\n" +
				"      196.32270696996784,\n" +
				"      1.0706482326644668,\n" +
				"      0.8565185861315733,\n" +
				"      0.6576839143510288,\n" +
				"      -347.4711101247537\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 5D bottom left: Gene clustering head zoom\": {\n" +
				"    \"layers\": {\n" +
				"      \"sbem-6dpf-1-whole-raw\": {},\n" +
				"      \"sbem-6dpf-1-whole-segmented-cells\": {\n" +
				"        \"colorByColumn\": \"clusters\",\n" +
				"        \"colorMap\": \"glasbey\",\n" +
				"        \"tables\": [\n" +
				"          \"gene_clusters\"\n" +
				"        ]\n" +
				"      }\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      253.47173714934286,\n" +
				"      196.28573239362976,\n" +
				"      27.21939599932696\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      3.9442456281833653,\n" +
				"      -8.350934537537663e-15,\n" +
				"      -1.1230567136688586e-14,\n" +
				"      -304.7547911193368,\n" +
				"      -6.335191718132021e-15,\n" +
				"      3.944245628183366,\n" +
				"      -8.49491616749521e-15,\n" +
				"      -397.1991418683425,\n" +
				"      5.759265198301837e-15,\n" +
				"      -4.3194488987263775e-16,\n" +
				"      3.9442456281833636,\n" +
				"      -107.35998367213847\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 5D bottom middle: Gene clustering head zoom\": {\n" +
				"    \"layers\": {\n" +
				"      \"sbem-6dpf-1-whole-raw\": {},\n" +
				"      \"sbem-6dpf-1-whole-segmented-cells\": {\n" +
				"        \"colorByColumn\": \"clusters\",\n" +
				"        \"colorMap\": \"glasbey\",\n" +
				"        \"tables\": [\n" +
				"          \"gene_clusters\"\n" +
				"        ]\n" +
				"      }\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      118.2508175986159,\n" +
				"      217.4001476148915,\n" +
				"      96.1816510182236\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -2.6932927556972674,\n" +
				"      3.4012062348993246,\n" +
				"      -0.0450478198965906,\n" +
				"      319.39410655025426,\n" +
				"      -1.496374493696442,\n" +
				"      -1.1331166722108985,\n" +
				"      3.911645307036318,\n" +
				"      532.0587352884239,\n" +
				"      3.0546843589821346,\n" +
				"      2.4437474871857057,\n" +
				"      1.8764489633747354,\n" +
				"      -1072.96994675185\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 5D top left: Head ganglia\": {\n" +
				"    \"layers\": {\n" +
				"      \"sbem-6dpf-1-whole-raw\": {},\n" +
				"      \"sbem-6dpf-1-whole-segmented-cells\": {\n" +
				"        \"colorByColumn\": \"merged_bilateral_ganglion_id\",\n" +
				"        \"colorMap\": \"glasbey\",\n" +
				"        \"tables\": [\n" +
				"          \"ganglia_ids\"\n" +
				"        ]\n" +
				"      }\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      253.47173714934286,\n" +
				"      196.28573239362976,\n" +
				"      27.21939599932696\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      3.9442456281833653,\n" +
				"      -8.350934537537663e-15,\n" +
				"      -1.1230567136688586e-14,\n" +
				"      -304.7547911193368,\n" +
				"      -6.335191718132021e-15,\n" +
				"      3.944245628183366,\n" +
				"      -8.49491616749521e-15,\n" +
				"      -397.1991418683425,\n" +
				"      5.759265198301837e-15,\n" +
				"      -4.3194488987263775e-16,\n" +
				"      3.9442456281833636,\n" +
				"      -107.35998367213847\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 5D top middle: Head ganglia\": {\n" +
				"    \"layers\": {\n" +
				"      \"sbem-6dpf-1-whole-raw\": {},\n" +
				"      \"sbem-6dpf-1-whole-segmented-cells\": {\n" +
				"        \"colorByColumn\": \"merged_bilateral_ganglion_id\",\n" +
				"        \"colorMap\": \"glasbey\",\n" +
				"        \"tables\": [\n" +
				"          \"ganglia_ids\"\n" +
				"        ]\n" +
				"      }\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      118.2508175986159,\n" +
				"      217.4001476148915,\n" +
				"      96.1816510182236\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -2.6932927556972674,\n" +
				"      3.4012062348993246,\n" +
				"      -0.0450478198965906,\n" +
				"      319.39410655025426,\n" +
				"      -1.496374493696442,\n" +
				"      -1.1331166722108985,\n" +
				"      3.911645307036318,\n" +
				"      532.0587352884239,\n" +
				"      3.0546843589821346,\n" +
				"      2.4437474871857057,\n" +
				"      1.8764489633747354,\n" +
				"      -1072.96994675185\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 6A and 6C: Gene expression assignment\": {\n" +
				"    \"layers\": {\n" +
				"      \"prospr-6dpf-1-whole-msx\": {\n" +
				"        \"color\": \"green\"\n" +
				"      },\n" +
				"      \"prospr-6dpf-1-whole-patched\": {\n" +
				"        \"color\": \"blue\"\n" +
				"      },\n" +
				"      \"prospr-6dpf-1-whole-virtual-cells\": {\n" +
				"        \"selectedLabelIds\": [\n" +
				"          3666,\n" +
				"          3641,\n" +
				"          4015\n" +
				"        ],\n" +
				"        \"showSelectedSegmentsIn3d\": false\n" +
				"      },\n" +
				"      \"sbem-6dpf-1-whole-raw\": {},\n" +
				"      \"sbem-6dpf-1-whole-segmented-cells\": {\n" +
				"        \"selectedLabelIds\": [\n" +
				"          32101,\n" +
				"          31967\n" +
				"        ],\n" +
				"        \"showSelectedSegmentsIn3d\": false\n" +
				"      }\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      119.96805985437719,\n" +
				"      99.19674811104227,\n" +
				"      263.3359252291697\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -13.194629461107638,\n" +
				"      10.214861012475334,\n" +
				"      0.30412004228770306,\n" +
				"      1308.0492653634751,\n" +
				"      9.677834961769312,\n" +
				"      12.330343938455595,\n" +
				"      5.73029230776116,\n" +
				"      -3359.034760194792,\n" +
				"      3.2825881491691082,\n" +
				"      4.706734959124246,\n" +
				"      -15.671802135929735,\n" +
				"      3266.795464793777\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 6B and 6D: Gene expression assignment\": {\n" +
				"    \"layers\": {\n" +
				"      \"prospr-6dpf-1-whole-lhx6\": {\n" +
				"        \"color\": \"green\"\n" +
				"      },\n" +
				"      \"prospr-6dpf-1-whole-virtual-cells\": {\n" +
				"        \"selectedLabelIds\": [\n" +
				"          737,\n" +
				"          739,\n" +
				"          1880\n" +
				"        ],\n" +
				"        \"showSelectedSegmentsIn3d\": false\n" +
				"      },\n" +
				"      \"prospr-6dpf-1-whole-wnt5\": {\n" +
				"        \"color\": \"blue\"\n" +
				"      },\n" +
				"      \"sbem-6dpf-1-whole-raw\": {},\n" +
				"      \"sbem-6dpf-1-whole-segmented-cells\": {\n" +
				"        \"selectedLabelIds\": [\n" +
				"          6759,\n" +
				"          6906\n" +
				"        ],\n" +
				"        \"showSelectedSegmentsIn3d\": false\n" +
				"      }\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      196.76744017557536,\n" +
				"      178.25856120112738,\n" +
				"      71.18235953395656\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -6.194255640646106,\n" +
				"      7.933757678523062,\n" +
				"      0.04504878697308157,\n" +
				"      629.360918560249,\n" +
				"      7.898413563957639,\n" +
				"      6.161063641179686,\n" +
				"      0.9857490646362296,\n" +
				"      -2224.580902902456,\n" +
				"      0.749402073270016,\n" +
				"      0.6419712954778718,\n" +
				"      -10.017066068703377,\n" +
				"      451.14359129392136\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 6F: Virtual cell assignment: gene expression level\": {\n" +
				"    \"layers\": {\n" +
				"      \"sbem-6dpf-1-whole-raw\": {},\n" +
				"      \"sbem-6dpf-1-whole-segmented-cells\": {\n" +
				"        \"colorByColumn\": \"expression_sum\",\n" +
				"        \"colorMap\": \"viridis\",\n" +
				"        \"colorMapMaxValue\": 40,\n" +
				"        \"colorMapMinValue\": 0,\n" +
				"        \"maxValue\": 1000,\n" +
				"        \"minValue\": 0,\n" +
				"        \"tables\": [\n" +
				"          \"vc_assignments\"\n" +
				"        ]\n" +
				"      },\n" +
				"      \"sbem-6dpf-1-whole-segmented-outside\": {}\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      151.49356333542673,\n" +
				"      142.11330737746303,\n" +
				"      124.51951538415905\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -1.6868627317328129,\n" +
				"      2.5114207685721133,\n" +
				"      -0.040111647775085676,\n" +
				"      666.6372173919165,\n" +
				"      -1.0506500045055518,\n" +
				"      -0.6616293061174092,\n" +
				"      2.7591176716716426,\n" +
				"      356.629046586707,\n" +
				"      2.2814420415901586,\n" +
				"      1.5522118029711398,\n" +
				"      1.2409713237596134,\n" +
				"      -720.738885334493\n" +
				"    ]\n" +
				"  },\n" +
				"  \"figure 7D: MMB - PlatyBrowser\": {\n" +
				"    \"layers\": {\n" +
				"      \"prospr-6dpf-1-whole-arx\": {\n" +
				"        \"color\": \"yellow\"\n" +
				"      },\n" +
				"      \"sbem-6dpf-1-whole-raw\": {},\n" +
				"      \"sbem-6dpf-1-whole-segmented-cells\": {\n" +
				"        \"maxValue\": 1000,\n" +
				"        \"minValue\": 0,\n" +
				"        \"selectedLabelIds\": [\n" +
				"          1667,\n" +
				"          1668,\n" +
				"          1681\n" +
				"        ],\n" +
				"        \"showSelectedSegmentsIn3d\": true\n" +
				"      }\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      223.89114744960432,\n" +
				"      161.56331936878885,\n" +
				"      26.85730262369839\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      21.66959697286015,\n" +
				"      0.0,\n" +
				"      0.0,\n" +
				"      -4464.63093102413,\n" +
				"      0.0,\n" +
				"      21.66959697286015,\n" +
				"      0.0,\n" +
				"      -2958.012016319144,\n" +
				"      0.0,\n" +
				"      0.0,\n" +
				"      21.66959697286015,\n" +
				"      -581.9869236336835\n" +
				"    ]\n" +
				"  },\n" +
				"  \"left eye\": {\n" +
				"    \"position\": [\n" +
				"      177.0,\n" +
				"      218.0,\n" +
				"      67.0\n" +
				"    ]\n" +
				"  },\n" +
				"  \"suppl. Fig. 1A: Ciliated support cells\": {\n" +
				"    \"position\": [\n" +
				"      195.47818432008413,\n" +
				"      111.9102705017601,\n" +
				"      61.26057745239114\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      61.05278354186309,\n" +
				"      14.095145699490478,\n" +
				"      0.0,\n" +
				"      -12646.878842442213,\n" +
				"      -14.095145699490478,\n" +
				"      61.05278354186309,\n" +
				"      0.0,\n" +
				"      -3432.1400319918653,\n" +
				"      0.0,\n" +
				"      0.0,\n" +
				"      62.658722541234816,\n" +
				"      -3838.509525305202\n" +
				"    ]\n" +
				"  },\n" +
				"  \"suppl. Fig. 1B: Nephridia: Clathrin pit\": {\n" +
				"    \"position\": [\n" +
				"      104.81359439167251,\n" +
				"      163.98830048765967,\n" +
				"      184.47804817270034\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      106.7946732435894,\n" +
				"      59.19725402586242,\n" +
				"      0.0,\n" +
				"      -20020.190645782226,\n" +
				"      -59.19725402586242,\n" +
				"      106.7946732435894,\n" +
				"      0.0,\n" +
				"      -10661.39999378362,\n" +
				"      0.0,\n" +
				"      0.0,\n" +
				"      122.10412408025984,\n" +
				"      -22525.530484163555\n" +
				"    ]\n" +
				"  },\n" +
				"  \"suppl. Fig. 2E: Chromatin segmentation\": {\n" +
				"    \"layers\": {\n" +
				"      \"sbem-6dpf-1-whole-raw\": {},\n" +
				"      \"sbem-6dpf-1-whole-segmented-chromatin\": {}\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      182.8222762123126,\n" +
				"      166.4643272001219,\n" +
				"      117.2710494132358\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      18.44692536537881,\n" +
				"      0.0,\n" +
				"      0.0,\n" +
				"      -2766.5088844171983,\n" +
				"      0.0,\n" +
				"      18.44692536537881,\n" +
				"      0.0,\n" +
				"      -2571.7550198586464,\n" +
				"      0.0,\n" +
				"      0.0,\n" +
				"      18.44692536537881,\n" +
				"      -2163.2902960456113\n" +
				"    ]\n" +
				"  },\n" +
				"  \"suppl. Fig. 2F: Morphology Clustering Full Body\": {\n" +
				"    \"layers\": {\n" +
				"      \"sbem-6dpf-1-whole-raw\": {},\n" +
				"      \"sbem-6dpf-1-whole-segmented-cells\": {\n" +
				"        \"colorByColumn\": \"clusters\",\n" +
				"        \"colorMap\": \"glasbey\",\n" +
				"        \"tables\": [\n" +
				"          \"morphology_clusters\"\n" +
				"        ]\n" +
				"      }\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      14.336284436072049,\n" +
				"      231.01765825291784,\n" +
				"      217.09553489633\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -1.907526694743768,\n" +
				"      2.3355679099972035,\n" +
				"      0.06360617842372362,\n" +
				"      915.9807986794217,\n" +
				"      -0.9742931264238693,\n" +
				"      -0.8697764801652066,\n" +
				"      2.718790738114475,\n" +
				"      367.6641394736041,\n" +
				"      2.1235998003262138,\n" +
				"      1.698879840260972,\n" +
				"      1.3044970202003874,\n" +
				"      -706.1162514871149\n" +
				"    ]\n" +
				"  },\n" +
				"  \"symmetric cell pairs\": {\n" +
				"    \"layers\": {\n" +
				"      \"sbem-6dpf-1-whole-raw\": {},\n" +
				"      \"sbem-6dpf-1-whole-segmented-cells\": {\n" +
				"        \"colorByColumn\": \"pair_index\",\n" +
				"        \"colorMap\": \"glasbeyZeroTransparent\",\n" +
				"        \"tables\": [\n" +
				"          \"symmetric_cells\"\n" +
				"        ]\n" +
				"      },\n" +
				"      \"sbem-6dpf-1-whole-segmented-outside\": {}\n" +
				"    },\n" +
				"    \"position\": [\n" +
				"      151.49356333542673,\n" +
				"      142.11330737746303,\n" +
				"      124.51951538415905\n" +
				"    ],\n" +
				"    \"view\": [\n" +
				"      -1.6868627317328129,\n" +
				"      2.5114207685721133,\n" +
				"      -0.040111647775085676,\n" +
				"      666.6372173919165,\n" +
				"      -1.0506500045055518,\n" +
				"      -0.6616293061174092,\n" +
				"      2.7591176716716426,\n" +
				"      356.629046586707,\n" +
				"      2.2814420415901586,\n" +
				"      1.5522118029711398,\n" +
				"      1.2409713237596134,\n" +
				"      -720.738885334493\n" +
				"    ]\n" +
				"  }\n" +
				"}\n";
	}
}
