import javax.crypto.SealedObject;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;
import java.io.PrintStream;

public class DESDecrypter implements Runnable {
	private long startkey, endkey;
	private SealedObject sldObj;
	private SecretKeySpec the_key;
	private	byte[] deskeyIN = new byte[8];
	private	byte[] deskeyOUT = new byte[8];
	private static int THREAD_COUNT = 0;
	private long encryptKey;
	private int thread_id;
	private boolean printStatus;

	/**
	* Constructor for the decrypter - makes a copy of the SealedObject given to it.
	* 
	* @param encryptKey the key to encrypt PLAINSTR with
	* @param startkey the key to start the brute force operation from
	* @param endkey the key to stop searching from
	*/
	public DESDecrypter (SealedObject sldObj, long startkey, long endkey, boolean printStatus) {
		if (/*cpyObj == null || */startkey < 0 || endkey < startkey) {
			throw new IllegalArgumentException("\"startkey\" > 0, and \"endkey\" >= \"startkey\"");
		}
		this.sldObj = sldObj;
		this.startkey = startkey;
		this.endkey = endkey;
		this.thread_id = THREAD_COUNT++;
		this.printStatus = printStatus;
	}

	/**
	* Implements the runnable interface.
	* 
	* Performs the brute force search and doesn't stop until it reaches
	* this.endkey
	*/
	public void run() {
		PrintStream p = null;
		if (this.printStatus) {
			p = new PrintStream(System.out);
		}

		// Get and store the current time -- for timing
		long runstart;
		runstart = System.currentTimeMillis();

		// Search for the right key
		for ( long i = this.startkey; i < this.endkey; i++ )
		{
			// Set the key and decipher the object
			this.setKey ( i );
			String decryptstr = this.decrypt();
			
			// Does the object contain the known plaintext
			if (( decryptstr != null ) && ( decryptstr.indexOf ( "Hopkins" ) != -1 ))
			{
				//  Remote printlns if running for time.
				if (this.printStatus) {
					p.println("Thread " + this.thread_id + " found decrypt key " + i + " producing message: " + decryptstr);
				}

				//p.printf("Thread #%d found decrypt key %d producing message: %s\n",
				//	this.thread_id, i , decryptstr);
				//System.out.println (  "Found decrypt key " + i + " producing message: " + decryptstr );
			}
			
			// Update progress every once in awhile.
			//  Remote printlns if running for time.
			if (printStatus && i % 100000 == 0 )
			{ 
				long elapsed = System.currentTimeMillis() - runstart;
				p.println("Thread " + this.thread_id + " Searched key number " + i + " at " + elapsed + " milliseconds.");

				//p.printf ( "Thread %d Searched key number %d at %d milliseconds.\n",
				//	this.thread_id, i, elapsed);
			}
		}
	}

	/**
	 * Decrypt the SealedObject - taken from SealedDES.java that was given.
	 * @return plaintext String or null if a decryption error
	 */
	private String decrypt () {
		try {
			return (String)this.sldObj.getObject(this.the_key);
		}
		catch ( Exception e ) {
			//      System.out.println("Failed to decrypt message. " + ". Exception: " + e.toString()  + ". Message: " + e.getMessage()) ; 
		}
		return null;
	}

	/**
	 * Set the key (convert from a long integer).
	 * @param theKey the long to use as a key for decryption
	 */
	private void setKey ( long theKey )	{
		try {
			// convert the integer to the 8 bytes required of keys
			deskeyIN[0] = (byte) (theKey        & 0xFF );
			deskeyIN[1] = (byte)((theKey >>  8) & 0xFF );
			deskeyIN[2] = (byte)((theKey >> 16) & 0xFF );
			deskeyIN[3] = (byte)((theKey >> 24) & 0xFF );
			deskeyIN[4] = (byte)((theKey >> 32) & 0xFF );
			deskeyIN[5] = (byte)((theKey >> 40) & 0xFF );
			deskeyIN[6] = (byte)((theKey >> 48) & 0xFF );

			// theKey should never be larger than 56-bits, so this should always be 0
			deskeyIN[7] = (byte)((theKey >> 56) & 0xFF );
			
			// turn the 56-bits into a proper 64-bit DES key
			makeDESKey(deskeyIN, deskeyOUT);
			
			// Create the specific key for DES
			this.the_key = new SecretKeySpec ( deskeyOUT, "DES" );
		}
		catch ( Exception e ) {
			System.out.println("Failed to assign key" +  theKey +
							   ". Exception: " + e.toString() + ". Message: " + e.getMessage()) ;
		}
	}

	/**
	 * Build a DES formatted key.
	 * Convert an array of 7 bytes into an array of 8 bytes.
	 * @param in byte array for input
	 * @param out byte array for output
	 */
	private static void makeDESKey(byte[] in, byte[] out) {
	    out[0] = (byte) ((in[0] >> 1) & 0xff);
	    out[1] = (byte) ((((in[0] & 0x01) << 6) | (((in[1] & 0xff)>>2) & 0xff)) & 0xff);
	    out[2] = (byte) ((((in[1] & 0x03) << 5) | (((in[2] & 0xff)>>3) & 0xff)) & 0xff);
	    out[3] = (byte) ((((in[2] & 0x07) << 4) | (((in[3] & 0xff)>>4) & 0xff)) & 0xff);
	    out[4] = (byte) ((((in[3] & 0x0F) << 3) | (((in[4] & 0xff)>>5) & 0xff)) & 0xff);
	    out[5] = (byte) ((((in[4] & 0x1F) << 2) | (((in[5] & 0xff)>>6) & 0xff)) & 0xff);
	    out[6] = (byte) ((((in[5] & 0x3F) << 1) | (((in[6] & 0xff)>>7) & 0xff)) & 0xff);
	    out[7] = (byte) (   in[6] & 0x7F);
			
	    for (int i = 0; i < 8; i++) {
	      out[i] = (byte) (out[i] << 1);
	    }
    }
}	