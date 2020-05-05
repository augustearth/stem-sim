/*
 * Probability.java
 *
 * @author Dennis Chao
 * @version
 * @created July 2001
 * Some portions taken from Derek Smith's code
 * Some of the code was taken from the Colt library (v 1.0.3) available at:
 *   http://hoschek.home.cern.ch/hoschek/colt/
 *   (Written by Wolfgang Hoschek. Check the Colt home page for more info.
 *    Copyright 1999 CERN - European Organization for Nuclear Research.)
 * And some of the Colt code is based on routines in Numerical Recipes in C
 */
package lib;

import java.lang.Double;
import java.util.*;

public class Probability {
  protected static final double MEAN_MAX = Integer.MAX_VALUE; // for all means larger than that, we don't try to compute a poisson deviation, but return the mean.
  protected static final double SWITCH_MEAN = 12.0; // switch from method A to method B

  public static long factorial (int n) {
    if (n < 0)
      throw new Error ("factorial called with negative number " + n);
    long product=1;
    for (int i=1; i<=n; i++)
      product *= i;
    return product;
  }

  public static long factorial (float n) {
    throw new Error ("Fractional factorials have a mathematical definition,but are not implemented yet.");
  }

  // exptFalling - (m!)/(k!)
  public static long exptFalling (int m, int k) {
    long product=1;
    for (int i=0; i<k; i++)
      product *= m-i;
    return product;
  }
   
  // Combination - computes C(n,k)
  public static double Combination(int n, int k) {
    double result = 1.0;
    for (int i=n; i>n-k; i--)
      result *= i;
    for (int i=k; i>1; i--)
      result /= i;
    return result;
  }

  // Poisson - compute (lambda^x * e^-lambda) / x!
  // note that the Poisson is an approximation to the Binomial(lambda/n, x)
  public static double Poisson(double lambda, int x) {
    double p = 1.0;
    for (int i=1; i<=x; i++)
      p *= lambda/i;
    return p*Math.exp(-lambda);
  }

  /**
   * Returns the value ln(Gamma(xx) for xx > 0.  Full accuracy is obtained for 
   * xx > 1. For 0 < xx < 1. the reflection formula (6.1.4) can be used first.
   * (Adapted from Numerical Recipes in C)
   * this routine was taken from colt's SlowPoisson.java
   */
  protected static final double[] cof = { // for method logGamma() 
    76.18009172947146,-86.50532032941677,
    24.01409824083091, -1.231739572450155,
    0.1208650973866179e-2, -0.5395239384953e-5};

  public static double logGamma(double xx) {
    double x = xx - 1.0;
    double tmp = x + 5.5;
    tmp -= (x + 0.5) * Math.log(tmp);
    double ser = 1.000000000190015;

    double[] coeff = cof;
    for (int j = 0; j <= 5; j++ ) {
      x++;
      ser += coeff[j]/x;
    }
    return -tmp + Math.log(2.5066282746310005*ser);
  }

  // RandomFromPoisson - returns a random int drawn from a Poisson distribution
  // this routine was adapted from colt's SlowPoisson.java nextInt method
  public static long RandomFromPoisson(double mean, KnuthRandom rand) {
    /* 
     * Adapted from "Numerical Recipes in C".
     */
    double xm = mean;

    if (xm == -1.0 ) return 0; // not defined
    if (xm < SWITCH_MEAN ) {
      double g = Math.exp(-xm);
      long poisson = -1;
      double product = 1;
      do {
        poisson++;
        product *= rand.randomDouble();
      } while ( product >= g );
      // bug in CLHEP 1.4.0: was "} while ( product > g );"
      return poisson;
    } else if (xm < MEAN_MAX ) {
      double t;
      double em;
      double sq = Math.sqrt(2.0*xm);
      double alxm = Math.log(xm);
      double g = xm*alxm - logGamma(xm + 1.0);
      
      do { 
        double y;
        do {
          y = Math.tan(Math.PI*rand.randomDouble());
          em = sq*y + xm;
        } while (em < 0.0);
        em = (double) (long)(em); // faster than em = Math.floor(em); (em>=0.0)
        t = 0.9*(1.0 + y*y)* Math.exp(em*alxm - logGamma(em + 1.0) - g);
      } while (rand.randomDouble() > t);
      return (long) em;
    }
    else { // mean is too large
      System.err.println("ERROR: RandomFromPoisson: mean is too large, returning the mean");
      return (long) xm;
    }
  }

  // Binomial - computes binomial coefficient
  public static double Binomial(int n, double p, int i) {
    return Combination(n,i) * Math.pow(p,i) * Math.pow(1.0-p, n-i);
  }
  
  // RandomFromNormal - returns a random int drawn from a Normal dist
  // (1/(sigma*sqrt(2pi))) * e^-(((x-mu)^2)/(2sigma^2))
  public static int RandomFromNormal(long n, double p, KnuthRandom rand) {
    double r = rand.randomDouble();
    int k=0;
    double term = Math.pow(1-p, n);
    double sum = term;
    if (term==0.0)
      throw new Error("Binomial " + n + "," + p + " can not be computed");
    while ((sum<=r) && (term>0.0) && (k<n)) {
      k++;
      term *= (n-k+1)*p/(k*(1.0-p));
      sum += term;
    }
    if (term==0.0 && k<n)
      throw new Error("Binomial " + n + "," + p + " can not be computed");
    return k;
  }

  // RandomFromBinomial - returns a random int drawn from a Binomial dist
  public static long RandomFromBinomial(long n, double p, KnuthRandom rand) {
    if (n>100)
      return RandomFromPoisson(n*p, rand); // Poisson approximation

    double r = rand.randomDouble();
    long k=0;
    double term = Math.pow(1-p, n);
    double sum = term;
    if (term==0.0) {
      return RandomFromPoisson(n*p, rand); // Poisson approximation
    // throw new Error("Binomial " + n + "," + p + " can not be computed");    
    }
    while ((sum<=r) && (term>0.0) && (k<n)) {
      k++;
      term *= (n-k+1)*p/(k*(1.0-p));
      sum += term;
    }
    if (term==0.0 && k<n)
      throw new Error("Binomial " + n + "," + p + " can not be computed");
    return k;
  }

  // stolen from cern's jet.random package
  // http://tilde-hoschek.home.cern.ch/~hoschek/colt/V1.0.1/docsrc/cern/jet/random/Gamma.java.html
  public static double RandomFromGamma(double alpha, double lambda, KnuthRandom rand) {
    /******************************************************************
     *                                                                *
     *    Gamma Distribution - Acceptance Rejection combined with     *
     *                         Acceptance Complement                  *
     *                                                                *
     ******************************************************************
     *                                                                *
     * FUNCTION:    - gds samples a random number from the standard   *
     *                gamma distribution with parameter  a > 0.       *
     *                Acceptance Rejection  gs  for  a < 1 ,          *
     *                Acceptance Complement gd  for  a >= 1 .         *
     * REFERENCES:  - J.H. Ahrens, U. Dieter (1974): Computer methods *
     *                for sampling from gamma, beta, Poisson and      *
     *                binomial distributions, Computing 12, 223-246.  *
     *              - J.H. Ahrens, U. Dieter (1982): Generating gamma *
     *                variates by a modified rejection technique,     *
     *                Communications of the ACM 25, 47-54.            *
     * SUBPROGRAMS: - drand(seed) ... (0,1)-Uniform generator with    *
     *                unsigned long integer *seed                     *
     *              - NORMAL(seed) ... Normal generator N(0,1).       *
     *                                                                *
     ******************************************************************/
    double a = alpha;
    double aa = -1.0, aaa = -1.0, 
      b=0.0, c=0.0, d=0.0, e, r, s=0.0, si=0.0, ss=0.0, q0=0.0,
      q1 = 0.0416666664, q2 =  0.0208333723, q3 = 0.0079849875,
      q4 = 0.0015746717, q5 = -0.0003349403, q6 = 0.0003340332,
      q7 = 0.0006053049, q8 = -0.0004701849, q9 = 0.0001710320,
      a1 = 0.333333333,  a2 = -0.249999949,  a3 = 0.199999867,
      a4 =-0.166677482,  a5 =  0.142873973,  a6 =-0.124385581,
      a7 = 0.110368310,  a8 = -0.112750886,  a9 = 0.104089866,
      e1 = 1.000000000,  e2 =  0.499999994,  e3 = 0.166666848,
      e4 = 0.041664508,  e5 =  0.008345522,  e6 = 0.001353826,
      e7 = 0.000247453;

    double gds,p,q,t,sign_u,u,v,w,x;
    double v1,v2,v12;

    // Check for invalid input values

    if (a <= 0.0) throw new IllegalArgumentException(); 
    if (lambda <= 0.0) new IllegalArgumentException(); 

    if (a < 1.0) { // CASE A: Acceptance rejection algorithm gs
      b = 1.0 + 0.36788794412 * a;              // Step 1
      for(;;) {
	p = b * rand.randomDouble();
	if (p <= 1.0) {                       // Step 2. Case gds <= 1
	  gds = Math.exp(Math.log(p) / a);
	  if (Math.log(rand.randomDouble()) <= -gds) return(gds/lambda);
	}
	else {                                // Step 3. Case gds > 1
	  gds = - Math.log ((b - p) / a);
	  if (Math.log(rand.randomDouble()) <= ((a - 1.0) * Math.log(gds))) return(gds/lambda);
	}
      }
    }

    else {        // CASE B: Acceptance complement algorithm gd (gaussian distribution, box muller transformation)
      if (a != aa) {                        // Step 1. Preparations
	aa = a;
	ss = a - 0.5;
	s = Math.sqrt(ss);
	d = 5.656854249 - 12.0 * s;
      }
      // Step 2. Normal deviate
      do {
	v1 = 2.0 * rand.randomDouble() - 1.0;
	v2 = 2.0 * rand.randomDouble() - 1.0;
	v12 = v1*v1 + v2*v2;
      } while ( v12 > 1.0 );
      t = v1*Math.sqrt(-2.0*Math.log(v12)/v12);
      x = s + 0.5 * t;
      gds = x * x;
      if (t >= 0.0) return(gds/lambda);         // Immediate acceptance

      u = rand.randomDouble();                // Step 3. Uniform random number
      if (d * u <= t * t * t) return(gds/lambda); // Squeeze acceptance

      if (a != aaa) {                           // Step 4. Set-up for hat case
	aaa = a;
	r = 1.0 / a;
	q0 = ((((((((q9 * r + q8) * r + q7) * r + q6) * r + q5) * r + q4) *
		r + q3) * r + q2) * r + q1) * r;
	if (a > 3.686) {
	  if (a > 13.022) {
	    b = 1.77;
	    si = 0.75;
	    c = 0.1515 / s;
	  }
	  else {
	    b = 1.654 + 0.0076 * ss;
	    si = 1.68 / s + 0.275;
	    c = 0.062 / s + 0.024;
	  }
	}
	else {
	  b = 0.463 + s - 0.178 * ss;
	  si = 1.235;
	  c = 0.195 / s - 0.079 + 0.016 * s;
	}
      }
      if (x > 0.0) {                        // Step 5. Calculation of q
	v = t / (s + s);                  // Step 6.
	if (Math.abs(v) > 0.25) {
	  q = q0 - s * t + 0.25 * t * t + (ss + ss) * Math.log(1.0 + v);
	}
	else {
	  q = q0 + 0.5 * t * t * ((((((((a9 * v + a8) * v + a7) * v + a6) *
				      v + a5) * v + a4) * v + a3) * v + a2) * v + a1) * v;
	}                                 // Step 7. Quotient acceptance
	if (Math.log(1.0 - u) <= q) return(gds/lambda);
      }

      for(;;) {                             // Step 8. Double exponential deviate t
	do {
	  e = -Math.log(rand.randomDouble());
	  u = rand.randomDouble();
	  u = u + u - 1.0;
	  sign_u = (u > 0)? 1.0 : -1.0;
	  t = b + (e * si) * sign_u;
	} while (t <= -0.71874483771719); // Step 9. Rejection of t
	v = t / (s + s);                  // Step 10. New q(t)
	if (Math.abs(v) > 0.25) {
	  q = q0 - s * t + 0.25 * t * t + (ss + ss) * Math.log(1.0 + v);
	}
	else {
	  q = q0 + 0.5 * t * t * ((((((((a9 * v + a8) * v + a7) * v + a6) *
				      v + a5) * v + a4) * v + a3) * v + a2) * v + a1) * v;
	}
	if (q <= 0.0) continue;           // Step 11.
	if (q > 0.5) {	  w = Math.exp(q) - 1.0;
	}
	else {
	  w = ((((((e7 * q + e6) * q + e5) * q + e4) * q + e3) * q + e2) *
	       q + e1) * q;
	}                                 // Step 12. Hat acceptance
	if ( c * u * sign_u <= w * Math.exp(e - 0.5 * t * t)) {
	  x = s + 0.5 * t;
	  return(x*x/lambda);
	}
      }
    }
  }

  // RandomFromExponential - returns a random double drawn from the 
  // exponential distribution.
  public static double RandomFromExponential(double lambda, KnuthRandom rand) {
    if (lambda==0.0)
      return Double.MAX_VALUE;
    double d;
    do {
      d = rand.randomDouble();
    } while (d == 0.0);
    return -Math.log(d) / lambda;
  }

  // RandomFromNegativeBinomial - returns a random int drawn from a 
  // negative Binomial dist
  public static long RandomFromNegativeBinomial(long n0, double p, 
						KnuthRandom rand) {
    double term = Math.pow(p, n0);
    if (true || term==0.0 || n0>50) {
      p = 1.0-p;
      double y = RandomFromGamma(n0,1.0,rand) * p/(1.0-p);
      //            System.out.println("RandomFromNegativeBinomial (" +n0 + "," + p + ") " + 
      //			 "Gamma (" + n0 + ")*p/(1-p) =" + y + "," + 
      //			 "Poisson(y) = "+
      //			 RandomFromPoisson(y,rand));
      return RandomFromPoisson(y,rand);
    }

    double r = rand.randomDouble();
    int k=0;
    double sum = term;
    //      System.out.println(r + " i=" + k + " term=" + term + " sum=" + sum);
    while ((sum<=r) && (term>0.0)) {
      k++;
      term *= (n0+k-1) * (1.0-p) / k;
      sum += term;
      //      System.out.println(r + " i=" + k + " term=" + term + " sum=" + sum);
    }
    if (term==0.0)
      throw new Error("Negative binomial " + n0 + "," + p + " can not be computed");
    return n0+k;
  }

  // RandomFromBirthDeath - birth death process with an initial population of n
  // taken from Renshaw's Modelling Biological Populations in Space and Time,
  // pages 34-5
  public static long RandomFromBirthDeath(long n,
					  int t,
					  double birth,
					  double death,
					  KnuthRandom rand) {
    double alpha = (death*(Math.exp((birth-death)*t)-1))/
                   (birth*Math.exp((birth-death)*t)-death);
    double beta = (birth*(Math.exp((birth-death)*t)-1))/
                  (birth*Math.exp((birth-death)*t)-death);
    
    double r = rand.randomDouble();
    long k=0;
    double term = (1.0-alpha)*(1.0-beta);
    double sum = term;
    if (term==0.0) {
      throw new Error("birth-death " + n + "," + t + "," + birth + "," + death + " can not be computed");
    }
    while ((sum<=r) && (term>0.0)) {
      k++;
      term *= beta;
      sum += term;
    }
    if (term==0.0 && k<n)
      throw new Error("birth-death " + n + "," + t + "," + birth + "," + death + " can not be computed");
    return k;
  }
					 
  // RandomFromCDF - returns a random number conditioned on cdf
  public static long RandomFromCDF(ArrayList cdf, KnuthRandom rand) {
    double r = rand.randomDouble();
    long i=0;
    for (Iterator it=cdf.iterator(); it.hasNext();) {
      if (r<((Double)it.next()).doubleValue())
	return i;
      else
	i++;
    }
    System.out.println("OVERFLOW in randomfromcdf " + cdf.size());
    return cdf.size();
  }

  /*
   * Convolve - convolves two vectors and returns the result.
   */
  public static double[] Convolve(double[] a, double[] b) {
    double[] result = new double[a.length + b.length - 1];
    for (int i=0; i<a.length; i++)
      for (int j=0; j<b.length; j++)
	result[i+j] += a[i]*b[j];
    return result;
  }

  /*
   * Convolve - convolves a vector with itself "n" times
   */
  public static double[] NConvolve(double[] a, int n) {
    double[] result = a;
    for (int i=0; i<n-1; i++)
      result = Convolve(a, result);
    return result;
  }

  /*
   * Convolve - convolves two vectors and returns the result.
   */
  public static long[] Convolve(long[] a, long[] b) {
    long[] result = new long[a.length + b.length - 1];
    for (int i=0; i<a.length; i++)
      for (int j=0; j<b.length; j++)
	result[i+j] += a[i]*b[j];
    return result;
  }

  /*
   * Convolve - convolves a vector with itself "n" times
   */
  public static long[] NConvolve(long[] a, int n) {
    long[] result = a;
    for (int i=0; i<n-1; i++)
      result = Convolve(a, result);
    return result;
  }

  /*
   * MaximumOverEvents - returns the distribution of the maximum values
   * of a random variable "x" if "a" is the distribution of "x".
   */
  public static double[] MaximumOverEvents(double[] a, int nNumTrials) {
    double[] result = new double[a.length];
    // take cumulative sum of a
    result[0] = a[0];
    for (int i=1; i<result.length; i++)
      result[i] = a[i] + result[i-1];

    // exponentiate entries
    for (int i=0; i<result.length; i++)
      result[i] = Math.pow(result[i], nNumTrials);

    // differentiate
    for (int i=result.length-1; i>0; i--)
      result[i] -= result[i-1];

    return result;
  }

  public static double[] MaximumBetweenEvents(double[] a, double[] b) {
    double[] cuma = new double[a.length];
    // take cumulative sum of a
    cuma[0] = a[0];
    for (int i=1; i<cuma.length; i++)
      cuma[i] = a[i] + cuma[i-1];
    // take cumulative sum of b    
    double[] cumb = new double[b.length];
    cumb[0] = b[0];
    for (int i=1; i<cumb.length; i++)
      cumb[i] = b[i] + cumb[i-1];

    double[] result = new double[a.length];
    // exponentiate entries
    result[0] = a[0] + b[0] - (a[0]*b[0]);
    for (int i=1; i<result.length; i++)
      result[i] = (a[i] * (cumb[i])) + (b[i]*(cuma[i-1]));

    return result;
  }

  /*
   * getDistributionTailBoundary - return r such that the probability that 
   * x>=r is f
   */
  public static int getDistributionTailBoundary(double[] a, double f) {
    double sum=0.0;
    int i;
    for (i=a.length-1; i>=0 && sum<f; i--)
      sum += a[i];
    if (sum-f < f+a[i]-sum)
      return i;
    else
      return i+1;
  }

  /*
   * getDistributionIntervalUpperBoundary - return r such that the 
   * probability that lo<=x<=r is f
   */
  public static int getDistributionIntervalUpperBoundary(double[] a, double f, int lo) {
    double sum=0.0;
    int i;
    for (i=lo; i<a.length && sum<f; i++)
      sum += a[i];
    if (i>=a.length)
      return a.length-1;
    else if (f-sum < sum+a[i]-f)
      return i-1;
    else
      return i;
  }

  // how many points are radius away from the center in dim dimensions?
  public static long getSurfaceSize(int dim, int radius) {
    if (radius==0)
      return 1;
    else if (dim==1) {
      return 2;
    } else {
      long sum=2;
      for (int i=1; i<radius; i++)
	sum += 2*getSurfaceSize(dim-1, radius-i);
      sum += getSurfaceSize(dim-1, radius);
      return sum;
    }
  }

  // getPermutation - returns a permuted array containing ints from 0..nSize-1
  // uses Fisher-Yates shuffle (described in Knuth)
  public static int[] getPermutation(int nSize, KnuthRandom r) {
    int [] p = new int[nSize];
    for (int i=0; i<nSize; i++)
      p[i] = i;
    for (int i=nSize-1; i>0; i--) {
      int j = r.randomInt(i);
      if (i!=j) {
        int temp = p[i];
        p[i] = p[j];
        p[j] = temp;
      }
    }
    return p;
  }

  // getPermutation - permutes elements in array p
  // uses Fisher-Yates shuffle (described in Knuth)
  public static void getPermutation(int[] p, KnuthRandom r) {
    for (int i=p.length-1; i>0; i--) {
      int j = r.randomInt(i+1);
      if (i!=j) {
        int temp = p[i];
        p[i] = p[j];
        p[j] = temp;
      }
    }
  }

  /*
   * main - a little test program
   */
  public static void main(String[] args) {
    if (true) {
      KnuthRandom r = new KnuthRandom();
      r.seedRandom(-1);
      for (int i=0; i<10; i++) {
        double expected = 10.0;
        System.out.println(RandomFromPoisson(expected,r));
      }
    }
      if (true)
	return;
    if (false) {
      KnuthRandom r = new KnuthRandom();
      r.seedRandom(-1);
      long asize = 300;
      long e =10000;
      int ts = 24 * (60/60);
      double sum1=0, sum2=0;
      for (int i=0; i<10*ts/24; i++) {
        double expected = asize*(1.0-Math.exp(-(12.0*e*asize / (8000+asize+e)) /
                                              ts/asize));
        sum1+=expected;
        long sub = RandomFromPoisson(expected,r);
        sum2+=sub;
        System.out.println(expected + " " + sub);
      }
      System.out.println(":"+sum1 + " " + sum2);
      if (true)
	return;
    }

    if (false) {
    double p = 1.0-Math.exp(-3.0/24.0);
    int n0 = 8;
    double term = Math.pow(p, n0);
    int k=0;
    double sum = term;
    double r = 0.999999;
    //      System.out.println(r + " i=" + k + " term=" + term + " sum=" + sum);
    while (k<24*8) { //(sum<=r) && (term>0.0)) {
      System.out.println(k + " " + term + " " + sum);
      k++;
      term *= (n0+k-1) * (1.0-p) / k;
      sum += term;
      //      System.out.println(r + " i=" + k + " term=" + term + " sum=" + sum);
    }


    if (true)
      return;
    }

    long nNumClones = (int)Math.pow(10,7);
    int nMaxRadius = 8;
    int nStringLength = 80;
    int nAlphabetSize = 4;

    KnuthRandom r = new KnuthRandom();
    r.seedRandom(-1);
    int t = 10;
    int n = 50;

long sum1=0,sum2=0;
    for (int j=0; j<10; j++) {
      long bb = 0;
      for (int i=0; i<4; i++) {
	bb += Probability.RandomFromNegativeBinomial(n,
						     Math.exp(-1.0/24.0),
						     r)-n;
      }
      System.out.print("neg bin=" + bb);
      sum1+=bb;
      bb =  Probability.RandomFromNegativeBinomial(n,
						   0.84648, //Math.exp(-4.0/24.0),
						   r)-n;
      sum2+= bb;
      System.out.println(", " + bb);
    }
    System.out.println(sum1 + " " + sum2);
    if (true)
      return;

    for (int i=0; i<=80; i++) {
      System.out.println(i + " " + 
			 //			 Probability.Binomial(80, 0.5, i) + " " + 
			 Probability.Binomial(80, 0.666666667, i) + " " + 
			 //			 Probability.Binomial(80, 0.75, i) + " " + 
			 //			 Probability.Binomial(80, 0.8, i) + " "
" "
);
    }
     for (int i=0; i<=20; i++) {
      Probability.RandomFromBinomial(i, 0.5, r);
      System.out.println(i + " " + Probability.Binomial(20, 0.5, i) + " " +
			 Probability.Binomial(20, 0.25, i));
    }

//Probability.RandomFromNegativeBinomial(n, Math.exp(-0.1*t),r);
    

    //  public static double Binomial(int n, double p, int i) {

if (true)
  return;
double b = Math.exp(-1541.92);
System.out.println(Math.pow(nAlphabetSize, nStringLength) + " specs");
if (b==0.0) {
System.out.println(b + " = 0");
}
    double sum = 0;
    KnuthRandom rand = new KnuthRandom();
    rand.seedRandom(467739585);

    for (int radius=0; radius<6; radius++) {
      System.out.println("radius " + radius);
    double p = Probability.Combination(nStringLength, radius) *
      Math.pow((nAlphabetSize-1)/(double)nAlphabetSize, radius) *
      Math.pow(1/(double)nAlphabetSize, nStringLength-radius);
    double lambda = p * nNumClones;

    System.out.println("  expected = " + lambda);
    double a = Probability.RandomFromPoisson(lambda, rand);
    sum += lambda;
    System.out.println("  actual = " + a);
    }
    System.out.println("sum = " + sum);
    System.out.println(sum/nNumClones + " of clones covered");

    /*
    System.out.println("answer = " + Probability.Poisson(1541.92, 254));
    System.out.println("answer = " + Probability.Poisson(1541.92, 255));
    System.out.println("answer = " + Probability.Poisson(729.99000549316406, 583));
    */
    //    System.out.println("answer = " + Probability.PoissonCDF(729.99000549316406, 0.99999999989999999));

    /*
    int radius = nMaxRadius;
    double p = Probability.Combination(nStringLength, radius) *
      Math.pow((nAlphabetSize-1)/(double)nAlphabetSize, radius) *
      Math.pow(1/(double)nAlphabetSize, nStringLength-radius);
    double lambda = p * nNumClones;
    Random rand = new Random();
    
    ArrayList poisson = Probability.PoissonCDF(lambda, 0.9999999);
    System.out.println("lambda = " + lambda + ", cdf size = " + poisson.size());
    for (int i=0; i<20; i++) {
       System.out.println(RandomFromCDF(poisson, rand));
      
    }
    */
    /*
    long[][]b = Probability.InversePoissonCDF2(nNumClones, 
					      nMaxRadius,
					      nStringLength,
					      nAlphabetSize);
    Random random = new Random();
    for (int i=0; i<nMaxRadius; i++) {
      double r = random.nextDouble();
      System.out.println(i + " " + r + " = " + b[i][(int)(r*Probability.resolution)]);
    }
    */
  }
}
