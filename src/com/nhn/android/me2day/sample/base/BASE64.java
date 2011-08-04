package com.nhn.android.me2day.sample.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
/*
 * @author Jang-Ho Hwang, rath@xrath.com
 * @version 1.0.000, 2002/06/04
 */
public class BASE64
{
	static final char[] MAP = 
	{
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 
		'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 
		'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 
		'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 
		'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 
		'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 
		'w', 'x', 'y', 'z', '0', '1', '2', '3', 
		'4', '5', '6', '7', '8', '9', '+', '/',
	};

	static final int[] REVERSE_MAP = 
	{
		 0, // Padding for human readable index access
		 0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0, 62,  0,  0,  0, 63, 52,
		53, 54, 55, 56, 57, 58, 59, 60,
		61,  0,  0,  0,  0,  0,  0,  0, 
		 0,  1,  2,  3,  4,  5,  6,  7, 
		 8,  9, 10, 11, 12, 13, 14, 15,
		16, 17, 18, 19, 20, 21, 22, 23, 
		24, 25,  0,  0,  0,  0,  0,  0, 
		26, 27, 28, 29, 30, 31, 32, 33, 
		34, 35, 36, 37, 38, 39, 40, 41, 
		42, 43, 44, 45, 46, 47, 48, 49, 
		50, 51, 
	};

	private boolean hasCRLF = true;

	public BASE64()
	{
		this( true );
	}

	public BASE64( boolean hasCRLF )
	{
		setCRLF( hasCRLF );
	}

	public void setCRLF( boolean hasCRLF )
	{
		this.hasCRLF = hasCRLF;
	}

	public boolean hasCRLF()
	{
		return this.hasCRLF;
	}

	public byte[] decode( String str )
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream(str.length());
		try
		{
			for(int i=0, len=str.length(); i<len; i++)
			{
				char c = str.charAt(i);
				if( c=='\r' || c=='\n' ) continue;
				int v0 = REVERSE_MAP[ str.charAt(i++) ];			
				int v1 = REVERSE_MAP[ str.charAt(i++) ];
				int v2 = REVERSE_MAP[ str.charAt(i++) ];
				int v3 = REVERSE_MAP[ str.charAt(i) ];
				int v = (v0 << 18) | (v1 << 12) | (v2 << 6) | (v3 << 0);
				bos.write( (v >> 16) & 0xff );
				bos.write( (v >>  8) & 0xff );
				bos.write( (v >>  0) & 0xff );
			}
		}
		catch( IndexOutOfBoundsException e ) {}
		return bos.toByteArray();
	}

	public String decodeAsString( String str )
	{
		return new String( decode(str) );
	}

	public String encode( String str )
	{
		return encode( str.getBytes() );
	}

	public String encode( byte[] b )
	{
		StringBuffer sb = new StringBuffer(b.length);
		int padCount = 0;
		ByteArrayInputStream bis = new ByteArrayInputStream( b );
		for(int i=0; i<b.length; i+=3)
		{
			int v0 = bis.read();
			int v1 = bis.read();
			int v2 = bis.read();
			if( v1==-1 ) { v1 = 0; padCount = 2; }
			else
			if( v2==-1 ) { v2 = 0; padCount = 1; }	
			int v = 
				((v0<<16) & 0x00FF0000) | 
				((v1<< 8) & 0x0000FF00) |
				((v2<< 0) & 0x000000FF);
			sb.append( MAP[(v>>18) & 0x3f] );
			sb.append( MAP[(v>>12) & 0x3f] );
			sb.append( MAP[(v>> 6) & 0x3f] );
			sb.append( MAP[(v>> 0) & 0x3f] );

			if( ((i+3)%57)==0 && hasCRLF )
				sb.append( '\n' );
		}
		for(int i=padCount, last=sb.length(); i>0; i--)
			sb.setCharAt( last-i, '=' );
		if( hasCRLF )
			sb.append( '\n' );
		return sb.toString();
	}
}
