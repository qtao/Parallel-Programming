import java.util.Random;
import java.util.ArrayList;
import javax.crypto.SealedObject;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;

public class ParallelSealedDES {
	public static final String PLAINSTR = "Johns Hopkins afraid of the big bad wolf?";

	public static void main(String[] args) throws InterruptedException {
		boolean printStatus = true;
		if ( args.length < 2 ) {
			System.out.println ("Usage: java ParallelSealedDES #threads key_size_in_bits [exclude status]");
			return;
		}

		if (args.length > 2) {
			printStatus = args[2].isEmpty() ? true : false;
		}
		int num_threads = Integer.parseInt(args[0]);
		long keybits = Long.parseLong(args[1]);

		long maxkey = ~(0L);
		maxkey = maxkey >>> (64 - keybits);

		// Get a number between 0 and 2^64 - 1
		Random generator = new Random ();
		long key =  generator.nextLong();
		
		// Mask off the high bits so we get a short key
		key = key & maxkey;
		if (printStatus) {
			System.out.format("Generated secret key %d\n", key);
		}

		long runstart = System.currentTimeMillis();

		Thread[] threads = new Thread[num_threads];
		DESDecrypter[] decrypters = new DESDecrypter[num_threads];

		long interval = (long) Math.ceil( (double) maxkey / num_threads);
		for (int i = 0; i < num_threads; i++) {
			long startkey = interval * i;
			long endkey = startkey + interval;
			if (endkey > maxkey) endkey = maxkey;
			DESDecrypter d = new DESDecrypter(encrypt(key), startkey, endkey, printStatus);
			Thread t = new Thread( d );
			threads[i] = t;
			decrypters[i] = d;
		}
		long startupTime = System.currentTimeMillis() - runstart;

		for(Thread t : threads) {
			t.start();
		}

		for(Thread t : threads) {
			t.join();
		}

		if (printStatus) {
			System.out.printf("Startup elapsed time: ~%d milliseconds\n", startupTime);
		} else {
			System.out.printf("%d,", startupTime);
		}



		long elapsed = System.currentTimeMillis() - runstart;
		if (printStatus) {
			System.out.printf("Final elapsed time: %d[Brute Force a DES Key]\n", elapsed);
		} else {
			System.out.printf("%d\n", elapsed);
		}
		
	}

	/**
	 * Encrypts PLAINSTR with the given key.
	 *
	 * @param encryptKey the key to encrypt PLAINSTR with
	 */
	private static SealedObject encrypt(long encryptKey) {
		byte[] deskeyIN = new byte[8];
		byte[] deskeyOUT = new byte[8];
		SecretKeySpec the_key = setKey(encryptKey, deskeyIN, deskeyOUT);
		Cipher des_cipher = null;
		try {
			des_cipher = Cipher.getInstance("DES");
		} 
		catch ( Exception e ) {
			System.out.println("Failed to create cipher.  Exception: " + e.toString() +
							   " Message: " + e.getMessage()) ; 
		}

		try {
			des_cipher.init ( Cipher.ENCRYPT_MODE, the_key );
			return new SealedObject( PLAINSTR, des_cipher );
		}
		catch ( Exception e ) {
			System.out.println("Failed to encrypt message. " + PLAINSTR +
							   ". Exception: " + e.toString() + ". Message: " + e.getMessage()) ; 
		}
		return null;
	}

	/**
	 * Set the key (convert from a long integer).
	 * @param theKey the long to use as a key for decryption
	 */
	private static SecretKeySpec setKey ( long theKey, byte[] deskeyIN, byte[] deskeyOUT )	{
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
			return new SecretKeySpec ( deskeyOUT, "DES" );
		}
		catch ( Exception e ) {
			System.out.println("Failed to assign key" +  theKey +
							   ". Exception: " + e.toString() + ". Message: " + e.getMessage()) ;
		}
		return null;
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