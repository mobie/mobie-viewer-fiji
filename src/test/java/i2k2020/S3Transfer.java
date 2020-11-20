package i2k2020;

import de.embl.cba.tables.image.SourceAndMetadata;
import mdbtools.libmdb.mem;
import org.scijava.command.Interactive;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class S3Transfer
{

	/**
	 *
	 * -bash-4.2$ ls /g/arendt/EM_6dpf_segmentation/platy-browser-data/data/rawdata/sbem-6dpf-1-whole-raw.n5/setup0/timepoint0/s1
	 * 0    107  116  125  134  143  23  32  41  50  6   69  78  87  96
	 * 1    108  117  126  135  15   24  33  42  51  60  7   79  88  97
	 * 10   109  118  127  136  16   25  34  43  52  61  70  8   89  98
	 * 100  11   119  128  137  17   26  35  44  53  62  71  80  9   99
	 * 101  110  12   129  138  18   27  36  45  54  63  72  81  90  attributes.json
	 * 102  111  120  13   139  19   28  37  46  55  64  73  82  91
	 * 103  112  121  130  14   2    29  38  47  56  65  74  83  92
	 * 104  113  122  131  140  20   3   39  48  57  66  75  84  93
	 * 105  114  123  132  141  21   30  4   49  58  67  76  85  94
	 * 106  115  124  133  142  22   31  40  5   59  68  77  86  95
	 *
	 * start from 94 inclusive
	 *
	 *-bash-4.2$ ls /g/arendt/EM_6dpf_segmentation/platy-browser-data/data/rawdata/sbem-6dpf-1-whole-raw.n5/setup0/timepoint0/s0
	 * 0    117  136  155  174  193  211  230  25   269  3   49  68  87
	 * 1    118  137  156  175  194  212  231  250  27   30  5   69  88
	 * 10   119  138  157  176  195  213  232  251  270  31  50  7   89
	 * 100  12   139  158  177  196  214  233  252  271  32  51  70  9
	 * 101  120  14   159  178  197  215  234  253  272  33  52  71  90
	 * 102  121  140  16   179  198  216  235  254  273  34  53  72  91
	 * 103  122  141  160  18   199  217  236  255  274  35  54  73  92
	 * 104  123  142  161  180  2    218  237  256  275  36  55  74  93
	 * 105  124  143  162  181  20   219  238  257  276  37  56  75  94
	 * 106  125  144  163  182  200  22   239  258  277  38  57  76  95
	 * 107  126  145  164  183  201  220  24   259  278  39  58  77  96
	 * 108  127  146  165  184  202  221  240  26   279  4   59  78  97
	 * 109  128  147  166  185  203  222  241  260  28   40  6   79  98
	 * 11   129  148  167  186  204  223  242  261  280  41  60  8   99
	 * 110  13   149  168  187  205  224  243  262  281  42  61  80  attributes.json
	 * 111  130  15   169  188  206  225  244  263  282  43  62  81
	 * 112  131  150  17   189  207  226  245  264  283  44  63  82
	 * 113  132  151  170  19   208  227  246  265  284  45  64  83
	 * 114  133  152  171  190  209  228  247  266  285  46  65  84
	 * 115  134  153  172  191  21   229  248  267  286  47  66  85
	 * 116  135  154  173  192  210  23   249  268  29   48  67  86
	 *
	 * start from 142 inclusive
	 *
	 * @param args
	 */
	public static void main( String[] args )
	{
		final String template = "sbatch -c 2 -t 48:00:00 --mem 16000 -e /g/cba/tischer/tmp/err_LEVEL_GROUP.txt -o /g/cba/tischer/tmp/out_LEVEL_GROUP.txt /g/cba/tischer/software/aws --profile tischi --endpoint-url=https://idr-ftp.openmicroscopy.org s3 cp --recursive /g/arendt/EM_6dpf_segmentation/platy-browser-data/data/rawdata/sbem-6dpf-1-whole-raw.n5/setup0/timepoint0/sLEVEL/GROUP s3://idr-upload/tischi/sbem-6dpf-1-whole-raw.n5/setup0/timepoint0/sLEVEL/GROUP";

		// level 1, start "94", end 144
		// level 0, start "142", end 286
		List< String > list = IntStream.range( 0, 144 ).mapToObj( i -> String.valueOf( i ) ).collect( Collectors.toList() );
		Collections.sort( list );
		list.add( "attributes.json" );

		IntStream.range( list.indexOf( "94" ), list.size() ).forEach(  i ->
		{
			String job = template.replace( "LEVEL", "1" ).replace( "GROUP", list.get( i ) );
			System.out.println( job );
		});

		// /g/cba/tischer/software/aws --profile tischi --endpoint-url=https://idr-ftp.openmicroscopy.org s3 sync /g/arendt/EM_6dpf_segmentation/platy-browser-data/data/rawdata/sbem-6dpf-1-whole-raw.n5/setup0/timepoint0/s1/94 s3://idr-upload/tischi/sbem-6dpf-1-whole-raw.n5/setup0/timepoint0/s1/94


		// sacct --format="JobID,State,CPUTime,MaxRSS"
		// TODO: attributes.json => check whether it arrived (did the sync command work?)
	}
}
