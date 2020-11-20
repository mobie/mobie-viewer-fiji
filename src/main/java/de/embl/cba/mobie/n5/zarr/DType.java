/**
 * Copyright (c) 2017, Stephan Saalfeld
 * All rights reserved.
 *
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
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.embl.cba.mobie.n5.zarr;

import com.google.gson.*;
import org.janelia.saalfeldlab.n5.*;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;

/**
 * Enumerates available zarr data types as defined at
 * https://docs.scipy.org/doc/numpy/reference/arrays.interface.html
 *
 * At this time, only primitive types are supported, no recursive structs.
 * This is compatible with the upcoming Zarr spec v3 core types plus extras.
 *
 * @author Stephan Saalfeld
 */
public class DType {

	private static final EnumMap< DataType, String > typestrs = new EnumMap<>( DataType.class);
	{
		typestrs.put( DataType.INT8, "|i1");
		typestrs.put( DataType.UINT8, "|u1");
		typestrs.put( DataType.INT16, ">i2");
		typestrs.put( DataType.UINT16, ">u2");
		typestrs.put( DataType.INT32, ">i4");
		typestrs.put( DataType.UINT32, ">u4");
		typestrs.put( DataType.INT64, ">i8");
		typestrs.put( DataType.UINT64, ">u8");
		typestrs.put( DataType.FLOAT32, ">f4");
		typestrs.put( DataType.FLOAT64, ">f8");
	}

	public static enum Primitive {

		BIT('t'),
		BOOLEAN('b'),
		INT('i'),
		UNSIGNED_INT('u'),
		FLOAT('f'),
		COMPLEX_FLOAT('c'),
		TIMEDELTA('m'),
		DATETIME('M'),
		OBJECT('O'),
		STRING('S'),
		UNICODE('U'),
		OTHER('V');

		private final char code;

		private Primitive(final char code) {

			this.code = code;
		}

		public char code() {

			return code;
		}

		public static Primitive fromCode(final char code) {

			for (final Primitive value : values())
				if (value.code() == code)
					return value;
			return null;
		}
	}

	protected String typestr;
	protected final ByteOrder order;

	/* According to https://docs.scipy.org/doc/numpy/reference/arrays.interface.html
	 * this should be the number of bytes per scalar except for BIT where it is the
	 * number of bits.  Also not sure what the quantifier means for strings, times,
	 * and objects
	 */
	protected final int nBytes;
	protected final int nBits;
	protected final ByteBlockFactory byteBlockFactory;
	protected final DataBlockFactory dataBlockFactory;


	/* the closest possible N5 DataType */
	protected final DataType dataType;

	public DType(final String typestr) {

		this.typestr = typestr;

		order = typestr.charAt(0) == '<' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
		final Primitive primitive = Primitive.fromCode(typestr.charAt(1));
		final int nB = Integer.parseInt(typestr.substring(2));

		switch (primitive) {
		case BIT:
			nBytes = 0;
			nBits = nB;
			dataBlockFactory = (blockSize, gridPosition, numElements) ->
					new ByteArrayDataBlock(blockSize, gridPosition, new byte[(numElements * nBits + 7) / 8]);
			byteBlockFactory = (blockSize, gridPosition, numElements) ->
					new ByteArrayDataBlock(blockSize, gridPosition, new byte[(numElements * nBits + 7) / 8]);
			break;
		case UNSIGNED_INT:
		case INT:
			nBytes = nB;
			nBits = 0;
			switch (nBytes) {
			case 1:
				dataBlockFactory = (blockSize, gridPosition, numElements) ->
						new ByteArrayDataBlock(blockSize, gridPosition, new byte[numElements]);
				break;
			case 2:
				dataBlockFactory = (blockSize, gridPosition, numElements) ->
						new ShortArrayDataBlock(blockSize, gridPosition, new short[numElements]);
				break;
			case 4:
				dataBlockFactory = (blockSize, gridPosition, numElements) ->
						new IntArrayDataBlock(blockSize, gridPosition, new int[numElements]);
				break;
			case 8:
				dataBlockFactory = (blockSize, gridPosition, numElements) ->
						new LongArrayDataBlock(blockSize, gridPosition, new long[numElements]);
				break;
			default: // because we do not know what else to do here
				dataBlockFactory = (blockSize, gridPosition, numElements) ->
						new ByteArrayDataBlock(blockSize, gridPosition, new byte[numElements * nBytes]);
			}
			byteBlockFactory = (blockSize, gridPosition, numElements) ->
					new ByteArrayDataBlock(blockSize, gridPosition, new byte[numElements * nBytes]);
			break;
		case FLOAT:
			nBytes = nB;
			nBits = 0;
			switch (nBytes) {
			case 4:
				dataBlockFactory = (blockSize, gridPosition, numElements) ->
						new FloatArrayDataBlock(blockSize, gridPosition, new float[numElements]);
				break;
			case 8:
				dataBlockFactory = (blockSize, gridPosition, numElements) ->
						new DoubleArrayDataBlock(blockSize, gridPosition, new double[numElements]);
				break;
			default: // because we do not know what else to do here
				dataBlockFactory = (blockSize, gridPosition, numElements) ->
						new ByteArrayDataBlock(blockSize, gridPosition, new byte[numElements * nBytes]);
			}
			byteBlockFactory = (blockSize, gridPosition, numElements) ->
					new ByteArrayDataBlock(blockSize, gridPosition, new byte[numElements * nBytes]);
			break;
		case COMPLEX_FLOAT:
			nBytes = nB;
			nBits = 0;
			switch (nBytes) {
			case 8: // this would support mapping onto an ImgLib2 ComplexFloatType
				dataBlockFactory = (blockSize, gridPosition, numElements) ->
						new FloatArrayDataBlock(blockSize, gridPosition, new float[numElements * 2]);
				break;
			case 16: // this would support mapping onto an ImgLib2 ComplexDoubleType
				dataBlockFactory = (blockSize, gridPosition, numElements) ->
						new DoubleArrayDataBlock(blockSize, gridPosition, new double[numElements * 2]);
				break;
			default: // because we do not know what else to do here
				dataBlockFactory = (blockSize, gridPosition, numElements) ->
						new ByteArrayDataBlock(blockSize, gridPosition, new byte[numElements * nBytes]);
			}
			byteBlockFactory = (blockSize, gridPosition, numElements) ->
					new ByteArrayDataBlock(blockSize, gridPosition, new byte[numElements * nBytes]);
			break;
//		case BOOLEAN:
//		case OBJECT:    // not sure about this
//		case OTHER:     // not sure about this
//		case STRING:    // not sure about this
//		case UNICODE:   // not sure about this
//		case TIMEDELTA: // not sure about this
//		case DATETIME:  // not sure about this
		default:
			nBytes = nB;
			nBits = 0;
			dataBlockFactory = (blockSize, gridPosition, numElements) ->
					new ByteArrayDataBlock(blockSize, gridPosition, new byte[numElements * nBytes]);
			byteBlockFactory = (blockSize, gridPosition, numElements) ->
					new ByteArrayDataBlock(blockSize, gridPosition, new byte[numElements * nBytes]);
		}

		dataType = getDataType(primitive, nBytes);
	}

	public DType( final DataType dataType, final int nPrimitives) {

		typestr = typestrs.get(dataType);
		order = ByteOrder.BIG_ENDIAN;
		nBits = 0;

		this.dataType = dataType;

		switch (dataType) {
		case INT16:
		case UINT16:
			nBytes = 2 * nPrimitives;
			dataBlockFactory = (blockSize, gridPosition, numElements) ->
					new ShortArrayDataBlock(blockSize, gridPosition, new short[numElements * nPrimitives]);
			break;
		case INT32:
		case UINT32:
			nBytes = 4 * nPrimitives;
			dataBlockFactory = (blockSize, gridPosition, numElements) ->
					new IntArrayDataBlock(blockSize, gridPosition, new int[numElements * nPrimitives]);
			break;
		case INT64:
		case UINT64:
			nBytes = 8 * nPrimitives;
			dataBlockFactory = (blockSize, gridPosition, numElements) ->
					new LongArrayDataBlock(blockSize, gridPosition, new long[numElements * nPrimitives]);
			break;
		case FLOAT32:
			nBytes = 4 * nPrimitives;
			dataBlockFactory = (blockSize, gridPosition, numElements) ->
					new FloatArrayDataBlock(blockSize, gridPosition, new float[numElements * nPrimitives]);
			break;
		case FLOAT64:
			nBytes = 8 * nPrimitives;
			dataBlockFactory = (blockSize, gridPosition, numElements) ->
					new DoubleArrayDataBlock(blockSize, gridPosition, new double[numElements * nPrimitives]);
			break;
//		case INT8:
//		case UINT8:
		default:
			nBytes = nPrimitives;
			dataBlockFactory = (blockSize, gridPosition, numElements) ->
					new ByteArrayDataBlock(blockSize, gridPosition, new byte[numElements * nPrimitives]);
		}
		byteBlockFactory = (blockSize, gridPosition, numElements) ->
				new ByteArrayDataBlock(blockSize, gridPosition, new byte[numElements * nBytes]);
	}

	public DType(final DataType dataType) {

		this(dataType, 1);
	}

	public DataType getDataType() {

		return dataType;
	}

	protected final static DataType getDataType(
			final Primitive primitive,
			final int nBytes) {

		switch (primitive) {
		case INT:
			switch (nBytes) {
			case 2:
				return DataType.INT16;
			case 4:
				return DataType.INT32;
			case 8:
				return DataType.INT64;
			default:
				return DataType.INT8; // including fallback for unknown quantifiers
			}
		case UNSIGNED_INT:
			switch (nBytes) {
			case 2:
				return DataType.UINT16;
			case 4:
				return DataType.UINT32;
			case 8:
				return DataType.UINT64;
			default:
				return DataType.UINT8; // including fallback for unknown quantifiers
			}
		case FLOAT:
			switch (nBytes) {
			case 4:
				return DataType.FLOAT32;
			case 8:
				return DataType.FLOAT64;
			default:
				return DataType.UINT8; // fallback
			}
		case COMPLEX_FLOAT:
			switch (nBytes) {
			case 8:
				return DataType.FLOAT32;
			case 16:
				return DataType.FLOAT64;
			default:
				return DataType.UINT8; // fallback
			}
		default:
			return DataType.UINT8; // fallback
		}
	}


	@Override
	public String toString() {

		return typestr;
	}

	/**
	 * Factory for {@link DataBlock DataBlocks}.
	 *
	 * @param blockSize
	 * @param gridPosition
	 * @param numElements not necessarily one element per block element
	 * @return
	 */
	public DataBlock<?> createDataBlock( final int[] blockSize, final long[] gridPosition, final int numElements) {

		return dataBlockFactory.createDataBlock(blockSize, gridPosition, numElements);
	}

	/**
	 * Factory for {@link ByteArrayDataBlock ByteArrayDataBlocks}.
	 *
	 * @param blockSize
	 * @param gridPosition
	 * @param numElements not necessarily one element per block element
	 * @return
	 */
	public ByteArrayDataBlock createByteBlock( final int[] blockSize, final long[] gridPosition, final int numElements) {

		return byteBlockFactory.createByteBlock(blockSize, gridPosition, numElements);
	}

	/**
	 * Factory for {@link DataBlock DataBlocks} with one data element for each
	 * block element (e.g. pixel image).
	 *
	 * @param blockSize
	 * @param gridPosition
	 * @return
	 */
	public DataBlock<?> createDataBlock( final int[] blockSize, final long[] gridPosition) {

		return dataBlockFactory.createDataBlock(blockSize, gridPosition, DataBlock.getNumElements(blockSize));
	}

	/**
	 * Factory for {@link ByteArrayDataBlock ByteArrayDataBlocks}.
	 *
	 * @param blockSize
	 * @param gridPosition
	 * @return
	 */
	public ByteArrayDataBlock createByteBlock( final int[] blockSize, final long[] gridPosition) {

		return byteBlockFactory.createByteBlock(blockSize, gridPosition, DataBlock.getNumElements(blockSize));
	}

	private static interface DataBlockFactory {

		public DataBlock<?> createDataBlock( final int[] blockSize, final long[] gridPosition, final int numElements );
	}

	private static interface ByteBlockFactory {

		public ByteArrayDataBlock createByteBlock( final int[] blockSize, final long[] gridPosition, final int numElements );
	}

	static public class JsonAdapter implements JsonDeserializer<DType>, JsonSerializer<DType> {

		@Override
		public DType deserialize(
				final JsonElement json,
				final Type typeOfT,
				final JsonDeserializationContext context) throws JsonParseException {

			return new DType(json.getAsString());
		}

		@Override
		public JsonElement serialize(
				final DType src,
				final Type typeOfSrc,
				final JsonSerializationContext context) {

			return new JsonPrimitive(src.toString());
		}
	}

	public ByteOrder getOrder() {

		return order;
	}

	/**
	 * Returns the number of bytes of this type.  If the type counts the number
	 * of bits, this method returns 0.
	 *
	 * @return number of bytes
	 */
	public int getNBytes() {

		return nBytes;
	}

	/**
	 * Returns the number of bits of this type.  If the type counts the number
	 * of bytes, this method returns 0;
	 * @return
	 */
	public int getNBits() {

		return nBits;
	}

	public byte[] createFillBytes(final String fill_value) {

		final byte[] fillBytes = new byte[nBytes];
		final ByteBuffer fillBuffer = ByteBuffer.wrap(fillBytes);
		fillBuffer.order(order);

		if (fill_value.equals("NaN")) {
			if (nBytes == 8) {
				fillBuffer.putDouble( Double.NaN);
			} else if (nBytes == 4) {
				fillBuffer.putFloat( Float.NaN);
			}
		} else if (fill_value.equals("Infinity")) {
			if (nBytes == 8) {
				fillBuffer.putDouble( Double.POSITIVE_INFINITY);
			} else if (nBytes == 4) {
				fillBuffer.putFloat( Float.POSITIVE_INFINITY);
			}
		} else if (fill_value.equals("-Infinity")) {
			if (nBytes == 8) {
				fillBuffer.putDouble( Double.NEGATIVE_INFINITY);
			} else if (nBytes == 4) {
				fillBuffer.putFloat( Float.NEGATIVE_INFINITY);
			}
		} else {
			try {
				switch (dataType) {
				case INT8:
					fillBytes[0] = Byte.parseByte(fill_value);
					break;
				case UINT8:
					fillBytes[0] = (byte)(0xff & Integer.parseInt(fill_value));
					break;
				case INT16:
					fillBuffer.putShort( Short.parseShort(fill_value));
					break;
				case UINT16:
					fillBuffer.putShort((short)(0xffff & Integer.parseInt(fill_value)));
					break;
				case INT32:
					fillBuffer.putInt( Integer.parseInt(fill_value));
					break;
				case UINT32:
					fillBuffer.putInt( Integer.parseUnsignedInt(fill_value));
					break;
				case INT64:
					fillBuffer.putLong( Long.parseLong(fill_value));
					break;
				case UINT64:
					fillBuffer.putLong( Long.parseUnsignedLong(fill_value));
					break;
				case FLOAT32:
					fillBuffer.putFloat( Float.parseFloat(fill_value));
					break;
				case FLOAT64:
					fillBuffer.putDouble( Double.parseDouble(fill_value));
					break;
				}
			} catch (final NumberFormatException e) {}
		}
		return fillBytes;
	}
}