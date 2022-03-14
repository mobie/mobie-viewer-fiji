package develop;

import net.imglib2.type.numeric.integer.UnsignedIntType;
import org.embl.mobie.viewer.SourceNameEncoder;

public class DevelopSourceNameBitEncoding
{
	public static void main( String[] args )
	{
		final UnsignedIntType unsignedIntType = new UnsignedIntType();
		unsignedIntType.set( 1024 );

		final long label = unsignedIntType.get();
		System.out.println(Long.toBinaryString((long)label));

		final long image = 10;
		final long l1 = image << 16;
		System.out.println(l1);
		System.out.println(Long.toBinaryString(l1));
		final long both = label + ( image << 16 );
		System.out.println(Long.toBinaryString(both));
		// image
		final long decodedImage = both >> 16;
		System.out.println(Long.toBinaryString( decodedImage ));
		System.out.println("image id: " + decodedImage);
		// label
		final long decodedLabel = both & 0x0000FFFF;
		System.out.println(Long.toBinaryString( decodedLabel ));
		System.out.println("label id: " + decodedLabel);

		System.out.printf( "----------------------\n" );

		SourceNameEncoder.addName( "hello" );
		SourceNameEncoder.addName( "world" );
		final UnsignedIntType labelIndex = new UnsignedIntType( 133 );
		SourceNameEncoder.encodeName( labelIndex, "hello" );
		System.out.println(labelIndex.get());
		System.out.println("name: " + SourceNameEncoder.getName( labelIndex ));
		System.out.println("value: " + SourceNameEncoder.getValue( labelIndex ));

	}
}
