package de.embl.cba.platynereis.utils;

import de.embl.cba.transforms.utils.TransformConversions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.embl.cba.transforms.utils.TransformConversions.changeTransformToScaledUnits;

public class BatchApplyAmiraTransformationsToBdvXml
{

	final double[] rotationAxis;
	final double rotationAngleInDegrees;
	final double[] translationInMicrometer;
	final String directory;
	final String filePattern;
	private static final Pattern sizePattern = Pattern.compile( ".*<size>(.*)</size>.*" );
	private static final String voxelSizeStart = "<voxelSize>";
	private static final String voxelSizeEnd = "</voxelSize>";
	private double[] voxelSizeInMicrometer;
	private double[] imageCentreInPixels;
	private double[] imageCentreInMicrometer;

	public BatchApplyAmiraTransformationsToBdvXml( double[] rotationAxis,
												   double rotationAngleInDegrees,
												   double[] translationInMicrometer,
												   String directory,
												   String filePattern )
	{
		this.rotationAxis = rotationAxis;
		this.rotationAngleInDegrees = rotationAngleInDegrees;
		this.translationInMicrometer = translationInMicrometer;
		this.directory = directory;
		this.filePattern = filePattern;

	}

	public void run() throws IOException
	{

		System.out.println( "Fetching files....");
		final List< File > files = FileUtils.getFileList( new File( directory ), filePattern );

		for ( File file : files )
		{

			if ( file.getName().contains( "OldAligned.xml" ) ) continue;
			if ( file.getName().contains( "-test.xml" ) ) continue;
			if ( file.getName().contains( "-aligned.xml" ) ) continue;

			setVoxelSizeAndDimensions( file );

			final AffineTransform3D affineTransform3D = TransformConversions.getAmiraAsPixelUnitsAffineTransform3D(
					rotationAxis,
					rotationAngleInDegrees,
					translationInMicrometer,
					voxelSizeInMicrometer,
					imageCentreInPixels );

			System.out.println( "\n" + file.getName() );
			System.out.println( "Transform in pixel units");
			System.out.println( TransformConversions.asStringBdvStyle( affineTransform3D ) );

			System.out.println( "Transform in scaled units");
			changeTransformToScaledUnits( affineTransform3D, voxelSizeInMicrometer );
			System.out.println( TransformConversions.asStringBdvStyle( affineTransform3D ) );

			List< String > fileContent = new ArrayList<>( Files.readAllLines( file.toPath(), StandardCharsets.UTF_8));

			for (int i = 0; i < fileContent.size(); i++) {
				if ( fileContent.get(i).contains( "<affine>" ))
				{
					String newAffine = "       <affine>"
							+ TransformConversions.asStringBdvStyle( affineTransform3D )
							+ "</affine>";

					fileContent.set(i, newAffine);
					break;
				}
			}

			// TODO: I cannot overwrite the original xml files...why?
			final Path path = new File( directory + File.separator + file.getName() + "-aligned.xml" ).toPath();

			Files.write( path, fileContent, StandardCharsets.UTF_8);

			System.out.println( "Written: " + path );
		}

	}


	private void setVoxelSizeAndDimensions( File file ) throws IOException
	{
		final List< String > lines = Files.readAllLines( file.toPath() );

		boolean isVoxelSize = false;

		for ( String line : lines )
		{

			if ( line.contains( voxelSizeStart ) )isVoxelSize = true;
			if ( line.contains( voxelSizeEnd ) )isVoxelSize = false;

			final Matcher matcher = sizePattern.matcher( line );

			if ( matcher.matches()  )
			{
				final String[] sizes = matcher.group( 1 ).split( " " );

				if ( isVoxelSize )
				{
					voxelSizeInMicrometer = new double[ 3 ];
					for ( int d = 0; d < sizes.length; ++d ) voxelSizeInMicrometer[ d ] = Double.parseDouble( sizes[ d ] );
				}
				else
				{
					imageCentreInPixels = new double[ 3 ];
					for ( int d = 0; d < sizes.length; ++d ) imageCentreInPixels[ d ] = Long.parseLong( sizes[ d ] ) / 2.0;
				}
			}
		}

		imageCentreInMicrometer = new double[ 3 ];

		for ( int d = 0; d < imageCentreInMicrometer.length; ++d )
		{
			imageCentreInMicrometer[ d ] = imageCentreInPixels[ d ] * voxelSizeInMicrometer[ d ];
		}

	}

	public static void main( String[] args ) throws IOException
	{
		final BatchApplyAmiraTransformationsToBdvXml batchApplyAmiraTransformationsToBdvXml = new BatchApplyAmiraTransformationsToBdvXml(
				new double[]{ 0.064, 0.762, 0.643 },
				237.0,
				new double[]{ 147.9, 48.13, 103.0661 },
				"/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr",
				".*parapod-fib.*.xml" );

		// Old one
//		final BatchApplyAmiraTransformationsToBdvXml batchApplyAmiraTransformationsToBdvXml = new BatchApplyAmiraTransformationsToBdvXml(
//				new double[]{ -0.387, -0.727, -0.565 },
//				107.596,
//				new double[]{ 148.578, 45.701, 115.941 },
//				"/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr",
//				".*parapod.*.xml" );

		batchApplyAmiraTransformationsToBdvXml.run();

	}
}
