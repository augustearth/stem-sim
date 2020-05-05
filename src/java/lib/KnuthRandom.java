/*
 * KnuthRandom.java
 */

package lib;


/*
 * This is modified from the algorithm given in Numerical Recipes in C.
 * It is their algorithm ran3, which was based on Knuth's subtractive
 * method on pages 171-172 of his volume II (NOT section 3.2-3.3 as is
 * mentioned in Numerical Recipes).
 *
 * As far as I can figure out, this returns a value in [0, 1).
 *
 * I split the function into 2 functions - a seeder and a
 * generator. The generator DOES NOT check to see if the seeder has been
 * called. I chose to do this as it makes the generator faster by one if 
 * statement per call (which adds up over a million calls). Of course 
 * it also leaves a potential hole. There is no easy way to cause the 
 * generator to dump core (i.e. by initializing an array index to something
 * ridiculous) if it has not been seeded. The contents of ma will be
 * unpredictable & who knows what behavior you'll get from the generator.
 * (Later I found that Knuth's original was also not a single function.)
 *
 * There was no need to pass a pointer to the seed value - the algorithm in 
 * Numerical Recipes sets it to 1 after initialization. This was done because
 * their function was too literally translated from Knuth's Fortran, and 
 * Fortran functions must set the variable that corresponds to their name
 * to some value. In C this is of course unnecessary since our function 
 * returns void.
 *
 * I removed the definition of MZ (which was just zero).
 *
 * The variable iff is also gone, since it was used to tell if the seeder
 * had already been called - now we don't check for that.
 *
 * Calling the seeder with a value < 0 will cause it to seed itself from
 * the system clock. I hope Knuth got this right because the system clock
 * is currently returning values that are bigger than MSEED, which means
 * that mj (and hence ma[55]) will be set to a negative value in the seeder
 * if no seed is supplied. Actually the Numerical Recipes people seem to
 * have screwed up on this - Knuth specifies that the seed should be
 * in the range 0 to MBIG - 1, and sets ma[55] to that value. These people
 * do
 *
 *  mj = MSEED - seed;
 *  mj %= MBIG;
 *  ma[55] = mj;
 *
 * And as a result, if seed > MSEED, ma[55] will be negative, since the
 * modulus function will also return something < 0. This, I would say, is
 * a bug. The whole of this translation seems awkward, these people don't
 * appear to be good C programmers. I have taken Knuth's Fortran over
 * the N.R. C for this small section, and I have added a check to ensure
 * that the seed is actually < MBIG, as Knuth wants.
 *
 * The seeder returns the value of the seed used.
 *
 * The N.R. people "warm up" their generator 4 times, whilst Knuth
 * does it 3 times. I followed Knuth.
 *
 * The #define of FAC looks like it could be taken out and the return
 * statement replaced with return mj / MBIG, but in fact this would slow
 * things down - you'd do a long division every time. Instead we let the
 * compiler (hopefully) do it once and thereafter we do a long multiplication.
 *
 * Another thing that looks like it could be improved is the indexing of
 * the ma array. The zeroeth value isn't used at all. Typical Fortran
 * stuff. I was going to change all this, but the loop index (running from
 * 1 to 54) is at one point used in a calculation, so I decided not to worry.
 *
 * There is a function dump_random_state() which dumps out the state of the
 * random number generator into a FILE *. The complement function 
 * restore_random_state() will read in a state from a FILE * and set up
 * the generator's state so that a run may be resumed. This stuff will 
 * only be compiled if you #define DUMPS.
 *
 * I took the static variables outside any function so that they could
 * be accessed by all functions in this file.
 *
 * Also below, included if you define CATCH_OTHER_GENERATORS, are function
 * stubs that will catch calls to other known random number generators.
 * This is useful if you want to dump and restore state and you are also
 * going to be linking with code that you didn't write. In order to be
 * able to re-run something, all the random numbers must be the same and
 * there is no way to guarantee this if people use drand48() etc. I know
 * you can re-seed that too, but it's a hassle to use several generators.
 * Worse, when you want to restart a run that did 100,000 calls to drand48(),
 * you need to seed it AND THEN call it 100,000 times to get it into the 
 * state it was in before you interrupted the run. When you write your own
 * generator, it is simple to write a dumping function that does not require
 * re-calling when you restart.
 *
 * This code appears to be efficient. When I replaced drand48() calls with calls
 * to the following, my programs ran more than twice as quickly! Go figure.
 * I would like to hear from anyone who runs any randomness certification
 * tests on this code.
 *
 * Terry Jones (terry@santafe.edu)
 * July 3, 1992.
 *
 * Ported to Java by Derek Smith (dsmith@santafe.edu)
 * March, 2000.
 *
 */

public class KnuthRandom {
	
	private long MBIG=1000000000;
	private long MSEED=161803398;
	private double FAC=1.0/MBIG;
	
	private int inext;
	private int inextp;
	private long[] ma = new long[56];
	
	private long _seed;
	
	public long seedRandom (long seed) {
		long mj;
		long mk;
		int i;
		int k;
		
		if (seed < 0){
			seed = System.currentTimeMillis() % MBIG;
		}
		
		if (seed >= MBIG){
			System.err.println("Seed value too big (> " + MBIG + ") in knuth_srand().");
			System.exit(1);
		}
		_seed = seed;
		
		ma[55] = mj = seed;
		mk = 1;
		
		for (i = 1; i <= 54; i++){
			int ii = (21 * i) % 55;
			ma[ii] = mk;
			mk = mj - mk;
			if (mk < 0){
				mk += MBIG;
			}
			mj = ma[ii];
		}
		
		for (k = 0; k < 4; k++){
			for (i = 1; i <= 55; i++){
				ma[i] -= ma[1 + (i + 30) % 55];
				if (ma[i] < 0){
					ma[i] += MBIG;
				}
			}
		}
		
		inext = 0;
		inextp = 31;
		
		return seed;
	}
	
	public long getSeed() {
		return _seed;
	}
	
	public double randomDouble ()
	//real int range [0-1)
	{
		long mj;
		
		if (++inext == 56){
			inext = 1;
		}
		
		if (++inextp == 56){
			inextp = 1;
		}
		
		mj = ma[inext] - ma[inextp];
		
		if (mj < 0){
			mj += MBIG;
		}
		
		ma[inext] = mj;
		return mj * FAC;
	}
	
	public int randomInt (int i) {
		return (int)(Math.floor(i*randomDouble()));
	}
	
	public long randomInt (long i) {
		return (long)(Math.floor(i*randomDouble()));
	}   
	
	// jte
	// 'probability' is the average rate of counts per unit time for an assumed Poisson random var
	public double getWaitingTimePoisson( double probability )  {
		return Math.log(1.0-randomDouble()) / Math.log(1.0-probability);
	}
}
