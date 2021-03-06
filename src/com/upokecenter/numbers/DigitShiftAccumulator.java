package com.upokecenter.numbers;
/*
Written by Peter O. in 2013.
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/
If you like this, you should donate to Peter O.
at: http://peteroupc.github.io/
 */

  final class DigitShiftAccumulator implements IShiftAccumulator {
    private static final long[] TenPowersLong = {
      1L, 10L, 100L, 1000L, 10000L, 100000L,
      1000000L, 10000000L, 100000000L,
      1000000000L,
      10000000000L,
      100000000000L,
      1000000000000L,
      10000000000000L,
      100000000000000L,
      1000000000000000L,
      10000000000000000L,
      100000000000000000L,
      1000000000000000000L,
    };

    private static final EInteger ValueTen = EInteger.FromInt32(10);

    private static final int[] ValueTenPowers = {
      1, 10, 100, 1000, 10000, 100000,
      1000000, 10000000, 100000000
    };

    private int bitLeftmost;

    private int bitsAfterLeftmost;
    private FastInteger discardedBitCount;
    private boolean isSmall;
    private FastInteger knownDigitLength;

    private EInteger shiftedBigInt;

    private int shiftedSmall;

    @Override public String toString() {
      return "[this.bitLeftmost=" + this.bitLeftmost +
       ", this.bitsAfterLeftmost=" + this.bitsAfterLeftmost +
       ", this.discardedBitCount=" + this.discardedBitCount +
       ", this.isSmall=" + this.isSmall + ", this.knownDigitLength=" +
         this.knownDigitLength + ", this.shiftedBigInt=" +
         this.shiftedBigInt + ", this.shiftedSmall=" +
       this.shiftedSmall + "]";
    }

    public DigitShiftAccumulator(
  EInteger bigint,
  int lastDiscarded,
  int olderDiscarded) {
      if (bigint.CanFitInInt32()) {
        this.shiftedSmall = bigint.ToInt32Checked();
        if (this.shiftedSmall < 0) {
          throw new IllegalArgumentException("shiftedSmall (" + this.shiftedSmall +
            ") is less than 0");
        }
        this.isSmall = true;
      } else {
        this.shiftedBigInt = bigint;
        this.isSmall = false;
      }
      this.bitsAfterLeftmost = (olderDiscarded != 0) ? 1 : 0;
      this.bitLeftmost = lastDiscarded;
    }

    public DigitShiftAccumulator(
  int smallint,
  int lastDiscarded,
  int olderDiscarded) {
        this.shiftedSmall = smallint;
        if (this.shiftedSmall < 0) {
          throw new IllegalArgumentException("shiftedSmall (" + this.shiftedSmall +
            ") is less than 0");
        }
        this.isSmall = true;
      this.bitsAfterLeftmost = (olderDiscarded != 0) ? 1 : 0;
      this.bitLeftmost = lastDiscarded;
    }

    public final FastInteger getDiscardedDigitCount() {
        this.discardedBitCount = (this.discardedBitCount == null) ? ((new FastInteger(0))) : this.discardedBitCount;
        return this.discardedBitCount;
      }

    public final int getLastDiscardedDigit() {
        return this.bitLeftmost;
      }

    public final int getOlderDiscardedDigits() {
        return this.bitsAfterLeftmost;
      }

    public final EInteger getShiftedInt() {
        return this.isSmall ? (EInteger.FromInt32(this.shiftedSmall)) :
        this.shiftedBigInt;
      }

    public final FastInteger getShiftedIntFast() {
        return this.isSmall ? (new FastInteger(this.shiftedSmall)) :
        FastInteger.FromBig(this.shiftedBigInt);
      }

    public FastInteger GetDigitLength() {
  this.knownDigitLength = (this.knownDigitLength == null) ? (this.CalcKnownDigitLength()) : this.knownDigitLength;
      return this.knownDigitLength;
    }

    public void ShiftRight(FastInteger fastint) {
      if (fastint == null) {
        throw new NullPointerException("fastint");
      }
      if (fastint.CanFitInInt32()) {
        int fi = fastint.AsInt32();
        if (fi < 0) {
          return;
        }
        this.ShiftRightInt(fi);
      } else {
        if (fastint.signum() <= 0) {
          return;
        }
        EInteger bi = fastint.AsEInteger();
        while (bi.signum() > 0) {
          int count = 1000000;
          if (bi.compareTo(EInteger.FromInt64(1000000)) < 0) {
            count = bi.ToInt32Checked();
          }
          this.ShiftRightInt(count);
          bi = bi.Subtract(EInteger.FromInt32(count));
          if (this.isSmall ? this.shiftedSmall == 0 :
          this.shiftedBigInt.isZero()) {
            break;
          }
        }
      }
    }

    public void ShiftRightInt(int digits) {
      // <summary>Shifts a number to the right, gathering information on
      // whether the last digit discarded is set and whether the discarded
      // digits to the right of that digit are set. Assumes that the big
      // integer being shifted is positive.</summary>
      if (this.isSmall) {
        this.ShiftRightSmall(digits);
      } else {
        this.ShiftRightBig(digits, false);
      }
    }

    public void ShiftToDigits(
  FastInteger bits,
  FastInteger preShift,
  boolean truncate) {
      if (preShift != null && preShift.signum() > 0) {
        FastInteger kdl = (this.knownDigitLength == null) ? (this.CalcKnownDigitLength()) : this.knownDigitLength;
        this.knownDigitLength = kdl;
        // DebugUtility.Log("bits=" + bits + " pre=" + preShift + " known=" +
        // (//kdl) + " [" + this.shiftedBigInt + "]");
        if (kdl.compareTo(bits) <= 0) {
          // Known digit length is already small enough
          if (truncate) {
            this.TruncateRight(preShift);
          } else {
            this.ShiftRight(preShift);
          }
        this.VerifyKnownLength();
          return;
        } else {
          FastInteger bitDiff = kdl.Copy().Subtract(bits);
          // DebugUtility.Log("bitDiff=" + bitDiff);
          int cmp = bitDiff.compareTo(preShift);
          if (cmp <= 0) {
            // Difference between desired digit length and current
            // length is smaller than the shift, make it the shift
           if (truncate) {
             this.TruncateRight(preShift);
           } else {
             this.ShiftRight(preShift);
           }
        this.VerifyKnownLength();
           return;
          } else {
           if (truncate) {
             this.TruncateRight(bitDiff);
           } else {
             this.ShiftRight(bitDiff);
           }
        this.VerifyKnownLength();
           return;
          }
        }
      }
      if (bits.CanFitInInt32()) {
        int intval = bits.AsInt32();
        if (intval < 0) {
          throw new IllegalArgumentException("intval (" + intval + ") is less than " +
                   "0");
        }
        if (this.isSmall) {
          this.ShiftToDigitsSmall(intval);
        } else {
          this.ShiftToDigitsBig(intval, truncate);
        }
        this.VerifyKnownLength();
      } else {
        FastInteger kdl = (this.knownDigitLength == null) ? (this.CalcKnownDigitLength()) : this.knownDigitLength;
        this.knownDigitLength = kdl;
        this.VerifyKnownLength();
        EInteger bigintDiff = kdl.AsEInteger();
        EInteger bitsBig = bits.AsEInteger();
        bigintDiff = bigintDiff.Subtract(bitsBig);
        if (bigintDiff.signum() > 0) {
          // current length is greater than the
          // desired bit length
          this.ShiftRight(FastInteger.FromBig(bigintDiff));
        }
        this.VerifyKnownLength();
      }
    }

    public void TruncateRight(FastInteger fastint) {
      if (fastint == null) {
        throw new NullPointerException("fastint");
      }
      if (fastint.CanFitInInt32()) {
        int fi = fastint.AsInt32();
        if (fi < 0) {
          return;
        }
        if (!this.isSmall) {
          if (this.shiftedBigInt.CanFitInInt64()) {
            this.TruncateRightLong(this.shiftedBigInt.ToInt64Checked(), fi);
          } else {
            this.ShiftRightBig(fi, true);
          }
        } else {
          this.TruncateRightSmall(fi);
        }
      } else {
        this.ShiftRight(fastint);
      }
    }

    private static int FastParseLong(String str, int offset, int length) {
      // Assumes the String is length 9 or less and contains
      // only the digits '0' through '9'
      if (length > 9) {
        throw new IllegalArgumentException("length (" + length + ") is more than " +
               "9 ");
      }
      int ret = 0;
      for (int i = 0; i < length; ++i) {
        int digit = (int)(str.charAt(offset + i) - '0');
        ret *= 10;
        ret += digit;
      }
      return ret;
    }

    private static int LongDigitLength(long value) {
      if (value >= 1000000000L) {
        return (value >= 1000000000000000000L) ? 19 : ((value >=
                 100000000000000000L) ? 18 : ((value >= 10000000000000000L) ?
                  17 : ((value >= 1000000000000000L) ? 16 :
                  ((value >= 100000000000000L) ? 15 : ((value
                  >= 10000000000000L) ?
                  14 : ((value >= 1000000000000L) ? 13 : ((value
                  >= 100000000000L) ? 12 : ((value >= 10000000000L) ?
                  11 : ((value >= 1000000000L) ? 10 : 9)))))))));
      } else {
        int v2 = (int)value;
        return (v2 >= 100000000) ? 9 : ((v2 >= 10000000) ? 8 : ((v2 >=
                  1000000) ? 7 : ((v2 >= 100000) ? 6 : ((v2
                  >= 10000) ? 5 : ((v2 >= 1000) ? 4 : ((v2 >= 100) ?
                  3 : ((v2 >= 10) ? 2 : 1)))))));
      }
    }

    private FastInteger CalcKnownDigitLength() {
      if (this.isSmall) {
        int kb = 0;
        int v2 = this.shiftedSmall;
        if (v2 < 100000) {
          kb = (v2 >= 10000) ? 5 : ((v2 >= 1000) ? 4 : ((v2 >= 100) ?
          3 : ((v2 >= 10) ? 2 : 1)));
        } else {
          kb = (v2 >= 1000000000) ? 10 : ((v2 >= 100000000) ? 9 : ((v2 >=
          10000000) ? 8 : ((v2 >= 1000000) ? 7 : 6)));
        }
        return new FastInteger(kb);
      }
      return new FastInteger(this.shiftedBigInt.GetDigitCount());
    }

    private void VerifyKnownLength() {
/*

*/
    }

    private void UpdateKnownLengthInt(int digits) {
      if (this.knownDigitLength != null) {
        this.knownDigitLength.SubtractInt(digits);
        if (this.knownDigitLength.CompareToInt(1) < 0) {
          this.knownDigitLength.SetInt(1);
        }
        this.VerifyKnownLength();
      }
    }

    private void UpdateKnownLength(FastInteger digitsFast) {
      if (this.knownDigitLength != null) {
        this.knownDigitLength.Subtract(digitsFast);
        if (this.knownDigitLength.CompareToInt(1) < 0) {
          this.knownDigitLength.SetInt(1);
        }
        this.VerifyKnownLength();
      }
    }

    private void ShiftRightBig(int digits, boolean truncate) {
      if (digits <= 0) {
        return;
      }
      if (this.shiftedBigInt.isZero()) {
        this.discardedBitCount = (this.discardedBitCount == null) ? ((new FastInteger(0))) : this.discardedBitCount;
        this.discardedBitCount.AddInt(digits);
        this.bitsAfterLeftmost |= this.bitLeftmost;
        this.bitLeftmost = 0;
        this.knownDigitLength = new FastInteger(1);
        return;
      }
      if (truncate) {
        EInteger bigquo;
        if (digits > 50) {
          // To avoid having to calculate a very big power of 10,
          // check the digit count to see if doing so can be avoided
          int bitLength = this.shiftedBigInt.GetUnsignedBitLength();
          boolean bigPower = false;
          // 10^48 has 160 bits; 10^98 has 326; bit length is cheaper
          // to calculate than base-10 digit length
          if (bitLength < 160 || (digits > 100 && bitLength < 326)) {
            bigPower = true;
          } else {
            FastInteger knownDigits = this.GetDigitLength();
            bigPower = knownDigits.Copy().SubtractInt(digits)
              .CompareToInt(-2) < 0;
            if (!bigPower) {
              // DebugUtility.Log("digitlength {0} [todiscard: {1}]"
              // , knownDigits, digits);
            }
          }
          if (bigPower) {
            // Power of 10 to be divided would be much bigger
       this.discardedBitCount = (this.discardedBitCount == null) ? ((new
              FastInteger(0))) : this.discardedBitCount;
            this.discardedBitCount.AddInt(digits);
            this.bitsAfterLeftmost |= this.bitLeftmost;
            this.bitsAfterLeftmost |= this.shiftedBigInt.isZero() ? 0 : 1;
            this.bitLeftmost = 0;
            this.knownDigitLength = new FastInteger(1);
            this.isSmall = true;
            this.shiftedSmall = 0;
            return;
          }
        }
        if (this.shiftedBigInt.isEven() && this.bitLeftmost == 0) {
          EInteger[] quorem = this.shiftedBigInt.DivRem(
          NumberUtility.FindPowerOfTen(digits));
          bigquo = quorem[0];
          this.bitLeftmost |= quorem[1].isZero() ? 0 : 1;
        } else {
          this.bitLeftmost = 1;
          bigquo = this.shiftedBigInt.Divide(
          NumberUtility.FindPowerOfTen(digits));
        }
        this.bitsAfterLeftmost |= this.bitLeftmost;
        this.discardedBitCount = this.discardedBitCount == null ?
           new FastInteger(digits) : this.discardedBitCount.AddInt(digits);
        if (bigquo.isZero()) {
          // Shifted all the way to 0
          this.isSmall = true;
          this.shiftedBigInt = null;
          this.shiftedSmall = 0;
          this.knownDigitLength = new FastInteger(1);
        } else if (bigquo.CanFitInInt32()) {
          this.isSmall = true;
          this.shiftedSmall = bigquo.ToInt32Unchecked();
          this.shiftedBigInt = null;
          this.UpdateKnownLengthInt(digits);
        } else {
          this.isSmall = false;
          this.shiftedBigInt = bigquo;
          this.UpdateKnownLengthInt(digits);
        }
        return;
      }
      if (digits == 1) {
        EInteger bigrem;
        EInteger bigquo;
        EInteger[] divrem = this.shiftedBigInt.DivRem(EInteger.FromInt32(10));
        bigquo = divrem[0];
        bigrem = divrem[1];
        this.bitsAfterLeftmost |= this.bitLeftmost;
        this.bitLeftmost = bigrem.ToInt32Checked();
        this.shiftedBigInt = bigquo;
        this.discardedBitCount = (this.discardedBitCount == null) ? ((new FastInteger(0))) : this.discardedBitCount;
        this.discardedBitCount.AddInt(digits);
        this.UpdateKnownLengthInt(digits);
        return;
      }
      if (digits >= 2 && digits <= 8) {
        EInteger bigrem;
        EInteger bigquo;
        EInteger[] divrem =
          this.shiftedBigInt.DivRem(NumberUtility.FindPowerOfTen(digits));
        bigquo = divrem[0];
        bigrem = divrem[1];
        int intRem = bigrem.ToInt32Checked();
        int smallPower = ValueTenPowers[digits - 1];
        int leftBit = intRem / smallPower;
        int otherBits = intRem - (leftBit * smallPower);
        this.bitsAfterLeftmost |= otherBits | this.bitLeftmost;
        this.bitLeftmost = leftBit;
        this.shiftedBigInt = bigquo;
        this.discardedBitCount = (this.discardedBitCount != null) ?
          this.discardedBitCount.AddInt(digits) : (new FastInteger(digits));
        this.UpdateKnownLengthInt(digits);
        this.bitsAfterLeftmost = (this.bitsAfterLeftmost != 0) ? 1 : 0;
        if (this.shiftedBigInt.CanFitInInt32()) {
          this.isSmall = true;
          this.shiftedSmall = this.shiftedBigInt.ToInt32Unchecked();
          this.shiftedBigInt = null;
        }
        return;
      }
  this.knownDigitLength = (this.knownDigitLength == null) ? (this.CalcKnownDigitLength()) : this.knownDigitLength;
      if (new FastInteger(digits).Decrement().compareTo(this.knownDigitLength)
      >= 0) {
        // Shifting more bits than available
        this.bitsAfterLeftmost |= this.shiftedBigInt.isZero() ? 0 : 1;
        this.isSmall = true;
        this.shiftedSmall = 0;
        this.knownDigitLength = new FastInteger(1);
        this.discardedBitCount = (this.discardedBitCount == null) ? ((new FastInteger(0))) : this.discardedBitCount;
        this.discardedBitCount.AddInt(digits);
        this.bitsAfterLeftmost |= this.bitLeftmost;
        this.bitLeftmost = 0;
        return;
      }
      if (this.shiftedBigInt.CanFitInInt32()) {
        this.isSmall = true;
        this.shiftedSmall = this.shiftedBigInt.ToInt32Checked();
        this.ShiftRightSmall(digits);
        return;
      }
      if (this.shiftedBigInt.CanFitInInt64()) {
        this.ShiftRightLong(this.shiftedBigInt.ToInt64Unchecked(), digits);
        return;
      }
      String str = this.shiftedBigInt.toString();
      // NOTE: Will be 1 if the value is 0
      int digitLength = str.length();
      int bitDiff = 0;
      if (digits > digitLength) {
        bitDiff = digits - digitLength;
      }
      this.discardedBitCount = (this.discardedBitCount == null) ? ((new FastInteger(0))) : this.discardedBitCount;
      this.discardedBitCount.AddInt(digits);
      this.bitsAfterLeftmost |= this.bitLeftmost;
      int digitShift = Math.min(digitLength, digits);
      if (digits >= digitLength) {
        this.isSmall = true;
        this.shiftedSmall = 0;
        this.knownDigitLength = new FastInteger(1);
      } else {
        int newLength = (int)(digitLength - digitShift);
        if (newLength <= 9) {
          // Fits in a small number
          this.isSmall = true;
          this.shiftedSmall = FastParseLong(str, 0, newLength);
        } else {
          this.shiftedBigInt = EInteger.FromSubstring(str, 0, newLength);
        }
        this.UpdateKnownLengthInt(digitShift);
      }
      for (int i = str.length() - 1; i >= 0; --i) {
        this.bitsAfterLeftmost |= this.bitLeftmost;
        this.bitLeftmost = (int)(str.charAt(i) - '0');
        --digitShift;
        if (digitShift <= 0) {
          break;
        }
      }
      this.bitsAfterLeftmost = (this.bitsAfterLeftmost != 0) ? 1 : 0;
      if (bitDiff > 0) {
        // Shifted more digits than the digit length
        this.bitsAfterLeftmost |= this.bitLeftmost;
        this.bitLeftmost = 0;
      }
    }

    private void ShiftRightLong(long shiftedLong, int digits) {
      if (digits <= 0) {
        return;
      }
      if (shiftedLong == 0) {
        this.shiftedSmall = 0;
        this.isSmall = true;
        this.discardedBitCount = (this.discardedBitCount == null) ? ((new FastInteger(0))) : this.discardedBitCount;
        this.discardedBitCount.AddInt(digits);
        this.bitsAfterLeftmost |= this.bitLeftmost;
        this.bitLeftmost = 0;
        this.knownDigitLength = new FastInteger(1);
        return;
      }
      if (digits >= 2 && digits <= 8) {
        if (shiftedLong >= ValueTenPowers[digits]) {
          long bigPower = ValueTenPowers[digits];
          long smallPower = ValueTenPowers[digits - 1];
          this.discardedBitCount = (this.discardedBitCount == null) ? ((new
              FastInteger(0))) : this.discardedBitCount;
          this.discardedBitCount.AddInt(digits);
          long div = shiftedLong / bigPower;
          long rem = shiftedLong - (div * bigPower);
          long rem2 = rem / smallPower;
          this.bitLeftmost = (int)rem2;
          this.bitsAfterLeftmost |= ((rem - (rem2 * smallPower)) == 0) ? 0 : 1;
          this.isSmall = div <= Integer.MAX_VALUE;
          if (this.isSmall) {
            this.shiftedSmall = (int)div;
            this.knownDigitLength = (div < 10) ? (new FastInteger(1)) :
              new FastInteger(LongDigitLength(div));
          } else {
            this.shiftedBigInt = EInteger.FromInt64(div);
            this.knownDigitLength = (div < 10) ? (new FastInteger(1)) :
              this.CalcKnownDigitLength();
          }
          return;
        } else if (this.shiftedSmall >= ValueTenPowers[digits - 1]) {
          int smallPower = ValueTenPowers[digits - 1];
          if (this.discardedBitCount != null) {
            this.discardedBitCount.AddInt(digits);
          } else {
            this.discardedBitCount = new FastInteger(digits);
          }
          long rem = shiftedLong;
          long rem2 = rem / smallPower;
          this.bitLeftmost = (int)rem2;
          this.bitsAfterLeftmost |= ((rem - (rem2 * smallPower)) == 0) ? 0 : 1;
          this.isSmall = true;
          this.shiftedSmall = 0;
          this.knownDigitLength = new FastInteger(1);
          return;
        } else {
          if (this.discardedBitCount != null) {
            this.discardedBitCount.AddInt(digits);
          } else {
            this.discardedBitCount = new FastInteger(digits);
          }
          this.bitLeftmost = 0;
          this.bitsAfterLeftmost |= (shiftedLong == 0) ? 0 : 1;
          this.isSmall = true;
          this.shiftedSmall = 0;
          this.knownDigitLength = new FastInteger(1);
          return;
        }
      }
      this.knownDigitLength = new FastInteger(
        LongDigitLength(shiftedLong));
      if (this.discardedBitCount != null) {
        this.discardedBitCount.AddInt(digits);
      } else {
        this.discardedBitCount = new FastInteger(digits);
      }
      int digitsShifted = 0;
      while (digits > 0) {
        if (shiftedLong == 0) {
          this.bitsAfterLeftmost |= this.bitLeftmost;
          this.bitLeftmost = 0;
          break;
        } else {
        long newShift = (shiftedLong < 43698) ? ((shiftedLong * 26215) >>
            18) : (shiftedLong / 10);
          int digit = (int)(shiftedLong - (newShift * 10));
          this.bitsAfterLeftmost |= this.bitLeftmost;
          this.bitLeftmost = digit;
          --digits;
          ++digitsShifted;
          shiftedLong = newShift;
        }
      }
      this.isSmall = shiftedLong <= Integer.MAX_VALUE;
      if (this.isSmall) {
        this.shiftedSmall = (int)shiftedLong;
      } else {
        this.shiftedBigInt = EInteger.FromInt64(shiftedLong);
      }
      this.UpdateKnownLengthInt(digitsShifted);
      this.bitsAfterLeftmost = (this.bitsAfterLeftmost != 0) ? 1 : 0;
    }

    private void ShiftRightSmall(int digits) {
      if (digits <= 0) {
        return;
      }
      if (this.shiftedSmall == 0) {
        this.discardedBitCount = (this.discardedBitCount == null) ? ((new FastInteger(0))) : this.discardedBitCount;
        this.discardedBitCount.AddInt(digits);
        this.bitsAfterLeftmost |= this.bitLeftmost;
        this.bitLeftmost = 0;
        this.knownDigitLength = new FastInteger(1);
        return;
      }
      if (digits >= 2 && digits <= 8) {
        if (this.shiftedSmall >= ValueTenPowers[digits]) {
          int bigPower = ValueTenPowers[digits];
          int smallPower = ValueTenPowers[digits - 1];
          this.discardedBitCount = (this.discardedBitCount == null) ? ((new
              FastInteger(0))) : this.discardedBitCount;
          this.discardedBitCount.AddInt(digits);
          int div = this.shiftedSmall / bigPower;
          int rem = this.shiftedSmall - (div * bigPower);
          int rem2 = rem / smallPower;
          this.bitLeftmost = rem2;
          this.bitsAfterLeftmost |= rem - (rem2 * smallPower);
          this.shiftedSmall = div;
          this.knownDigitLength = (div < 10) ? (new FastInteger(1)) :
            this.CalcKnownDigitLength();
          return;
        } else if (this.shiftedSmall >= ValueTenPowers[digits - 1]) {
          int smallPower = ValueTenPowers[digits - 1];
          if (this.discardedBitCount != null) {
            this.discardedBitCount.AddInt(digits);
          } else {
            this.discardedBitCount = new FastInteger(digits);
          }
          int rem = this.shiftedSmall;
          int rem2 = rem / smallPower;
          this.bitLeftmost = rem2;
          this.bitsAfterLeftmost |= rem - (rem2 * smallPower);
          this.shiftedSmall = 0;
          this.knownDigitLength = new FastInteger(1);
          return;
        } else {
          if (this.discardedBitCount != null) {
            this.discardedBitCount.AddInt(digits);
          } else {
            this.discardedBitCount = new FastInteger(digits);
          }
          int rem = this.shiftedSmall;
          this.bitLeftmost = 0;
          this.bitsAfterLeftmost |= rem;
          this.shiftedSmall = 0;
          this.knownDigitLength = new FastInteger(1);
          return;
        }
      }
      int v2 = this.shiftedSmall;
      int kb = (v2 >= 1000000000) ? 10 : ((v2 >= 100000000) ? 9 : ((v2 >=
      10000000) ? 8 : ((v2 >= 1000000) ? 7 : ((v2 >= 100000) ? 6 : ((v2 >=
      10000) ? 5 : ((v2 >= 1000) ? 4 : ((v2 >= 100) ? 3 : ((v2 >= 10) ? 2 :
      1))))))));
      this.knownDigitLength = new FastInteger(kb);
      if (this.discardedBitCount != null) {
        this.discardedBitCount.AddInt(digits);
      } else {
        this.discardedBitCount = new FastInteger(digits);
      }
      int digitsShifted = 0;
      while (digits > 0) {
        if (this.shiftedSmall == 0) {
          this.bitsAfterLeftmost |= this.bitLeftmost;
          this.bitLeftmost = 0;
          this.knownDigitLength = new FastInteger(1);
          break;
        } else {
          int digit = (int)(this.shiftedSmall % 10);
          this.bitsAfterLeftmost |= this.bitLeftmost;
          this.bitLeftmost = digit;
          --digits;
          ++digitsShifted;
          this.shiftedSmall /= 10;
        }
      }
      this.UpdateKnownLengthInt(digitsShifted);
      this.bitsAfterLeftmost = (this.bitsAfterLeftmost != 0) ? 1 : 0;
    }

    private void ShiftToDigitsBig(int digits, boolean truncate) {
      // Shifts a number until it reaches the given number of digits,
      // gathering information on whether the last digit discarded is set
      // and whether the discarded digits to the right of that digit are set.
      // Assumes that the big integer being shifted is positive.
      if (this.knownDigitLength != null) {
        if (this.knownDigitLength.CompareToInt(digits) <= 0) {
          return;
        }
      }
      String str;
  this.knownDigitLength = (this.knownDigitLength == null) ? (this.CalcKnownDigitLength()) : this.knownDigitLength;
      if (this.knownDigitLength.CompareToInt(digits) <= 0) {
        return;
      }
      FastInteger digitDiff = this.knownDigitLength.Copy().SubtractInt(digits);
      if (truncate && digitDiff.CanFitInInt32()) {
        this.TruncateRight(digitDiff);
        return;
      }
      if (digitDiff.CompareToInt(1) == 0) {
        EInteger bigrem;
        EInteger bigquo;
        EInteger[] divrem = this.shiftedBigInt.DivRem(ValueTen);
        bigquo = divrem[0];
        bigrem = divrem[1];
        this.bitsAfterLeftmost |= this.bitLeftmost;
        this.bitLeftmost = bigrem.ToInt32Checked();
        this.shiftedBigInt = bigquo;
        this.discardedBitCount = (this.discardedBitCount == null) ? ((new FastInteger(0))) : this.discardedBitCount;
        this.discardedBitCount.Add(digitDiff);
       this.UpdateKnownLength(digitDiff);
        this.bitsAfterLeftmost = (this.bitsAfterLeftmost != 0) ? 1 : 0;
        return;
      }
      if (digitDiff.CompareToInt(9) <= 0) {
        EInteger bigrem;
        int diffInt = digitDiff.AsInt32();
        EInteger radixPower = NumberUtility.FindPowerOfTen(diffInt);
        EInteger bigquo;
        EInteger[] divrem = this.shiftedBigInt.DivRem(radixPower);
        bigquo = divrem[0];
        bigrem = divrem[1];
        int rem = bigrem.ToInt32Checked();
        this.bitsAfterLeftmost |= this.bitLeftmost;
        for (int i = 0; i < diffInt; ++i) {
          if (i == diffInt - 1) {
            this.bitLeftmost = rem % 10;
          } else {
            int intQuot = (rem < 43698) ? ((rem * 26215) >> 18) : (rem / 10);
            this.bitsAfterLeftmost |= rem - (intQuot * 10);
            rem = intQuot;
          }
        }
        this.shiftedBigInt = bigquo;
        this.discardedBitCount = (this.discardedBitCount == null) ? ((new FastInteger(0))) : this.discardedBitCount;
        this.discardedBitCount.Add(digitDiff);
        this.UpdateKnownLength(digitDiff);
        this.bitsAfterLeftmost = (this.bitsAfterLeftmost != 0) ? 1 : 0;
        return;
      }
      if (digitDiff.CanFitInInt32()) {
        EInteger bigrem = null;
        EInteger bigquo;
        EInteger[] divrem;
        if (!this.shiftedBigInt.isEven() || this.bitsAfterLeftmost != 0) {
          EInteger radixPower =
          NumberUtility.FindPowerOfTen(digitDiff.AsInt32() - 1);
          this.bitsAfterLeftmost |= 1;
          bigquo = this.shiftedBigInt.Divide(radixPower);
        } else {
          EInteger radixPower =
          NumberUtility.FindPowerOfTen(digitDiff.AsInt32() - 1);
          divrem = this.shiftedBigInt.DivRem(radixPower);
          bigquo = divrem[0];
          bigrem = divrem[1];
          this.bitsAfterLeftmost |= this.bitLeftmost;
          if (!bigrem.isZero()) {
            this.bitsAfterLeftmost |= 1;
          }
        }
        EInteger bigquo2;
        divrem = bigquo.DivRem(ValueTen);
        bigquo2 = divrem[0];
        bigrem = divrem[1];
        this.bitLeftmost = bigrem.ToInt32Checked();
        this.shiftedBigInt = bigquo2;
        this.discardedBitCount = (this.discardedBitCount == null) ? ((new FastInteger(0))) : this.discardedBitCount;
        this.discardedBitCount.Add(digitDiff);
        this.UpdateKnownLength(digitDiff);
        this.bitsAfterLeftmost = (this.bitsAfterLeftmost != 0) ? 1 : 0;
        return;
      }
      str = this.shiftedBigInt.toString();
      // NOTE: Will be 1 if the value is 0
      int digitLength = str.length();
      this.knownDigitLength = new FastInteger(digitLength);
      // Shift by the difference in digit length
      if (digitLength > digits) {
        int digitShift = digitLength - digits;
        this.UpdateKnownLengthInt(digitShift);
        int newLength = (int)(digitLength - digitShift);
        // System.out.println("dlen= " + digitLength + " dshift=" +
        // digitShift + " newlen= " + newLength);
        this.discardedBitCount = (this.discardedBitCount == null) ? ((new FastInteger(0))) : this.discardedBitCount;
        if (digitShift <= Integer.MAX_VALUE) {
          this.discardedBitCount.AddInt((int)digitShift);
        } else {
          this.discardedBitCount.AddBig(EInteger.FromInt32(digitShift));
        }
        for (int i = str.length() - 1; i >= 0; --i) {
          this.bitsAfterLeftmost |= this.bitLeftmost;
          this.bitLeftmost = (int)(str.charAt(i) - '0');
          --digitShift;
          if (digitShift <= 0) {
            break;
          }
        }
        if (newLength <= 9) {
          this.isSmall = true;
          this.shiftedSmall = FastParseLong(str, 0, newLength);
        } else {
          this.shiftedBigInt = EInteger.FromSubstring(str, 0, newLength);
        }
        this.bitsAfterLeftmost = (this.bitsAfterLeftmost != 0) ? 1 : 0;
      }
    }

    private void ShiftToDigitsSmall(int digits) {
      int kb = 0;
      int v2 = this.shiftedSmall;
      kb = (v2 >= 1000000000) ? 10 : ((v2 >= 100000000) ? 9 : ((v2 >=
      10000000) ? 8 : ((v2 >= 1000000) ? 7 : ((v2 >= 100000) ? 6 : ((v2 >=
      10000) ? 5 : ((v2 >= 1000) ? 4 : ((v2 >= 100) ? 3 : ((v2 >= 10) ? 2 :
      1))))))));
      this.knownDigitLength = new FastInteger(kb);
      if (kb > digits) {
        int digitShift = (int)(kb - digits);
        this.UpdateKnownLengthInt(digitShift);
        this.discardedBitCount = this.discardedBitCount != null ?
          this.discardedBitCount.AddInt(digitShift) :
          (new FastInteger(digitShift));
        for (int i = 0; i < digitShift; ++i) {
          int digit = (int)(this.shiftedSmall % 10);
          this.shiftedSmall /= 10;
          this.bitsAfterLeftmost |= this.bitLeftmost;
          this.bitLeftmost = digit;
        }
        this.bitsAfterLeftmost = (this.bitsAfterLeftmost != 0) ? 1 : 0;
      }
    }

    private void TruncateRightLong(long shiftedLong, int digits) {
      if (digits <= 0) {
        return;
      }
      if (shiftedLong == 0 || digits >= 21) {
        this.discardedBitCount = (this.discardedBitCount == null) ? ((new FastInteger(0))) : this.discardedBitCount;
        this.discardedBitCount.AddInt(digits);
        this.bitsAfterLeftmost |= this.bitLeftmost;
        this.bitLeftmost = shiftedLong == 0 ? 0 : 1;
        this.shiftedSmall = 0;
        this.isSmall = true;
        this.knownDigitLength = new FastInteger(1);
        return;
      }
      if (digits >= 1 && digits <= TenPowersLong.length - 1) {
        if (shiftedLong >= TenPowersLong[digits]) {
          long bigPower = TenPowersLong[digits];
          if (this.discardedBitCount != null) {
            this.discardedBitCount.AddInt(digits);
          } else {
            this.discardedBitCount = new FastInteger(digits);
          }
          long quo = shiftedLong / bigPower;
          this.bitsAfterLeftmost |= this.bitLeftmost;
          this.bitLeftmost = (shiftedLong & 1) == 1 ? 1 :
            (shiftedLong - (quo * bigPower) == 0 ? 0 : 1);
          shiftedLong = quo;
          this.isSmall = shiftedLong <= Integer.MAX_VALUE;
          if (this.isSmall) {
            this.shiftedSmall = (int)shiftedLong;
          } else {
            this.shiftedBigInt = EInteger.FromInt64(shiftedLong);
          }
          this.UpdateKnownLengthInt(digits);
          return;
        } else {
          if (this.discardedBitCount != null) {
            this.discardedBitCount.AddInt(digits);
          } else {
            this.discardedBitCount = new FastInteger(digits);
          }
          this.bitsAfterLeftmost |= this.bitLeftmost;
          this.bitLeftmost = shiftedLong == 0 ? 0 : 1;
          shiftedLong = 0;
          this.isSmall = shiftedLong <= Integer.MAX_VALUE;
          if (this.isSmall) {
            this.shiftedSmall = (int)shiftedLong;
          } else {
            this.shiftedBigInt = EInteger.FromInt64(shiftedLong);
          }
          this.UpdateKnownLengthInt(digits);
          return;
        }
      }
      this.ShiftRightInt(digits);
    }

    private void TruncateRightSmall(int digits) {
      if (digits <= 0) {
        return;
      }
      if (this.shiftedSmall == 0 || digits >= 11) {
        this.discardedBitCount = (this.discardedBitCount == null) ? ((new FastInteger(0))) : this.discardedBitCount;
        this.discardedBitCount.AddInt(digits);
        this.bitsAfterLeftmost |= this.bitLeftmost;
        this.bitLeftmost = (this.shiftedSmall == 0) ? 0 : 1;
        this.shiftedSmall = 0;
        this.knownDigitLength = new FastInteger(1);
        return;
      }
      if (digits >= 1 && digits <= 8) {
        if (this.shiftedSmall >= ValueTenPowers[digits]) {
          int bigPower = ValueTenPowers[digits];
          if (this.discardedBitCount != null) {
            this.discardedBitCount.AddInt(digits);
          } else {
            this.discardedBitCount = new FastInteger(digits);
          }
          this.bitsAfterLeftmost |= this.bitLeftmost;
          if ((this.shiftedSmall & 1) == 1) {
            this.bitLeftmost = 1;
            this.shiftedSmall /= bigPower;
          } else {
            int quo = this.shiftedSmall / bigPower;
            int rem = this.shiftedSmall - (quo * bigPower);
            this.shiftedSmall = quo;
            this.bitLeftmost |= (rem == 0) ? 0 : 1;
          }
          this.UpdateKnownLengthInt(digits);
          return;
        } else {
          if (this.discardedBitCount != null) {
            this.discardedBitCount.AddInt(digits);
          } else {
            this.discardedBitCount = new FastInteger(digits);
          }
          this.bitsAfterLeftmost |= this.bitLeftmost;
          this.bitLeftmost = (this.shiftedSmall == 0) ? 0 : 1;
          this.shiftedSmall = 0;
          this.knownDigitLength = new FastInteger(1);
          return;
        }
      }
      this.ShiftRightSmall(digits);
    }
  }
