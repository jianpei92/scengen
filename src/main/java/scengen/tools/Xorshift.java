package main.java.scengen.tools;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;


/** <p>Xorshift provides a fast and thread-safe implementation of George Marsaglia's Xorshift
 * peudo-random number generator (see <a href="http://www.jstatsoft.org/v08/i14/paper/">&ldquo;Xorshift RNGs&rdquo;</a>,
 * <i>Journal of Statistical Software</i>, 8:1&minus;6, 2003) with a cycle length of
 * 2<sup>1024</sup>&nbsp;&minus;&nbsp;1.</p>
 * 
 * <p> The implementation is identical to <a href="http://dsiutils.di.unimi.it/docs/it/unimi/dsi/util/XorShiftStarRandom.html">XorShiftStarRandom1024</a>, but synchronizes calls to {@link #nextLong(long)} and {@link #setSeed(long)} to ensure thread-safety.</p>
 * 
 * <p> The following information is provided by the original author:</p> <p>"The quality of this generator is high: for instance, it performs better than <samp>WELL1024a</samp> 
 * or <samp>MT19937</samp> in suites like  
 * <a href="http://www.iro.umontreal.ca/~simardr/testu01/tu01.html">TestU01</a> and
 * <a href="http://www.phy.duke.edu/~rgb/General/dieharder.php">Dieharder</a>. More precisely, over 100 runs of the BigCrush test suite
 * starting from equispaced points of the state space:
 * <ul>
 * <li>this generator and its reverse fail 351 tests (the only test failed at all points is MatrixRank);
 * <li>the best generator of this family suggested by William H. Press in <em>Numerical Recipes</em>, third edition, 
 * Cambridge University Press, 2007, and its reverse fail 463 tests 
 * (the tests failed at all points are MatrixRank and BirthdaySpacing);
 * <li><samp>WELL1024a</samp> and its reverse fail 882 tests (the only test failed at all points is MatrixRank);
 * <li><samp>MT19937</samp> and its reverse fail 516 tests (the only test failed at all points is LinearComp);
 * <li>{@link Random} and its reverse fail 13564 tests of all kind."
 * </ul>
 * </p>
 * <p>The class extends {@link Random}, overriding the {@link Random#next(int)} method. 
 * 
 * @author Sebastiano Vigna
 * @author Nils Loehndorf
 */
public class Xorshift extends Random {
	private static final long serialVersionUID = 1L;

	/** 2<sup>-53</sup>. */
	private static final double NORM_53 = 1. / ( 1L << 53 );
	/** 2<sup>-24</sup>. */
	private static final double NORM_24 = 1. / ( 1L << 24 );

	/** The internal state of the algorithm. */
	private long[] s;
	private int p;
	
	/** Creates a new generator seeded using {@link #randomSeed()}. */
	public Xorshift() {
		this( randomSeed() );
	}

	/** Creates a new generator using a given seed.
	 * 
	 * @param seed a nonzero seed for the generator (if zero, the generator will be seeded with -1).
	 */
	public Xorshift( final long seed ) {
		super( seed );
	}

	@Override
	protected int next( int bits ) {
		return (int)( nextLong() >>> 64 - bits );
	}
	
	@Override
	public synchronized long nextLong() {
		long s0 = s[ p ];
		long s1 = s[ p = ( p + 1 ) & 15 ];
		s1 ^= s1 << 1;
		return 1181783497276652981L * ( s[ p ] = s1 ^ s0 ^ ( s1 >>> 13 ) ^ ( s0 >>> 7 ) );
	}

	@Override
	public int nextInt() {
		return (int)nextLong();
	}
	
	@Override
	public int nextInt( final int n ) {
		return (int)nextLong( n );
	}
	
	/** Returns a pseudorandom uniformly distributed {@code long} value
     * between 0 (inclusive) and the specified value (exclusive), drawn from
     * this random number generator's sequence. The algorithm used to generate
     * the value guarantees that the result is uniform, provided that the
     * sequence of 64-bit values produced by this generator is. 
     * 
     * @param n the positive bound on the random number to be returned.
     * @return the next pseudorandom {@code long} value between {@code 0} (inclusive) and {@code n} (exclusive).
     */
	public long nextLong( final long n ) {
        if ( n <= 0 ) throw new IllegalArgumentException();
		// No special provision for n power of two: all our bits are good.
		for(;;) {
			final long bits = nextLong() >>> 1;
			final long value = bits % n;
			if ( bits - value + ( n - 1 ) >= 0 ) return value;
		}
	}
	
	@Override
	 public double nextDouble() {
		return ( nextLong() >>> 11 ) * NORM_53;
	}
	
	@Override
	public float nextFloat() {
		return (float)( ( nextLong() >>> 40 ) * NORM_24 );
	}

	@Override
	public boolean nextBoolean() {
		return ( nextLong() & 1 ) != 0;
	}
	
	@Override
	public void nextBytes( final byte[] bytes ) {
		int i = bytes.length, n = 0;
		while( i != 0 ) {
			n = Math.min( i, 8 );
			for ( long bits = nextLong(); n-- != 0; bits >>= 8 ) bytes[ --i ] = (byte)bits;
		}
	}
	
	/** Returns a random seed generated by taking a unique increasing long, adding
	 * {@link System#nanoTime()} and scrambling the result using the finalisation step of Austin
	 * Appleby's <a href="http://sites.google.com/site/murmurhash/">MurmurHash3</a>.
	 * 
	 * @return a reasonably good random seed. 
	 */
	private static final AtomicLong seedUniquifier = new AtomicLong();
	public static long randomSeed() {
		long seed = seedUniquifier.incrementAndGet() + System.nanoTime();

		seed ^= seed >>> 33;
		seed *= 0xff51afd7ed558ccdL;
		seed ^= seed >>> 33;
		seed *= 0xc4ceb9fe1a85ec53L;
		seed ^= seed >>> 33;

		return seed;
	}

	/** Sets the seed of this generator.
	 * 
	 * @param seed a nonzero seed for the generator (if zero, the generator will be seeded with -1).
	 */
	@Override
	public synchronized void setSeed( final long seed ) {
		if ( s == null ) s = new long[ 16 ];
		for( int i = s.length; i-- != 0; ) s[ i ] = seed == 0 ? -1 : seed;
		for( int i = s.length * 4; i-- != 0; ) nextLong(); // Warmup.
	}
}