package a51;

/**
 * A full (reference-style) implementation of A5/1 in Java.
 * - Registers implemented as Boolean[] with index 0 = LSB, index Len-1 = MSB.
 * - Uses standard taps and clocking bits.
 *
 * Note: This code is intended for educational / lab use.
 */
public class A51Cipher {
    // Register lengths
    private static final int R1_LEN = 19;
    private static final int R2_LEN = 22;
    private static final int R3_LEN = 23;

    // Clocking bit indices (0 = LSB)
    private static final int R1_CLOCK = 8;
    private static final int R2_CLOCK = 10;
    private static final int R3_CLOCK = 10;

    // Tap positions for feedback (indices, 0-based LSB)
    private static final int[] R1_TAPS = {13, 16, 17, 18};
    private static final int[] R2_TAPS = {20, 21};
    private static final int[] R3_TAPS = {7, 20, 21, 22};

    private final boolean[] r1 = new boolean[R1_LEN];
    private final boolean[] r2 = new boolean[R2_LEN];
    private final boolean[] r3 = new boolean[R3_LEN];

    public A51Cipher() {
        // registers start at zero by default
    }

    // Utility: clock a single register (shift right; new LSB = feedback)
    private void clockRegister(boolean[] reg, int[] taps) {
        boolean fb = false;
        for (int t : taps) fb ^= reg[t];
        // shift right: MSB becomes previous [len-2], ... index 1 becomes index0
        for (int i = reg.length - 1; i >= 1; i--) reg[i] = reg[i - 1];
        reg[0] = fb;
    }

    // Compute majority of three bits
    private boolean majority(boolean a, boolean b, boolean c) {
        int sum = (a?1:0) + (b?1:0) + (c?1:0);
        return sum >= 2;
    }

    // Clock irregularly according to majority rule
    private void clockIrregular() {
        boolean cb1 = r1[R1_CLOCK];
        boolean cb2 = r2[R2_CLOCK];
        boolean cb3 = r3[R3_CLOCK];
        boolean maj = majority(cb1, cb2, cb3);
        if (cb1 == maj) clockRegister(r1, R1_TAPS);
        if (cb2 == maj) clockRegister(r2, R2_TAPS);
        if (cb3 == maj) clockRegister(r3, R3_TAPS);
    }

    // Clock all registers (used during key/frame mixing)
    private void clockAll() {
        clockRegister(r1, R1_TAPS);
        clockRegister(r2, R2_TAPS);
        clockRegister(r3, R3_TAPS);
    }

    // Output bit is XOR of MSBs
    private boolean outputBit() {
        return r1[R1_LEN - 1] ^ r2[R2_LEN - 1] ^ r3[R3_LEN - 1];
    }

    // Helper: set registers to zero
    private void clearRegisters() {
        for (int i = 0; i < r1.length; i++) r1[i] = false;
        for (int i = 0; i < r2.length; i++) r2[i] = false;
        for (int i = 0; i < r3.length; i++) r3[i] = false;
    }

    /**
     * Initialize with 64-bit key (given as long, use only low-order 64 bits)
     * and 22-bit frame number (int, use low-order 22 bits).
     * @param key64
     * @param frame22
     */
    public void init(long key64, int frame22) {
        clearRegisters();

        // mix key: for i=0..63: R[0] ^= key_bit_i; then clock all registers
        for (int i = 0; i < 64; i++) {
            boolean kbit = ((key64 >>> i) & 1L) != 0;
            r1[0] ^= kbit;
            r2[0] ^= kbit;
            r3[0] ^= kbit;
            clockAll();
        }

        // mix frame number (22 bits)
        for (int i = 0; i < 22; i++) {
            boolean fbit = ((frame22 >>> i) & 1) != 0;
            r1[0] ^= fbit;
            r2[0] ^= fbit;
            r3[0] ^= fbit;
            clockAll();
        }

        // warm-up: 100 clock cycles with irregular clocking, discard output
        for (int i = 0; i < 100; i++) clockIrregular();
    }

    /**
     * Generate keystream of n bits and return as boolean[] (true = 1).
     * @param n
     * @return 
     */
    public boolean[] getKeystreamBits(int n) {
        boolean[] ks = new boolean[n];
        for (int i = 0; i < n; i++) {
            clockIrregular();
            ks[i] = outputBit();
        }
        return ks;
    }

    /**
     * Generate keystream as byte array (8 bits per byte), big-endian inside byte:
     * first generated bit goes to most-significant bit of first byte.
     * @param bytesCount
     * @return 
     */
    public byte[] getKeystreamBytes(int bytesCount) {
        boolean[] bits = getKeystreamBits(bytesCount * 8);
        byte[] out = new byte[bytesCount];
        for (int b = 0; b < bytesCount; b++) {
            int val = 0;
            for (int bit = 0; bit < 8; bit++) {
                if (bits[b * 8 + bit]) val |= (1 << (7 - bit));
            }
            out[b] = (byte)(val & 0xFF);
        }
        return out;
    }

    // XOR data with keystream bytes (data length defines how many keystream bytes used)
    public byte[] encryptBytes(byte[] data) {
        byte[] ks = getKeystreamBytes(data.length);
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) out[i] = (byte)(data[i] ^ ks[i]);
        return out;
    }

    // Convenience: hex string -> bytes
    public static byte[] hexToBytes(String hex) {
        hex = hex.replaceAll("\\s",""); // remove spaces
        int len = hex.length();
        if (len % 2 != 0) hex = "0" + hex;
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(2*i), 16);
            int lo = Character.digit(hex.charAt(2*i+1), 16);
            out[i] = (byte)((hi<<4) | lo);
        }
        return out;
    }

    public static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02X", b & 0xFF));
        return sb.toString();
    }
}
