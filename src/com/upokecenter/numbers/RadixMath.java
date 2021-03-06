package com.upokecenter.numbers;
/*
Written by Peter O. in 2013.
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/
If you like this, you should donate to Peter O.
at: http://peteroupc.github.io/
 */

    /**
     * Encapsulates radix-independent arithmetic.
     * @param <T> Data type for a numeric value in a particular radix.
     */
  class RadixMath<T> implements IRadixMath<T> {
    private static final int IntegerModeFixedScale = 1;
    private static final int IntegerModeRegular = 0;

    private static final int[] BitMasks = {
      0x7fffffff, 0x3fffffff, 0x1fffffff,
      0xfffffff, 0x7ffffff, 0x3ffffff, 0x1ffffff,
      0xffffff, 0x7fffff, 0x3fffff, 0x1fffff,
      0xfffff, 0x7ffff, 0x3ffff, 0x1ffff,
      0xffff, 0x7fff, 0x3fff, 0x1fff,
      0xfff, 0x7ff, 0x3ff, 0x1ff,
      0xff, 0x7f, 0x3f, 0x1f,
      0xf, 0x7, 0x3, 0x1
    };

    private static final long[] BitMasks64 = {
      0x7fffffffffffffffL, 0x3fffffffffffffffL, 0x1fffffffffffffffL,
  0xfffffffffffffffL, 0x7ffffffffffffffL, 0x3ffffffffffffffL,
        0x1ffffffffffffffL,
    0xffffffffffffffL, 0x7fffffffffffffL, 0x3fffffffffffffL,
        0x1fffffffffffffL,
      0xfffffffffffffL, 0x7ffffffffffffL, 0x3ffffffffffffL, 0x1ffffffffffffL,
      0xffffffffffffL, 0x7fffffffffffL, 0x3fffffffffffL, 0x1fffffffffffL,
      0xfffffffffffL, 0x7ffffffffffL, 0x3ffffffffffL, 0x1ffffffffffL,
      0xffffffffffL, 0x7fffffffffL, 0x3fffffffffL, 0x1fffffffffL,
      0xfffffffffL, 0x7ffffffffL, 0x3ffffffffL, 0x1ffffffffL,
      0xffffffffL, 0x7fffffff, 0x3fffffff, 0x1fffffff,
      0xfffffff, 0x7ffffff, 0x3ffffff, 0x1ffffff,
      0xffffff, 0x7fffff, 0x3fffff, 0x1fffff,
      0xfffff, 0x7ffff, 0x3ffff, 0x1ffff,
      0xffff, 0x7fff, 0x3fff, 0x1fff,
      0xfff, 0x7ff, 0x3ff, 0x1ff,
      0xff, 0x7f, 0x3f, 0x1f,
      0xf, 0x7, 0x3, 0x1
    };

    private static final int[] OverflowMaxes = {
     2147483647, 214748364, 21474836,
     2147483, 214748, 21474, 2147, 214, 21, 2
    };

    private static final EInteger ValueMinusOne = EInteger.FromInt32(0).Subtract(EInteger.FromInt64(1));

    private static final int[] ValueTenPowers = {
      1, 10, 100, 1000, 10000, 100000,
      1000000, 10000000, 100000000,
      1000000000
    };

    private static final long[] OverflowMaxes64 = {
9223372036854775807L, 922337203685477580L,
  92233720368547758L, 9223372036854775L,
  922337203685477L, 92233720368547L,
  9223372036854L, 922337203685L,
  92233720368L, 9223372036L,
      922337203L, 92233720, 9223372,
      922337, 92233, 9223, 922, 92, 9 };

    private static final long[] ValueTenPowers64 = {
      1, 10, 100, 1000,
      10000, 100000, 1000000,
      10000000, 100000000, 1000000000,
      10000000000L, 100000000000L,
      1000000000000L, 10000000000000L,
      100000000000000L, 1000000000000000L,
      10000000000000000L, 100000000000000000L,
      1000000000000000000L
    };

    private final IRadixMathHelper<T> helper;
    private final int support;
    private final int thisRadix;

    // Conservative maximum base-10 radix power for
    // TryMultiplyByRadix Power; derived from
    // Integer.MAX_VALUE*8/3 (8 is the number of bits in a byte;
    // 3 is a conservative estimate of log(10)/log(2).)
    private static EInteger valueMaxDigits = EInteger.FromInt64(5726623058L);

    public RadixMath(IRadixMathHelper<T> helper) {
      this.helper = helper;
      this.support = helper.GetArithmeticSupport();
      this.thisRadix = helper.GetRadix();
    }

    public T Abs(T value, EContext ctx) {
      int flags = this.helper.GetFlags(value);
      return ((flags & BigNumberFlags.FlagSignalingNaN) != 0) ?
        this.SignalingNaNInvalid(value, ctx) : (
          ((flags & BigNumberFlags.FlagQuietNaN) != 0) ?
          this.ReturnQuietNaN(
  value,
  ctx) : (((flags & BigNumberFlags.FlagNegative) != 0) ? this.RoundToPrecision(
             this.helper.CreateNewWithFlags(this.helper.GetMantissa(value), this.helper.GetExponent(value), flags & ~BigNumberFlags.FlagNegative),
             ctx) : this.RoundToPrecision(
  value,
  ctx)));
    }

    public T Add(T thisValue, T other, EContext ctx) {
      if ((Object)thisValue == null) {
        throw new NullPointerException("thisValue");
      }
      if ((Object)other == null) {
        throw new NullPointerException("other");
      }
      return this.AddEx(thisValue, other, ctx, false);
    }

private FastInteger MaxDigitLengthForBitLength(FastInteger prec) {
  FastInteger result;
  if (this.thisRadix == 2) {
    result = prec;
  } else {
    if (this.thisRadix == 10 && prec.CompareToInt(2135) <= 0) {
      int value = (1 + (((prec.AsInt32() - 1) * 631305) >> 21));
      result = new FastInteger(value);
    } else {
      EInteger valueEInteger = ShiftedMask(prec);
      valueEInteger = valueEInteger.Subtract(EInteger.FromInt32(1));
      IShiftAccumulator shiftAccumulator =
        this.helper.CreateShiftAccumulator(valueEInteger);
      result = shiftAccumulator.GetDigitLength();
    }
  }
  return result;
}

private static EInteger ShiftedMask(FastInteger prec) {
  EInteger bthis = EInteger.FromInt32(1);
  prec = prec.Copy();
  while (prec.signum() > 0) {
    int num = (prec.CompareToInt(1000000) >= 0) ? 1000000 : prec.AsInt32();
    bthis = bthis.ShiftLeft(num);
    prec.SubtractInt(num);
  }
  return bthis.Subtract(EInteger.FromInt32(1));
}

    private boolean IsHigherThanBitLength(EInteger ei, FastInteger prec) {
      return prec.compareTo(FastInteger.FromBig(
        ei.GetUnsignedBitLengthAsEInteger())) < 0;
}

    private T AddEx32Bit(
      int expcmp,
      FastIntegerFixed op1Exponent,
      FastIntegerFixed op1Mantissa,
      FastIntegerFixed op2Exponent,
      FastIntegerFixed op2Mantissa,
      FastIntegerFixed resultExponent,
      int thisFlags,
      int otherFlags,
      EContext ctx) {
      T retval = null;
      if ((expcmp == 0 || (op1Exponent.CanFitInInt32() &&
        op2Exponent.CanFitInInt32())) &&
        op1Mantissa.CanFitInInt32() && op2Mantissa.CanFitInInt32() &&
        (thisFlags & BigNumberFlags.FlagNegative) == (otherFlags &
          BigNumberFlags.FlagNegative)) {
        int negflag = thisFlags & BigNumberFlags.FlagNegative;
        int e1int = 0;
        int e2int = 0;
        if (expcmp != 0) {
          e1int = op1Exponent.AsInt32();
          e2int = op2Exponent.AsInt32();
        }
        int m1, m2;
        boolean haveRetval = false;
     if (expcmp == 0 || (e1int != Integer.MIN_VALUE && e2int !=
          Integer.MIN_VALUE)) {
          int ediff = (expcmp == 0) ? 0 : ((e1int > e2int) ? (e1int - e2int) :
            (e2int - e1int));
          int radix = this.thisRadix;
          if (expcmp == 0) {
            m1 = op1Mantissa.AsInt32();
            m2 = op2Mantissa.AsInt32();
            if (m2 <= Integer.MAX_VALUE - m1) {
              m1 += m2;
              retval = this.helper.CreateNewWithFlagsFastInt(
                new FastIntegerFixed(m1),
                resultExponent,
                negflag);
              haveRetval = true;
            }
          } else if (ediff <= 9 && radix == 10) {
            int power = ValueTenPowers[ediff];
            int maxoverflow = OverflowMaxes[ediff];
            if (expcmp > 0) {
              m1 = op1Mantissa.AsInt32();
              m2 = op2Mantissa.AsInt32();
              if (m1 == 0) {
                retval = this.helper.CreateNewWithFlagsFastInt(
                  op2Mantissa,
                  op2Exponent,
                  otherFlags);
              } else if (m1 <= maxoverflow) {
                m1 *= power;
                if (m2 <= Integer.MAX_VALUE - m1) {
                  m1 += m2;
                  retval = this.helper.CreateNewWithFlagsFastInt(
                    new FastIntegerFixed(m1),
                    resultExponent,
                    negflag);
                  haveRetval = true;
                }
              }
            } else {
              m1 = op1Mantissa.AsInt32();
              m2 = op2Mantissa.AsInt32();
              if (m2 == 0) {
                retval = this.helper.CreateNewWithFlagsFastInt(
                  op1Mantissa,
                  op1Exponent,
                  thisFlags);
              }
              if (m2 <= maxoverflow) {
                m2 *= power;
                if (m1 <= Integer.MAX_VALUE - m2) {
                  m2 += m1;
                  retval = this.helper.CreateNewWithFlagsFastInt(
                    new FastIntegerFixed(m2),
                    resultExponent,
                    negflag);
                  haveRetval = true;
                }
              }
            }
          } else if (ediff <= 30 && radix == 2) {
            int mask = BitMasks[ediff];
            if (expcmp > 0) {
              m1 = op1Mantissa.AsInt32();
              m2 = op2Mantissa.AsInt32();
              if (m1 == 0) {
                retval = this.helper.CreateNewWithFlagsFastInt(
                  op2Mantissa,
                  op2Exponent,
                  otherFlags);
              } else if ((m1 & mask) == m1) {
                m1 <<= ediff;
                if (m2 <= Integer.MAX_VALUE - m1) {
                  m1 += m2;
                  retval = this.helper.CreateNewWithFlagsFastInt(
                    new FastIntegerFixed(m1),
                    resultExponent,
                    negflag);
                  haveRetval = true;
                }
              }
            } else {
              m1 = op1Mantissa.AsInt32();
              m2 = op2Mantissa.AsInt32();
              if (m2 == 0) {
                retval = this.helper.CreateNewWithFlagsFastInt(
                  op1Mantissa,
                  op1Exponent,
                  thisFlags);
              } else if ((m2 & mask) == m2) {
                m2 <<= ediff;
                if (m1 <= Integer.MAX_VALUE - m2) {
                  m2 += m1;
                  retval = this.helper.CreateNewWithFlagsFastInt(
                    new FastIntegerFixed(m2),
                    resultExponent,
                    negflag);
                  haveRetval = true;
                }
              }
            }
          }
        }
        if (haveRetval) {
          if (!IsNullOrSimpleContext(ctx)) {
            retval = this.RoundToPrecision(retval, ctx);
          }
          return retval;
        }
      }
      if ((thisFlags & BigNumberFlags.FlagNegative) != 0 &&
          (otherFlags & BigNumberFlags.FlagNegative) == 0) {
        FastIntegerFixed fftmp;
            fftmp = op1Exponent; op1Exponent = op2Exponent; op2Exponent = fftmp;
            fftmp = op1Mantissa; op1Mantissa = op2Mantissa; op2Mantissa = fftmp;
            int tmp;
            tmp = thisFlags; thisFlags = otherFlags; otherFlags = tmp;
            expcmp = -expcmp;
            resultExponent = expcmp < 0 ? op1Exponent : op2Exponent;
      }
      if ((expcmp == 0 || (op1Exponent.CanFitInInt32() &&
        op2Exponent.CanFitInInt32())) &&
        op1Mantissa.CanFitInInt32() && op2Mantissa.CanFitInInt32() &&
        (thisFlags & BigNumberFlags.FlagNegative) == 0 &&
        (otherFlags & BigNumberFlags.FlagNegative) != 0 &&
        !op2Mantissa.isValueZero() && !op1Mantissa.isValueZero()) {
        int e1int = 0;
        int e2int = 0;
        int result = 0;
        if (expcmp != 0) {
          e1int = op1Exponent.AsInt32();
          e2int = op2Exponent.AsInt32();
        }
        int m1, m2;
        boolean haveRetval = false;
     if (expcmp == 0 || (e1int != Integer.MIN_VALUE && e2int !=
          Integer.MIN_VALUE)) {
          int ediff = (expcmp == 0) ? 0 : ((e1int > e2int) ? (e1int - e2int) :
            (e2int - e1int));
          int radix = this.thisRadix;
          if (expcmp == 0) {
            m1 = op1Mantissa.AsInt32();
            m2 = op2Mantissa.AsInt32();
            if (Integer.MIN_VALUE + m2 <= m1 && m1 >= m2) {
              m1 -= m2;
              result = m1;
              retval = this.helper.CreateNewWithFlagsFastInt(
                new FastIntegerFixed(m1),
                resultExponent,
                0);
              haveRetval = true;
            }
          } else if (radix == 10 && ediff <= 9) {
            int power = ValueTenPowers[ediff];
            int maxoverflow = OverflowMaxes[ediff];
            m1 = op1Mantissa.AsInt32();
            m2 = op2Mantissa.AsInt32();
            boolean negbit = false;
            boolean multed = false;
            if (expcmp < 0) {
              if (m2 <= maxoverflow) {
                m2 *= power;
                multed = true;
              }
            } else {
              if (m1 <= maxoverflow) {
                m1 *= power;
                multed = true;
              }
            }
            if (multed && Integer.MIN_VALUE + m2 <= m1) {
                m1 -= m2;
                if (m1 != Integer.MIN_VALUE) {
                  negbit = m1 < 0;
                  result = Math.abs(m1);
                  retval = this.helper.CreateNewWithFlagsFastInt(
                    new FastIntegerFixed(result),
                    resultExponent,
                    negbit ? BigNumberFlags.FlagNegative : 0);
                  haveRetval = true;
                }
            }
          }
        }
        if (haveRetval && result != 0) {
          if (!IsNullOrSimpleContext(ctx)) {
            retval = this.RoundToPrecision(retval, ctx);
          }
          return retval;
        }
      }
      return null;
    }

    private T AddEx64Bit(
      long expcmp,
      FastIntegerFixed op1Exponent,
      FastIntegerFixed op1Mantissa,
      FastIntegerFixed op2Exponent,
      FastIntegerFixed op2Mantissa,
      FastIntegerFixed resultExponent,
      int thisFlags,
      int otherFlags,
      EContext ctx) {
      T retval = null;
      if ((expcmp == 0 || (op1Exponent.CanFitInInt64() &&
        op2Exponent.CanFitInInt64())) &&
        op1Mantissa.CanFitInInt64() && op2Mantissa.CanFitInInt64() &&
        (thisFlags & BigNumberFlags.FlagNegative) == (otherFlags &
          BigNumberFlags.FlagNegative)) {
        int negflag = thisFlags & BigNumberFlags.FlagNegative;
        long e1long = 0;
        long e2long = 0;
        if (expcmp != 0) {
          e1long = op1Exponent.AsInt64();
          e2long = op2Exponent.AsInt64();
        }
        long m1, m2;
        boolean haveRetval = false;
   if (expcmp == 0 || (e1long != Long.MIN_VALUE && e2long !=
          Long.MIN_VALUE)) {
  long ediffLong = (expcmp == 0) ? 0 : ((e1long > e2long) ? (e1long -
            e2long) : (e2long - e1long));
          int radix = this.thisRadix;
          if (expcmp == 0) {
            m1 = op1Mantissa.AsInt64();
            m2 = op2Mantissa.AsInt64();
            if (m2 <= Long.MAX_VALUE - m1) {
              m1 += m2;
              retval = this.helper.CreateNewWithFlagsFastInt(
                FastIntegerFixed.FromLong(m1),
                resultExponent,
                negflag);
              haveRetval = true;
            }
          } else if (ediffLong < ValueTenPowers64.length && radix == 10) {
            long power = ValueTenPowers64[(int)ediffLong];
            long maxoverflow = OverflowMaxes64[(int)ediffLong];
            if (expcmp > 0) {
              m1 = op1Mantissa.AsInt64();
              m2 = op2Mantissa.AsInt64();
              if (m1 == 0) {
                retval = this.helper.CreateNewWithFlagsFastInt(
                  op2Mantissa,
                  op2Exponent,
                  otherFlags);
              } else if (m1 <= maxoverflow) {
                m1 *= power;
                if (m2 <= Long.MAX_VALUE - m1) {
                  m1 += m2;
                  retval = this.helper.CreateNewWithFlagsFastInt(
                    FastIntegerFixed.FromLong(m1),
                    resultExponent,
                    negflag);
                  haveRetval = true;
                }
              }
            } else {
              m1 = op1Mantissa.AsInt64();
              m2 = op2Mantissa.AsInt64();
              if (m2 == 0) {
                retval = this.helper.CreateNewWithFlagsFastInt(
                  op1Mantissa,
                  op1Exponent,
                  thisFlags);
              }
              if (m2 <= maxoverflow) {
                m2 *= power;
                if (m1 <= Long.MAX_VALUE - m2) {
                  m2 += m1;
                  retval = this.helper.CreateNewWithFlagsFastInt(
                    FastIntegerFixed.FromLong(m2),
                    resultExponent,
                    negflag);
                  haveRetval = true;
                }
              }
            }
          } else if (ediffLong < BitMasks64.length && radix == 2) {
            long mask = BitMasks64[(int)ediffLong];
            if (expcmp > 0) {
              m1 = op1Mantissa.AsInt64();
              m2 = op2Mantissa.AsInt64();
              if (m1 == 0) {
                retval = this.helper.CreateNewWithFlagsFastInt(
                  op2Mantissa,
                  op2Exponent,
                  otherFlags);
              } else if ((m1 & mask) == m1) {
                m1 <<= (int)ediffLong;
                if (m2 <= Long.MAX_VALUE - m1) {
                  m1 += m2;
                  retval = this.helper.CreateNewWithFlagsFastInt(
                    FastIntegerFixed.FromLong(m1),
                    resultExponent,
                    negflag);
                  haveRetval = true;
                }
              }
            } else {
              m1 = op1Mantissa.AsInt64();
              m2 = op2Mantissa.AsInt64();
              if (m2 == 0) {
                retval = this.helper.CreateNewWithFlagsFastInt(
                  op1Mantissa,
                  op1Exponent,
                  thisFlags);
              } else if ((m2 & mask) == m2) {
                m2 <<= (int)ediffLong;
                if (m1 <= Long.MAX_VALUE - m2) {
                  m2 += m1;
                  retval = this.helper.CreateNewWithFlagsFastInt(
                    FastIntegerFixed.FromLong(m2),
                    resultExponent,
                    negflag);
                  haveRetval = true;
                }
              }
            }
          }
        }
        if (haveRetval) {
          if (!IsNullOrSimpleContext(ctx)) {
            retval = this.RoundToPrecision(retval, ctx);
          }
          return retval;
        }
      }
      if ((thisFlags & BigNumberFlags.FlagNegative) != 0 &&
          (otherFlags & BigNumberFlags.FlagNegative) == 0) {
        FastIntegerFixed fftmp;
            fftmp = op1Exponent; op1Exponent = op2Exponent; op2Exponent = fftmp;
            fftmp = op1Mantissa; op1Mantissa = op2Mantissa; op2Mantissa = fftmp;
            int tmp;
            tmp = thisFlags; thisFlags = otherFlags; otherFlags = tmp;
            expcmp = -expcmp;
            resultExponent = expcmp < 0 ? op1Exponent : op2Exponent;
      }
      if ((expcmp == 0 || (op1Exponent.CanFitInInt64() &&
        op2Exponent.CanFitInInt64())) &&
        op1Mantissa.CanFitInInt64() && op2Mantissa.CanFitInInt64() &&
        (thisFlags & BigNumberFlags.FlagNegative) == 0 &&
        (otherFlags & BigNumberFlags.FlagNegative) != 0 &&
        !op2Mantissa.isValueZero() && !op1Mantissa.isValueZero()) {
        long e1long = 0;
        long e2long = 0;
        long result = 0;
        if (expcmp != 0) {
          e1long = op1Exponent.AsInt64();
          e2long = op2Exponent.AsInt64();
        }
        long m1, m2;
        boolean haveRetval = false;
   if (expcmp == 0 || (e1long != Long.MIN_VALUE && e2long !=
          Long.MIN_VALUE)) {
  long ediffLong = (expcmp == 0) ? 0 : ((e1long > e2long) ? (e1long -
            e2long) : (e2long - e1long));
          int radix = this.thisRadix;
          if (expcmp == 0) {
            m1 = op1Mantissa.AsInt64();
            m2 = op2Mantissa.AsInt64();
            if (Long.MIN_VALUE + m2 <= m1 && m1 >= m2) {
              m1 -= m2;
              result = m1;
              retval = this.helper.CreateNewWithFlagsFastInt(
                FastIntegerFixed.FromLong(m1),
                resultExponent,
                0);
              haveRetval = true;
            }
          } else if (radix == 10 && ediffLong < ValueTenPowers64.length) {
            long power = ValueTenPowers64[(int)ediffLong];
            long maxoverflow = OverflowMaxes64[(int)ediffLong];
            m1 = op1Mantissa.AsInt64();
            m2 = op2Mantissa.AsInt64();
            boolean negbit = false;
            boolean multed = false;
            if (expcmp < 0) {
              if (m2 <= maxoverflow) {
                m2 *= power;
                multed = true;
              }
            } else {
              if (m1 <= maxoverflow) {
                m1 *= power;
                multed = true;
              }
            }
            if (multed && Long.MIN_VALUE + m2 <= m1) {
                m1 -= m2;
                if (m1 != Long.MIN_VALUE) {
                  negbit = m1 < 0;
                  result = Math.abs(m1);
                  retval = this.helper.CreateNewWithFlagsFastInt(
                    FastIntegerFixed.FromLong(result),
                    resultExponent,
                    negbit ? BigNumberFlags.FlagNegative : 0);
                  haveRetval = true;
                }
            }
          }
        }
        if (haveRetval && result != 0) {
          if (!IsNullOrSimpleContext(ctx)) {
            retval = this.RoundToPrecision(retval, ctx);
          }
          return retval;
        }
      }
      return null;
    }

    public T AddEx(
  T thisValue,
  T other,
  EContext ctx,
  boolean roundToOperandPrecision) {
      int thisFlags = this.helper.GetFlags(thisValue);
      int otherFlags = this.helper.GetFlags(other);
      if (((thisFlags | otherFlags) & BigNumberFlags.FlagSpecial) != 0) {
        T result = this.HandleNotANumber(thisValue, other, ctx);
        if ((Object)result != (Object)null) {
          return result;
        }
        if ((thisFlags & BigNumberFlags.FlagInfinity) != 0) {
          if ((otherFlags & BigNumberFlags.FlagInfinity) != 0) {
            if ((thisFlags & BigNumberFlags.FlagNegative) != (otherFlags &
  BigNumberFlags.FlagNegative)) {
              return this.SignalInvalid(ctx);
            }
          }
          return thisValue;
        }
        if ((otherFlags & BigNumberFlags.FlagInfinity) != 0) {
          return other;
        }
      }
      FastIntegerFixed op1Exponent = this.helper.GetExponentFastInt(thisValue);
      FastIntegerFixed op2Exponent = this.helper.GetExponentFastInt(other);
      FastIntegerFixed op1Mantissa = this.helper.GetMantissaFastInt(thisValue);
      FastIntegerFixed op2Mantissa = this.helper.GetMantissaFastInt(other);
      int expcmp = op1Exponent.compareTo(op2Exponent);
      FastIntegerFixed resultExponent = expcmp < 0 ? op1Exponent : op2Exponent;
      T retval = null;
      if ((thisFlags & BigNumberFlags.FlagNegative) == 0 &&
        (otherFlags & BigNumberFlags.FlagNegative) == 0) {
        if (expcmp < 0 && op2Mantissa.isValueZero()) {
          return IsNullOrSimpleContext(ctx) ?
           thisValue : this.RoundToPrecision(thisValue, ctx);
        } else if (expcmp >= 0 && op1Mantissa.isValueZero()) {
          return IsNullOrSimpleContext(ctx) ?
           other : this.RoundToPrecision(other, ctx);
        }
      }
      if (!roundToOperandPrecision) {
      retval = this.AddEx32Bit(
  expcmp,
  op1Exponent,
  op1Mantissa,
  op2Exponent,
  op2Mantissa,
  resultExponent,
  thisFlags,
  otherFlags,
  ctx);
      if ((Object)retval != (Object)null) {
 return retval;
}
      retval = this.AddEx64Bit(
  expcmp,
  op1Exponent,
  op1Mantissa,
  op2Exponent,
  op2Mantissa,
  resultExponent,
  thisFlags,
  otherFlags,
  ctx);
      if ((Object)retval != (Object)null) {
 return retval;
}
}
      if (expcmp == 0) {
        retval = this.AddCore2(
          op1Mantissa,
          op2Mantissa,
          op1Exponent,
          thisFlags,
          otherFlags,
          ctx);
        if (!IsNullOrSimpleContext(ctx)) {
          retval = this.RoundToPrecision(retval, ctx);
        }
      } else {
        retval = this.AddExDiffExp(
  thisValue,
  other,
  thisFlags,
  otherFlags,
  ctx,
  expcmp,
  roundToOperandPrecision);
      }
      return retval;
    }

    public int compareTo(T thisValue, T otherValue) {
      if (otherValue == null) {
        return 1;
      }
      int thisFlags = this.helper.GetFlags(thisValue);
      int otherFlags = this.helper.GetFlags(otherValue);
      if (((thisFlags | otherFlags) & BigNumberFlags.FlagSpecial) != 0) {
        return CompareToHandleSpecial2(
  thisValue,
  otherValue,
  thisFlags,
  otherFlags);
      }
      return CompareToInternal(thisValue, otherValue, true, this.helper);
    }

    public T CompareToWithContext(
  T thisValue,
  T otherValue,
  boolean treatQuietNansAsSignaling,
  EContext ctx) {
      if (otherValue == null) {
        return this.SignalInvalid(ctx);
      }
      T result = this.CompareToHandleSpecial(
  thisValue,
  otherValue,
  treatQuietNansAsSignaling,
  ctx);
      if ((Object)result != (Object)null) {
        return result;
      }
      int cmp = CompareToInternal(thisValue, otherValue, false, this.helper);
      return (cmp == -2) ? this.SignalInvalidWithMessage(
        ctx,
        "Out of memory ") :
        this.ValueOf(this.compareTo(thisValue, otherValue), null);
    }

    public T Divide(T thisValue, T divisor, EContext ctx) {
      return this.DivideInternal(
  thisValue,
  divisor,
  ctx,
  IntegerModeRegular,
  EInteger.FromInt32(0));
    }

    public T DivideToExponent(
  T thisValue,
  T divisor,
  EInteger desiredExponent,
  EContext ctx) {
      if (ctx != null && !ctx.ExponentWithinRange(desiredExponent)) {
        return this.SignalInvalidWithMessage(
  ctx,
  "Exponent not within exponent range: " + desiredExponent);
      }
      EContext ctx2 = (ctx == null) ?
        EContext.ForRounding(ERounding.HalfDown) :
        ctx.WithUnlimitedExponents().WithPrecision(0);
      T ret = this.DivideInternal(
  thisValue,
  divisor,
  ctx2,
  IntegerModeFixedScale,
  desiredExponent);
      if (!ctx2.getHasMaxPrecision() && this.IsFinite(ret)) {
        // If a precision is given, call Quantize to ensure
        // that the value fits the precision
        ret = this.Quantize(ret, ret, ctx2);
        if ((ctx2.getFlags() & EContext.FlagInvalid) != 0) {
          ctx2.setFlags(EContext.FlagInvalid);
        }
      }
      if (ctx != null && ctx.getHasFlags()) {
        ctx.setFlags(ctx.getFlags()|(ctx2.getFlags()));
      }
      return ret;
    }

    public T DivideToIntegerNaturalScale(
  T thisValue,
  T divisor,
  EContext ctx) {
      FastInteger desiredScale =
        FastInteger.FromBig(this.helper.GetExponent(thisValue))
        .SubtractBig(this.helper.GetExponent(divisor));
      EContext ctx2 =
        EContext.ForRounding(ERounding.Down).WithBigPrecision(ctx == null ?
  EInteger.FromInt32(0) :
ctx.getPrecision()).WithBlankFlags();
      T ret = this.DivideInternal(
  thisValue,
  divisor,
  ctx2,
  IntegerModeFixedScale,
  EInteger.FromInt32(0));
      if ((ctx2.getFlags() & (EContext.FlagInvalid |
                    EContext.FlagDivideByZero)) != 0) {
        if (ctx != null && ctx.getHasFlags()) {
          ctx.setFlags(ctx.getFlags()|(EContext.FlagInvalid | EContext.FlagDivideByZero));
        }
        return ret;
      }
      boolean neg = (this.helper.GetSign(thisValue) < 0) ^
        (this.helper.GetSign(divisor) < 0);
      // Now the exponent's sign can only be 0 or positive
      if (this.helper.GetMantissa(ret).isZero()) {
        // Value is 0, so just change the exponent
        // to the preferred one
        EInteger dividendExp = this.helper.GetExponent(thisValue);
        EInteger divisorExp = this.helper.GetExponent(divisor);
        ret = this.helper.CreateNewWithFlags(
  EInteger.FromInt32(0),
  dividendExp.Subtract(divisorExp),
  this.helper.GetFlags(ret));
      } else {
        if (desiredScale.signum() < 0) {
          // Desired scale is negative, shift left
          desiredScale.Negate();
          EInteger bigmantissa = this.helper.GetMantissa(ret);
          bigmantissa = this.TryMultiplyByRadixPower(bigmantissa, desiredScale);
          if (bigmantissa == null) {
            return this.SignalInvalidWithMessage(
              ctx,
              "Result requires too much memory");
          }
          EInteger exponentDivisor = this.helper.GetExponent(divisor);
          ret = this.helper.CreateNewWithFlags(
  bigmantissa,
  this.helper.GetExponent(thisValue).Subtract(exponentDivisor),
  this.helper.GetFlags(ret));
        } else if (desiredScale.signum() > 0) {
          // Desired scale is positive, shift away zeros
          // but not after scale is reached
          EInteger bigmantissa = this.helper.GetMantissa(ret);
          FastInteger fastexponent =
            FastInteger.FromBig(this.helper.GetExponent(ret));
          EInteger bigradix = EInteger.FromInt32(this.thisRadix);
          while (true) {
            if (desiredScale.compareTo(fastexponent) == 0) {
              break;
            }
            EInteger bigrem;
            EInteger bigquo;
            {
              EInteger[] divrem = bigmantissa.DivRem(bigradix);
              bigquo = divrem[0];
              bigrem = divrem[1];
            }
            if (!bigrem.isZero()) {
              break;
            }
            bigmantissa = bigquo;
            fastexponent.Increment();
          }
          ret = this.helper.CreateNewWithFlags(
            bigmantissa,
            fastexponent.AsEInteger(),
            this.helper.GetFlags(ret));
        }
      }
      if (ctx != null) {
        ret = this.RoundToPrecision(ret, ctx);
      }
      ret = this.EnsureSign(ret, neg);
      return ret;
    }

    public T DivideToIntegerZeroScale(
  T thisValue,
  T divisor,
  EContext ctx) {
      EContext ctx2 = EContext.ForRounding(ERounding.Down)
        .WithBigPrecision(ctx == null ? EInteger.FromInt32(0) :
                    ctx.getPrecision()).WithBlankFlags();
      T ret = this.DivideInternal(
        thisValue,
        divisor,
        ctx2,
        IntegerModeFixedScale,
        EInteger.FromInt32(0));
      if ((ctx2.getFlags() & (EContext.FlagInvalid |
                    EContext.FlagDivideByZero)) != 0) {
        if (ctx.getHasFlags()) {
          ctx.setFlags(ctx.getFlags()|(ctx2.getFlags() & (EContext.FlagInvalid |
                    EContext.FlagDivideByZero)));
        }
        return ret;
      }
      if (ctx != null) {
        ctx2 = ctx.WithBlankFlags().WithUnlimitedExponents();
        ret = this.RoundToPrecision(ret, ctx2);
        if ((ctx2.getFlags() & EContext.FlagRounded) != 0) {
          return this.SignalInvalid(ctx);
        }
      }
      return ret;
    }

    public T Exp(T thisValue, EContext ctx) {
      if (ctx == null) {
        return this.SignalInvalidWithMessage(ctx, "ctx is null");
      }
      if (!ctx.getHasMaxPrecision()) {
        return this.SignalInvalidWithMessage(
  ctx,
  "ctx has unlimited precision");
      }
      int flags = this.helper.GetFlags(thisValue);
      if ((flags & BigNumberFlags.FlagSignalingNaN) != 0) {
        // NOTE: Returning a signaling NaN is independent of
        // rounding mode
        return this.SignalingNaNInvalid(thisValue, ctx);
      }
      if ((flags & BigNumberFlags.FlagQuietNaN) != 0) {
        // NOTE: Returning a quiet NaN is independent of
        // rounding mode
        return this.ReturnQuietNaN(thisValue, ctx);
      }
      EContext ctxCopy = ctx.WithBlankFlags();
      if ((flags & BigNumberFlags.FlagInfinity) != 0) {
        if ((flags & BigNumberFlags.FlagNegative) != 0) {
          T retval = this.helper.CreateNewWithFlags(
            EInteger.FromInt32(0),
            EInteger.FromInt32(0),
            0);
          retval = this.RoundToPrecision(
            retval,
            ctxCopy);
          if (ctx.getHasFlags()) {
            ctx.setFlags(ctx.getFlags()|(ctxCopy.getFlags()));
          }
          return retval;
        }
        return thisValue;
      }
      int sign = this.helper.GetSign(thisValue);
      T one = this.helper.ValueOf(1);
      EInteger guardDigits = this.thisRadix == 2 ? ctx.getPrecision().Add(EInteger.FromInt32(10)) : EInteger.FromInt32(10);
      EContext ctxdiv = SetPrecisionIfLimited(
        ctx,
        ctx.getPrecision().Add(guardDigits))
        .WithRounding(ERounding.OddOrZeroFiveUp).WithBlankFlags();
      if (sign == 0) {
        thisValue = this.RoundToPrecision(one, ctxCopy);
      } else if (sign > 0 && this.compareTo(thisValue, one) <= 0) {
        thisValue = this.ExpInternal(thisValue, ctxdiv.getPrecision(), ctxCopy);
        if (ctx.getHasFlags()) {
          ctx.setFlags(ctx.getFlags()|(EContext.FlagInexact |
            EContext.FlagRounded));
        }
      } else if (sign < 0) {
        // exp(x) = 1.Divide(exp)(-x) where x<0
        T val = this.Exp(this.NegateRaw(thisValue), ctxdiv);
        if ((ctxdiv.getFlags() & EContext.FlagOverflow) != 0 ||
            !this.IsFinite(val)) {
          // Overflow, try again with expanded exponent range
          EInteger newMax;
          ctxdiv.setFlags(0);
          newMax = ctx.getEMax();
          EInteger expdiff = ctx.getEMin();
          expdiff = newMax.Subtract(expdiff);
          newMax = newMax.Add(expdiff);
          ctxdiv = ctxdiv.WithBigExponentRange(ctxdiv.getEMin(), newMax);
          thisValue = this.Exp(this.NegateRaw(thisValue), ctxdiv);
          if ((ctxdiv.getFlags() & EContext.FlagOverflow) != 0) {
            // Still overflowed
            if (ctx.getHasFlags()) {
              ctx.setFlags(ctx.getFlags()|(BigNumberFlags.UnderflowFlags));
            }
            // Return a "subnormal" zero, with fake extra digits to stimulate
            // rounding
            EInteger ctxdivPrec = ctxdiv.getPrecision();
            newMax = ctx.getEMin();
            if (ctx.getAdjustExponent()) {
              newMax = newMax.Subtract(ctxdivPrec);
              newMax = newMax.Add(EInteger.FromInt32(1));
            }
            thisValue = this.helper.CreateNewWithFlags(
              EInteger.FromInt32(0),
              newMax,
              0);
            return this.RoundToPrecisionInternal(
              thisValue,
              0,
              1,
              null,
              false,
              ctx);
          }
        } else {
          thisValue = val;
        }
        thisValue = this.Divide(one, thisValue, ctxCopy);
        // DebugUtility.Log("end= " + thisValue);
        // DebugUtility.Log("endbit "+this.BitMantissa(thisValue));
        if (ctx.getHasFlags()) {
          ctx.setFlags(ctx.getFlags()|(EContext.FlagInexact |
            EContext.FlagRounded));
        }
      } else {
        T intpart = this.Quantize(
          thisValue,
          one,
          EContext.ForRounding(ERounding.Down));
        if (!this.GetHelper().GetExponent(intpart).isZero()) {
          throw new IllegalArgumentException("integer part not zero, as expected");
        }
        if (this.compareTo(thisValue, this.helper.ValueOf(50000)) > 0 &&
            ctx.getHasExponentRange()) {
          // Try to check for overflow quickly
          // Do a trial powering using a lower number than e,
          // and a power of 50000
          this.PowerIntegral(
  this.helper.ValueOf(2),
  EInteger.FromInt64(50000),
  ctxCopy);
          if ((ctxCopy.getFlags() & EContext.FlagOverflow) != 0) {
            // The trial powering caused overflow, so exp will
            // cause overflow as well
            return this.SignalOverflow2(ctx, false);
          }
          ctxCopy.setFlags(0);
          // Now do the same using the integer part of the operand
          // as the power
          this.PowerIntegral(
  this.helper.ValueOf(2),
  this.helper.GetMantissa(intpart),
  ctxCopy);
          if ((ctxCopy.getFlags() & EContext.FlagOverflow) != 0) {
            // The trial powering caused overflow, so exp will
            // cause overflow as well
            return this.SignalOverflow2(ctx, false);
          }
          ctxCopy.setFlags(0);
        }
        T fracpart = this.Add(thisValue, this.NegateRaw(intpart), null);
        ctxdiv = SetPrecisionIfLimited(ctxdiv, ctxdiv.getPrecision().Add(guardDigits))
           .WithBlankFlags();
        fracpart = this.Add(one, this.Divide(fracpart, intpart, ctxdiv), null);
        ctxdiv.setFlags(0);
        // DebugUtility.Log("fracpart=" + fracpart);
        EInteger workingPrec = ctxdiv.getPrecision();
        workingPrec = workingPrec.Add(EInteger.FromInt64(17));
        thisValue = this.ExpInternal(fracpart, workingPrec, ctxdiv);
        // DebugUtility.Log("thisValue=" + thisValue);
        if ((ctxdiv.getFlags() & EContext.FlagUnderflow) != 0) {
          if (ctx.getHasFlags()) {
            ctx.setFlags(ctx.getFlags()|(ctxdiv.getFlags()));
          }
        }
        if (ctx.getHasFlags()) {
          ctx.setFlags(ctx.getFlags()|(EContext.FlagInexact |
            EContext.FlagRounded));
        }
        thisValue = this.PowerIntegral(
  thisValue,
  this.helper.GetMantissa(intpart),
  ctxCopy);
      }
      if (ctx.getHasFlags()) {
        ctx.setFlags(ctx.getFlags()|(ctxCopy.getFlags()));
      }
      return thisValue;
    }

    public IRadixMathHelper<T> GetHelper() {
      return this.helper;
    }

    public T Ln(T thisValue, EContext ctx) {
      if (ctx == null) {
        return this.SignalInvalidWithMessage(ctx, "ctx is null");
      }
      if (!ctx.getHasMaxPrecision()) {
        return this.SignalInvalidWithMessage(
  ctx,
  "ctx has unlimited precision");
      }
      int flags = this.helper.GetFlags(thisValue);
      if ((flags & BigNumberFlags.FlagSignalingNaN) != 0) {
        // NOTE: Returning a signaling NaN is independent of
        // rounding mode
        return this.SignalingNaNInvalid(thisValue, ctx);
      }
      if ((flags & BigNumberFlags.FlagQuietNaN) != 0) {
        // NOTE: Returning a quiet NaN is independent of
        // rounding mode
        return this.ReturnQuietNaN(thisValue, ctx);
      }
      int sign = this.helper.GetSign(thisValue);
      if (sign < 0) {
        return this.SignalInvalid(ctx);
      }
      if ((flags & BigNumberFlags.FlagInfinity) != 0) {
        return thisValue;
      }
      EContext ctxCopy = ctx.WithBlankFlags();
      T one = this.helper.ValueOf(1);
      if (sign == 0) {
        return this.helper.CreateNewWithFlags(
  EInteger.FromInt32(0),
  EInteger.FromInt32(0),
  BigNumberFlags.FlagNegative | BigNumberFlags.FlagInfinity);
      } else {
        int cmpOne = this.compareTo(thisValue, one);
        EContext ctxdiv = null;
        if (cmpOne == 0) {
          // Equal to 1
          thisValue = this.RoundToPrecision(
           this.helper.CreateNewWithFlags(EInteger.FromInt32(0), EInteger.FromInt32(0), 0),
           ctxCopy);
        } else if (cmpOne < 0) {
          // Less than 1
          FastInteger error = new FastInteger(10);
          EInteger bigError = error.AsEInteger();
          ctxdiv = SetPrecisionIfLimited(ctx, ctx.getPrecision().Add(bigError))
            .WithRounding(ERounding.OddOrZeroFiveUp).WithBlankFlags();
          T quarter = this.Divide(one, this.helper.ValueOf(4), ctxCopy);
          if (this.compareTo(thisValue, quarter) <= 0) {
            // One quarter or less
            T half = this.Multiply(quarter, this.helper.ValueOf(2), null);
            FastInteger roots = new FastInteger(0);
            // Take square root until this value
            // is one half or more
            while (this.compareTo(thisValue, half) < 0) {
              thisValue = this.SquareRoot(
  thisValue,
  ctxdiv.WithUnlimitedExponents());
              roots.Increment();
            }
            thisValue = this.LnInternal(thisValue, ctxdiv.getPrecision(), ctxdiv);
            EInteger bigintRoots = PowerOfTwo(roots);
            // Multiply back 2^X, where X is the number
            // of square root calls
            thisValue = this.Multiply(
  thisValue,
  this.helper.CreateNewWithFlags(bigintRoots, EInteger.FromInt32(0), 0),
  ctxCopy);
          } else {
            T smallfrac = this.Divide(one, this.helper.ValueOf(16), ctxdiv);
            T closeToOne = this.Add(one, this.NegateRaw(smallfrac), null);
            if (this.compareTo(thisValue, closeToOne) >= 0) {
              // This value is close to 1, so use a higher working precision
              error =

  this.helper.CreateShiftAccumulator(this.helper.GetMantissa(thisValue))
                .GetDigitLength();
              error = error.Copy();
              error.AddInt(6);
              error.AddBig(ctx.getPrecision());
              bigError = error.AsEInteger();
              thisValue = this.LnInternal(
  thisValue,
  error.AsEInteger(),
  ctxCopy);
            } else {
              thisValue = this.LnInternal(thisValue, ctxdiv.getPrecision(), ctxCopy);
            }
          }
          if (ctx.getHasFlags()) {
            ctxCopy.setFlags(ctxCopy.getFlags()|(EContext.FlagInexact));
            ctxCopy.setFlags(ctxCopy.getFlags()|(EContext.FlagRounded));
          }
        } else {
          // Greater than 1
          T two = this.helper.ValueOf(2);
          if (this.compareTo(thisValue, two) >= 0) {
            FastInteger roots = new FastInteger(0);
            FastInteger error;
            EInteger bigError;
            error = new FastInteger(10);
            bigError = error.AsEInteger();
            ctxdiv = SetPrecisionIfLimited(ctx, ctx.getPrecision().Add(bigError))
              .WithRounding(ERounding.OddOrZeroFiveUp).WithBlankFlags();
            T smallfrac = this.Divide(one, this.helper.ValueOf(10), ctxdiv);
            T closeToOne = this.Add(one, smallfrac, null);
            // Take square root until this value
            // is close to 1
            while (this.compareTo(thisValue, closeToOne) >= 0) {
              thisValue = this.SquareRoot(
  thisValue,
  ctxdiv.WithUnlimitedExponents());
              roots.Increment();
            }
            // Find -Ln(1/thisValue)
            thisValue = this.Divide(one, thisValue, ctxdiv);
            thisValue = this.LnInternal(thisValue, ctxdiv.getPrecision(), ctxdiv);
            thisValue = this.NegateRaw(thisValue);
            EInteger bigintRoots = PowerOfTwo(roots);
            // Multiply back 2^X, where X is the number
            // of square root calls
            thisValue = this.Multiply(
  thisValue,
  this.helper.CreateNewWithFlags(bigintRoots, EInteger.FromInt32(0), 0),
  ctxCopy);
          } else {
            FastInteger error;
            EInteger bigError;
            error = new FastInteger(10);
            bigError = error.AsEInteger();
            ctxdiv = SetPrecisionIfLimited(ctx, ctx.getPrecision().Add(bigError))
              .WithRounding(ERounding.OddOrZeroFiveUp).WithBlankFlags();
            T smallfrac = this.Divide(one, this.helper.ValueOf(16), ctxdiv);
            T closeToOne = this.Add(one, smallfrac, null);
            if (this.compareTo(thisValue, closeToOne) < 0) {
              error =

  this.helper.CreateShiftAccumulator(this.helper.GetMantissa(thisValue))
                .GetDigitLength();
              error = error.Copy();
              error.AddInt(6);
              error.AddBig(ctx.getPrecision());
              bigError = error.AsEInteger();
              // Greater than 1 and close to 1, will require a higher working
              // precision
              thisValue = this.LnInternal(
  thisValue,
  error.AsEInteger(),
  ctxCopy);
            } else {
              // Find -Ln(1/thisValue)
              thisValue = this.Divide(one, thisValue, ctxdiv);
              thisValue = this.LnInternal(thisValue, ctxdiv.getPrecision(), ctxCopy);
              thisValue = this.NegateRaw(thisValue);
            }
          }
          if (ctx.getHasFlags()) {
            ctxCopy.setFlags(ctxCopy.getFlags()|(EContext.FlagInexact));
            ctxCopy.setFlags(ctxCopy.getFlags()|(EContext.FlagRounded));
          }
        }
      }
      if (ctx.getHasFlags()) {
        ctx.setFlags(ctx.getFlags()|(ctxCopy.getFlags()));
      }
      return thisValue;
    }

    public T Log10(T thisValue, EContext ctx) {
      if (ctx == null) {
        return this.SignalInvalidWithMessage(ctx, "ctx is null");
      }
      if (!ctx.getHasMaxPrecision()) {
        return this.SignalInvalidWithMessage(
  ctx,
  "ctx has unlimited precision");
      }
      int flags = this.helper.GetFlags(thisValue);
      if ((flags & BigNumberFlags.FlagSignalingNaN) != 0) {
        // NOTE: Returning a signaling NaN is independent of
        // rounding mode
        return this.SignalingNaNInvalid(thisValue, ctx);
      }
      if ((flags & BigNumberFlags.FlagQuietNaN) != 0) {
        // NOTE: Returning a quiet NaN is independent of
        // rounding mode
        return this.ReturnQuietNaN(thisValue, ctx);
      }
      int sign = this.helper.GetSign(thisValue);
      if (sign < 0) {
        return this.SignalInvalid(ctx);
      }
      if ((flags & BigNumberFlags.FlagInfinity) != 0) {
        return thisValue;
      }
      EContext ctxCopy = ctx.WithBlankFlags();
      T one = this.helper.ValueOf(1);
      // DebugUtility.Log("input " + thisValue);
      if (sign == 0) {
        // Result is negative infinity if input is 0
        thisValue = this.RoundToPrecision(
            this.helper.CreateNewWithFlags(EInteger.FromInt32(0), EInteger.FromInt32(0), BigNumberFlags.FlagNegative | BigNumberFlags.FlagInfinity),
            ctxCopy);
      } else if (this.compareTo(thisValue, one) == 0) {
        // Result is 0 if input is 1
        thisValue = this.RoundToPrecision(
            this.helper.CreateNewWithFlags(EInteger.FromInt32(0), EInteger.FromInt32(0), 0),
            ctxCopy);
      } else {
        EInteger exp = this.helper.GetExponent(thisValue);
        EInteger mant = this.helper.GetMantissa(thisValue);
        if (mant.equals(EInteger.FromInt32(1)) && this.thisRadix == 10) {
          // Value is 1 and radix is 10, so the result is the exponent
          thisValue = this.helper.CreateNewWithFlags(
            exp.Abs(),
            EInteger.FromInt32(0),
            exp.signum() < 0 ? BigNumberFlags.FlagNegative : 0);
          thisValue = this.RoundToPrecision(
  thisValue,
  ctxCopy);
        } else {
          EInteger mantissa = this.helper.GetMantissa(thisValue);
          FastInteger expTmp = FastInteger.FromBig(exp);
          EInteger tenBig = EInteger.FromInt32(10);
          while (true) {
            EInteger bigrem;
            EInteger bigquo;
            {
              EInteger[] divrem = mantissa.DivRem(tenBig);
              bigquo = divrem[0];
              bigrem = divrem[1];
            }
            if (!bigrem.isZero()) {
              break;
            }
            mantissa = bigquo;
            expTmp.Increment();
          }
          if (mantissa.compareTo(EInteger.FromInt32(1)) == 0 &&
              (this.thisRadix == 10 || expTmp.signum() == 0 || exp.isZero())) {
            // Value is an integer power of 10
            thisValue = this.helper.CreateNewWithFlags(
              expTmp.AsEInteger().Abs(),
              EInteger.FromInt32(0),
              expTmp.signum() < 0 ? BigNumberFlags.FlagNegative : 0);
            thisValue = this.RoundToPrecision(
                thisValue,
                ctxCopy);
          } else {
            EContext ctxdiv = SetPrecisionIfLimited(
              ctx,
              ctx.getPrecision().Add(EInteger.FromInt32(10)))
              .WithRounding(ERounding.OddOrZeroFiveUp).WithBlankFlags();
            T logNatural = this.Ln(thisValue, ctxdiv);
            T logTen = this.LnTenConstant(ctxdiv);
            thisValue = this.Divide(logNatural, logTen, ctx);
            // Treat result as inexact
            if (ctx.getHasFlags()) {
              ctx.setFlags(ctx.getFlags()|(EContext.FlagInexact |
                EContext.FlagRounded));
            }
          }
        }
      }
      if (ctx.getHasFlags()) {
        ctx.setFlags(ctx.getFlags()|(ctxCopy.getFlags()));
      }
      return thisValue;
    }

    public T Max(T a, T b, EContext ctx) {
      if (a == null) {
        throw new NullPointerException("a");
      }
      if (b == null) {
        throw new NullPointerException("b");
      }
      // Handle infinity and NaN
      T result = this.MinMaxHandleSpecial(a, b, ctx, false, false);
      if ((Object)result != (Object)null) {
        return result;
      }
      int cmp = this.compareTo(a, b);
      if (cmp != 0) {
        return cmp < 0 ? this.RoundToPrecision(b, ctx) :
          this.RoundToPrecision(a, ctx);
      }
      int flagNegA = this.helper.GetFlags(a) & BigNumberFlags.FlagNegative;
      return (flagNegA != (this.helper.GetFlags(b) &
                    BigNumberFlags.FlagNegative)) ? ((flagNegA != 0) ?
                this.RoundToPrecision(b, ctx) : this.RoundToPrecision(a, ctx)) :
        ((flagNegA == 0) ?
         (this.helper.GetExponent(a).compareTo(this.helper.GetExponent(b)) > 0 ?
          this.RoundToPrecision(a, ctx) : this.RoundToPrecision(b, ctx)) :
         (this.helper.GetExponent(a).compareTo(this.helper.GetExponent(b)) > 0 ?
          this.RoundToPrecision(b, ctx) : this.RoundToPrecision(a, ctx)));
    }

    public T MaxMagnitude(T a, T b, EContext ctx) {
      if (a == null) {
        throw new NullPointerException("a");
      }
      if (b == null) {
        throw new NullPointerException("b");
      }
      // Handle infinity and NaN
      T result = this.MinMaxHandleSpecial(a, b, ctx, false, true);
      if ((Object)result != (Object)null) {
        return result;
      }
      int cmp = this.compareTo(this.AbsRaw(a), this.AbsRaw(b));
      return (cmp == 0) ? this.Max(a, b, ctx) : ((cmp > 0) ?
                this.RoundToPrecision(
  a,
  ctx) : this.RoundToPrecision(
  b,
  ctx));
    }

    public T Min(T a, T b, EContext ctx) {
      if (a == null) {
        throw new NullPointerException("a");
      }
      if (b == null) {
        throw new NullPointerException("b");
      }
      // Handle infinity and NaN
      T result = this.MinMaxHandleSpecial(a, b, ctx, true, false);
      if ((Object)result != (Object)null) {
        return result;
      }
      int cmp = this.compareTo(a, b);
      if (cmp != 0) {
        return cmp > 0 ? this.RoundToPrecision(b, ctx) :
          this.RoundToPrecision(a, ctx);
      }
      int signANeg = this.helper.GetFlags(a) & BigNumberFlags.FlagNegative;
      return (signANeg != (this.helper.GetFlags(b) &
                    BigNumberFlags.FlagNegative)) ? ((signANeg != 0) ?
                this.RoundToPrecision(a, ctx) : this.RoundToPrecision(b, ctx)) :
        ((signANeg == 0) ?
         (this.helper.GetExponent(a).compareTo(this.helper.GetExponent(b)) > 0 ?
          this.RoundToPrecision(b, ctx) : this.RoundToPrecision(a, ctx)) :
         (this.helper.GetExponent(a).compareTo(this.helper.GetExponent(b)) > 0 ?
          this.RoundToPrecision(a, ctx) : this.RoundToPrecision(b, ctx)));
    }

    public T MinMagnitude(T a, T b, EContext ctx) {
      if (a == null) {
        throw new NullPointerException("a");
      }
      if (b == null) {
        throw new NullPointerException("b");
      }
      // Handle infinity and NaN
      T result = this.MinMaxHandleSpecial(a, b, ctx, true, true);
      if ((Object)result != (Object)null) {
        return result;
      }
      int cmp = this.compareTo(this.AbsRaw(a), this.AbsRaw(b));
      return (cmp == 0) ? this.Min(a, b, ctx) : ((cmp < 0) ?
                this.RoundToPrecision(
  a,
  ctx) : this.RoundToPrecision(
  b,
  ctx));
    }

    public T Multiply(T thisValue, T other, EContext ctx) {
      int thisFlags = this.helper.GetFlags(thisValue);
      int otherFlags = this.helper.GetFlags(other);
      if (((thisFlags | otherFlags) & BigNumberFlags.FlagSpecial) != 0) {
        T result = this.HandleNotANumber(thisValue, other, ctx);
        if ((Object)result != (Object)null) {
          return result;
        }
        if ((thisFlags & BigNumberFlags.FlagInfinity) != 0) {
          // Attempt to multiply infinity by 0
          boolean negflag = ((thisFlags & BigNumberFlags.FlagNegative) != 0) ^
            ((otherFlags & BigNumberFlags.FlagNegative) != 0);
          return ((otherFlags & BigNumberFlags.FlagSpecial) == 0 &&
             this.helper.GetMantissa(other).isZero()) ? this.SignalInvalid(ctx) :
            this.EnsureSign(
  thisValue,
  negflag);
        }
        if ((otherFlags & BigNumberFlags.FlagInfinity) != 0) {
          // Attempt to multiply infinity by 0
          boolean negflag = ((thisFlags & BigNumberFlags.FlagNegative) != 0) ^
            ((otherFlags & BigNumberFlags.FlagNegative) != 0);
          return ((thisFlags & BigNumberFlags.FlagSpecial) == 0 &&
                  this.helper.GetMantissa(thisValue).isZero()) ?
            this.SignalInvalid(ctx) : this.EnsureSign(other, negflag);
        }
      }
      EInteger bigintOp2 = this.helper.GetExponent(other);
      EInteger newexp = this.helper.GetExponent(thisValue).Add(bigintOp2);
      EInteger mantissaOp2 = this.helper.GetMantissa(other);
      // DebugUtility.Log("" + (this.helper.GetMantissa(thisValue)) + "," +
      // (this.helper.GetExponent(thisValue)) + " -> " + mantissaOp2 +", " +
      // (bigintOp2));
      thisFlags = (thisFlags & BigNumberFlags.FlagNegative) ^ (otherFlags &
  BigNumberFlags.FlagNegative);
      T ret =
        this.helper.CreateNewWithFlags(
          this.helper.GetMantissa(thisValue).Multiply(mantissaOp2),
          newexp,
          thisFlags);
      if (ctx != null && ctx != EContext.UnlimitedHalfEven) {
        ret = this.RoundToPrecision(ret, ctx);
      }
      return ret;
    }

    public T MultiplyAndAdd(
  T thisValue,
  T multiplicand,
  T augend,
  EContext ctx) {
      if (multiplicand == null) {
        throw new NullPointerException("multiplicand");
      }
      if (augend == null) {
        throw new NullPointerException("augend");
      }
      EContext ctx2 = EContext.UnlimitedHalfEven.WithBlankFlags();
      T ret = this.MultiplyAddHandleSpecial(
  thisValue,
  multiplicand,
  augend,
  ctx);
      if ((Object)ret != (Object)null) {
        return ret;
      }
      T product = this.Multiply(thisValue, multiplicand, ctx2);
      ret = this.Add(product, augend, ctx);
      if (ctx != null && ctx.getHasFlags()) {
        ctx.setFlags(ctx.getFlags()|(ctx2.getFlags()));
      }
      return ret;
    }

    public T Negate(T value, EContext ctx) {
      int flags = this.helper.GetFlags(value);
      if ((flags & BigNumberFlags.FlagSignalingNaN) != 0) {
        return this.SignalingNaNInvalid(value, ctx);
      }
      if ((flags & BigNumberFlags.FlagQuietNaN) != 0) {
        return this.ReturnQuietNaN(value, ctx);
      }
      EInteger mant = this.helper.GetMantissa(value);
      T zero;
      if ((flags & BigNumberFlags.FlagInfinity) == 0 && mant.isZero()) {
        if ((flags & BigNumberFlags.FlagNegative) == 0) {
          // positive 0 minus positive 0 is always positive 0
          zero = this.helper.CreateNewWithFlags(
  mant,
  this.helper.GetExponent(value),
  flags & ~BigNumberFlags.FlagNegative);
          return this.RoundToPrecision(zero, ctx);
        }
        zero = ctx != null && ctx.getRounding() == ERounding.Floor ?
          this.helper.CreateNewWithFlags(
  mant,
  this.helper.GetExponent(value),
  flags | BigNumberFlags.FlagNegative) : this.helper.CreateNewWithFlags(
  mant,
  this.helper.GetExponent(value),
  flags & ~BigNumberFlags.FlagNegative);
        return this.RoundToPrecision(zero, ctx);
      }
      flags ^= BigNumberFlags.FlagNegative;
      return this.RoundToPrecision(
   this.helper.CreateNewWithFlags(mant, this.helper.GetExponent(value), flags),
   ctx);
    }

    public T NextMinus(T thisValue, EContext ctx) {
      if (ctx == null) {
        return this.SignalInvalidWithMessage(ctx, "ctx is null");
      }
      if (!ctx.getHasMaxPrecision()) {
        return this.SignalInvalidWithMessage(
  ctx,
  "ctx has unlimited precision");
      }
      if (!ctx.getHasExponentRange()) {
        return this.SignalInvalidWithMessage(
  ctx,
  "doesn't satisfy ctx.getHasExponentRange()");
      }
      int flags = this.helper.GetFlags(thisValue);
      if ((flags & BigNumberFlags.FlagSignalingNaN) != 0) {
        return this.SignalingNaNInvalid(thisValue, ctx);
      }
      if ((flags & BigNumberFlags.FlagQuietNaN) != 0) {
        return this.ReturnQuietNaN(thisValue, ctx);
      }
      if ((flags & BigNumberFlags.FlagInfinity) != 0) {
        if ((flags & BigNumberFlags.FlagNegative) != 0) {
          return thisValue;
        } else {
          EInteger bigexp2 = ctx.getEMax();
          EInteger bigprec = ctx.getPrecision();
          if (ctx.getAdjustExponent()) {
            bigexp2 = bigexp2.Add(EInteger.FromInt32(1));
            bigexp2 = bigexp2.Subtract(bigprec);
          }
          EInteger overflowMant = this.TryMultiplyByRadixPower(
              EInteger.FromInt32(1),
              FastInteger.FromBig(ctx.getPrecision()));
          if (overflowMant == null) {
            return this.SignalInvalidWithMessage(
              ctx,
              "Result requires too much memory");
          }
          overflowMant = overflowMant.Subtract(EInteger.FromInt32(1));
          return this.helper.CreateNewWithFlags(overflowMant, bigexp2, 0);
        }
      }
      FastInteger minexp = FastInteger.FromBig(ctx.getEMin());
      if (ctx.getAdjustExponent()) {
        minexp.SubtractBig(ctx.getPrecision()).Increment();
      }
      FastInteger bigexp =
        FastInteger.FromBig(this.helper.GetExponent(thisValue));
      if (bigexp.compareTo(minexp) <= 0) {
        // Use a smaller exponent if the input exponent is already
        // very small
        minexp = bigexp.Copy().SubtractInt(2);
      }
      T quantum = this.helper.CreateNewWithFlags(
        EInteger.FromInt32(1),
        minexp.AsEInteger(),
        BigNumberFlags.FlagNegative);
      EContext ctx2;
      ctx2 = ctx.WithRounding(ERounding.Floor);
      return this.Add(thisValue, quantum, ctx2);
    }

    public T NextPlus(T thisValue, EContext ctx) {
      if (ctx == null) {
        return this.SignalInvalidWithMessage(ctx, "ctx is null");
      }
      if (!ctx.getHasMaxPrecision()) {
        return this.SignalInvalidWithMessage(
  ctx,
  "ctx has unlimited precision");
      }
      if (!ctx.getHasExponentRange()) {
        return this.SignalInvalidWithMessage(
  ctx,
  "doesn't satisfy ctx.getHasExponentRange()");
      }
      int flags = this.helper.GetFlags(thisValue);
      if ((flags & BigNumberFlags.FlagSignalingNaN) != 0) {
        return this.SignalingNaNInvalid(thisValue, ctx);
      }
      if ((flags & BigNumberFlags.FlagQuietNaN) != 0) {
        return this.ReturnQuietNaN(thisValue, ctx);
      }
      if ((flags & BigNumberFlags.FlagInfinity) != 0) {
        if ((flags & BigNumberFlags.FlagNegative) != 0) {
          EInteger bigexp2 = ctx.getEMax();
          EInteger bigprec = ctx.getPrecision();
          if (ctx.getAdjustExponent()) {
            bigexp2 = bigexp2.Add(EInteger.FromInt32(1));
            bigexp2 = bigexp2.Subtract(bigprec);
          }
          EInteger overflowMant = this.TryMultiplyByRadixPower(
              EInteger.FromInt32(1),
              FastInteger.FromBig(ctx.getPrecision()));
          if (overflowMant == null) {
            return this.SignalInvalidWithMessage(
              ctx,
              "Result requires too much memory");
          }
          overflowMant = overflowMant.Subtract(EInteger.FromInt32(1));
          return this.helper.CreateNewWithFlags(
  overflowMant,
  bigexp2,
  BigNumberFlags.FlagNegative);
        }
        return thisValue;
      }
      FastInteger minexp = FastInteger.FromBig(ctx.getEMin());
      if (ctx.getAdjustExponent()) {
        minexp.SubtractBig(ctx.getPrecision()).Increment();
      }
      FastInteger bigexp =
        FastInteger.FromBig(this.helper.GetExponent(thisValue));
      if (bigexp.compareTo(minexp) <= 0) {
        // Use a smaller exponent if the input exponent is already
        // very small
        minexp = bigexp.Copy().SubtractInt(2);
      }
      T quantum = this.helper.CreateNewWithFlags(
        EInteger.FromInt32(1),
        minexp.AsEInteger(),
        0);
      EContext ctx2;
      T val = thisValue;
      ctx2 = ctx.WithRounding(ERounding.Ceiling);
      return this.Add(val, quantum, ctx2);
    }

    public T NextToward(T thisValue, T otherValue, EContext ctx) {
      if (ctx == null) {
        return this.SignalInvalidWithMessage(ctx, "ctx is null");
      }
      if (!ctx.getHasMaxPrecision()) {
        return this.SignalInvalidWithMessage(
  ctx,
  "ctx has unlimited precision");
      }
      if (!ctx.getHasExponentRange()) {
        return this.SignalInvalidWithMessage(
  ctx,
  "doesn't satisfy ctx.getHasExponentRange()");
      }
      int thisFlags = this.helper.GetFlags(thisValue);
      int otherFlags = this.helper.GetFlags(otherValue);
      if (((thisFlags | otherFlags) & BigNumberFlags.FlagSpecial) != 0) {
        T result = this.HandleNotANumber(thisValue, otherValue, ctx);
        if ((Object)result != (Object)null) {
          return result;
        }
      }
      EContext ctx2;
      int cmp = this.compareTo(thisValue, otherValue);
      if (cmp == 0) {
        return this.RoundToPrecision(
   this.EnsureSign(thisValue, (otherFlags & BigNumberFlags.FlagNegative) != 0),
   ctx.WithNoFlags());
      } else {
        if ((thisFlags & BigNumberFlags.FlagInfinity) != 0) {
          if ((thisFlags & (BigNumberFlags.FlagInfinity |
                    BigNumberFlags.FlagNegative)) == (otherFlags &
                    (BigNumberFlags.FlagInfinity |
  BigNumberFlags.FlagNegative))) {
            // both values are the same infinity
            return thisValue;
          } else {
            EInteger bigexp2 = ctx.getEMax();
            EInteger bigprec = ctx.getPrecision();
            if (ctx.getAdjustExponent()) {
              bigexp2 = bigexp2.Add(EInteger.FromInt32(1));
              bigexp2 = bigexp2.Subtract(bigprec);
            }
            EInteger overflowMant = this.TryMultiplyByRadixPower(
                EInteger.FromInt32(1),
                FastInteger.FromBig(ctx.getPrecision()));
            if (overflowMant == null) {
              return this.SignalInvalidWithMessage(
                ctx,
                "Result requires too much memory");
            }
            overflowMant = overflowMant.Subtract(EInteger.FromInt32(1));
            return this.helper.CreateNewWithFlags(
  overflowMant,
  bigexp2,
  thisFlags & BigNumberFlags.FlagNegative);
          }
        }
        FastInteger minexp = FastInteger.FromBig(ctx.getEMin());
        if (ctx.getAdjustExponent()) {
          minexp.SubtractBig(ctx.getPrecision()).Increment();
        }
        FastInteger bigexp =
          FastInteger.FromBig(this.helper.GetExponent(thisValue));
        if (bigexp.compareTo(minexp) < 0) {
          // Use a smaller exponent if the input exponent is already
          // very small
          minexp = bigexp.Copy().SubtractInt(2);
        } else {
          // Ensure the exponent is lower than the exponent range
          // (necessary to flag underflow correctly)
          minexp.SubtractInt(2);
        }
        T quantum = this.helper.CreateNewWithFlags(
          EInteger.FromInt32(1),
          minexp.AsEInteger(),
          (cmp > 0) ? BigNumberFlags.FlagNegative : 0);
        T val = thisValue;
        ctx2 = ctx.WithRounding((cmp > 0) ? ERounding.Floor :
                    ERounding.Ceiling).WithBlankFlags();
        val = this.Add(val, quantum, ctx2);
        if ((ctx2.getFlags() & (EContext.FlagOverflow |
                    EContext.FlagUnderflow)) == 0) {
          // Don't set flags except on overflow or underflow,
          // in accordance with the DecTest test cases
          ctx2.setFlags(0);
        }
        if ((ctx2.getFlags() & EContext.FlagUnderflow) != 0) {
          EInteger bigmant = this.helper.GetMantissa(val);
          EInteger maxmant = this.TryMultiplyByRadixPower(
            EInteger.FromInt32(1),
            FastInteger.FromBig(ctx.getPrecision()).Decrement());
          if (maxmant == null) {
            return this.SignalInvalidWithMessage(
              ctx,
              "Result requires too much memory");
          }
          if (bigmant.compareTo(maxmant) >= 0 ||
              ctx.getPrecision().compareTo(EInteger.FromInt32(1)) == 0) {
            // don't treat max-precision results as having underflowed
            ctx2.setFlags(0);
          }
        }
        if (ctx.getHasFlags()) {
          ctx.setFlags(ctx.getFlags()|(ctx2.getFlags()));
        }
        return val;
      }
    }

    public T Pi(EContext ctx) {
      if (ctx == null) {
        return this.SignalInvalidWithMessage(ctx, "ctx is null");
      }
      if (!ctx.getHasMaxPrecision()) {
        return this.SignalInvalidWithMessage(
  ctx,
  "ctx has unlimited precision");
      }
      // Gauss-Legendre algorithm
      T a = this.helper.ValueOf(1);
      EContext ctxdiv = SetPrecisionIfLimited(
        ctx,
        ctx.getPrecision().Add(EInteger.FromInt32(10)))
        .WithRounding(ERounding.OddOrZeroFiveUp);
      T two = this.helper.ValueOf(2);
      T b = this.Divide(a, this.SquareRoot(two, ctxdiv), ctxdiv);
      T four = this.helper.ValueOf(4);
      T half = ((this.thisRadix & 1) == 0) ?
        this.helper.CreateNewWithFlags(
  EInteger.FromInt64(this.thisRadix / 2),
  ValueMinusOne,
  0) : null;
      T t = this.Divide(a, four, ctxdiv);
      boolean more = true;
      int lastCompare = 0;
      int vacillations = 0;
      T lastGuess = null;
      T guess = null;
      EInteger powerTwo = EInteger.FromInt32(1);
      while (more) {
        lastGuess = guess;
        T aplusB = this.Add(a, b, null);
        T newA = (half == null) ? this.Divide(aplusB, two, ctxdiv) :
          this.Multiply(aplusB, half, null);
        T valueAMinusNewA = this.Add(a, this.NegateRaw(newA), null);
        if (!a.equals(b)) {
          T atimesB = this.Multiply(a, b, ctxdiv);
          b = this.SquareRoot(atimesB, ctxdiv);
        }
        a = newA;
        guess = this.Multiply(aplusB, aplusB, null);
        guess = this.Divide(guess, this.Multiply(t, four, null), ctxdiv);
        T newGuess = guess;
        if ((Object)lastGuess != (Object)null) {
          int guessCmp = this.compareTo(lastGuess, newGuess);
          if (guessCmp == 0) {
            more = false;
          } else if ((guessCmp > 0 && lastCompare < 0) || (lastCompare > 0 &&
                    guessCmp < 0)) {
            // Guesses are vacillating
            ++vacillations;
            more &= vacillations <= 3 || guessCmp <= 0;
          }
          lastCompare = guessCmp;
        }
        if (more) {
          T tmpT = this.Multiply(valueAMinusNewA, valueAMinusNewA, null);
          tmpT = this.Multiply(
  tmpT,
  this.helper.CreateNewWithFlags(powerTwo, EInteger.FromInt32(0), 0),
  null);
          t = this.Add(t, this.NegateRaw(tmpT), ctxdiv);
          powerTwo = powerTwo.ShiftLeft(1);
        }
        guess = newGuess;
      }
      return this.RoundToPrecision(guess, ctx);
    }

    public T Plus(T thisValue, EContext context) {
      return this.RoundToPrecisionInternal(
  thisValue,
  0,
  0,
  null,
  true,
  context);
    }

    public T Power(T thisValue, T pow, EContext ctx) {
      T ret = this.HandleNotANumber(thisValue, pow, ctx);
      if ((Object)ret != (Object)null) {
        return ret;
      }
      int thisSign = this.helper.GetSign(thisValue);
      int powSign = this.helper.GetSign(pow);
      int thisFlags = this.helper.GetFlags(thisValue);
      int powFlags = this.helper.GetFlags(pow);
      if (thisSign == 0 && powSign == 0) {
        // Both operands are zero: invalid
        return this.SignalInvalid(ctx);
      }
      if (thisSign < 0 && (powFlags & BigNumberFlags.FlagInfinity) != 0) {
        // This value is negative and power is infinity: invalid
        return this.SignalInvalid(ctx);
      }
      if (thisSign > 0 && (thisFlags & BigNumberFlags.FlagInfinity) == 0 &&
          (powFlags & BigNumberFlags.FlagInfinity) != 0) {
        // Power is infinity and this value is greater than
        // zero and not infinity
        int cmp = this.compareTo(thisValue, this.helper.ValueOf(1));
        if (cmp < 0) {
          // Value is less than 1
          if (powSign < 0) {
            // Power is negative infinity, return positive infinity
            return this.helper.CreateNewWithFlags(
  EInteger.FromInt32(0),
  EInteger.FromInt32(0),
  BigNumberFlags.FlagInfinity);
          }
          // Power is positive infinity, return 0
          return this.RoundToPrecision(
           this.helper.CreateNewWithFlags(EInteger.FromInt32(0), EInteger.FromInt32(0), 0),
           ctx);
        }
        if (cmp == 0) {
          // Extend the precision of the mantissa as much as possible,
          // in the special case that this value is 1
          return this.ExtendPrecision(this.helper.ValueOf(1), ctx);
        }
        // Value is greater than 1
        if (powSign > 0) {
          // Power is positive infinity, return positive infinity
          return pow;
        }
        // Power is negative infinity, return 0
        return this.RoundToPrecision(
            this.helper.CreateNewWithFlags(EInteger.FromInt32(0), EInteger.FromInt32(0), 0),
            ctx);
      }
      EInteger powExponent = this.helper.GetExponent(pow);
      boolean isPowIntegral = powExponent.signum() > 0;
      boolean isPowOdd = false;
      T powInt = null;
      if (!isPowIntegral) {
        powInt = this.Quantize(
      pow,
      this.helper.CreateNewWithFlags(EInteger.FromInt32(0), EInteger.FromInt32(0), 0),
      EContext.ForRounding(ERounding.Down));
        isPowIntegral = this.compareTo(powInt, pow) == 0;
        isPowOdd = !this.helper.GetMantissa(powInt).isEven();
      } else {
        if (powExponent.equals(EInteger.FromInt32(0))) {
          isPowOdd = !this.helper.GetMantissa(powInt).isEven();
        } else if (this.thisRadix % 2 == 0) {
          // Never odd for even radixes
          isPowOdd = false;
        } else {
          powInt = this.Quantize(
  pow,
  this.helper.CreateNewWithFlags(EInteger.FromInt32(0), EInteger.FromInt32(0), 0),
  EContext.ForRounding(ERounding.Down));
          isPowOdd = !this.helper.GetMantissa(powInt).isEven();
        }
      }
      // DebugUtility.Log("pow=" + pow + " powint=" + powInt);
      boolean isResultNegative = (thisFlags & BigNumberFlags.FlagNegative) != 0 &&
        (powFlags & BigNumberFlags.FlagInfinity) == 0 && isPowIntegral &&
        isPowOdd;
      if (thisSign == 0 && powSign != 0) {
        int infinityFlags = (powSign < 0) ? BigNumberFlags.FlagInfinity : 0;
        if (isResultNegative) {
          infinityFlags |= BigNumberFlags.FlagNegative;
        }
        thisValue = this.helper.CreateNewWithFlags(
          EInteger.FromInt32(0),
          EInteger.FromInt32(0),
          infinityFlags);
        if ((infinityFlags & BigNumberFlags.FlagInfinity) == 0) {
          thisValue = this.RoundToPrecision(thisValue, ctx);
        }
        return thisValue;
      }
      if ((!isPowIntegral || powSign < 0) && (ctx == null ||
                    !ctx.getHasMaxPrecision())) {
        String ValueOutputMessage =
            "ctx is null or has unlimited precision, " +
            "and pow's exponent is not an integer or is negative";
        return this.SignalInvalidWithMessage(
  ctx,
  ValueOutputMessage);
      }
      if (thisSign < 0 && !isPowIntegral) {
        return this.SignalInvalid(ctx);
      }
      if ((thisFlags & BigNumberFlags.FlagInfinity) != 0) {
        // This value is infinity
        int negflag = isResultNegative ? BigNumberFlags.FlagNegative : 0;
        return (powSign > 0) ? this.RoundToPrecision(
            this.helper.CreateNewWithFlags(EInteger.FromInt32(0), EInteger.FromInt32(0), negflag | BigNumberFlags.FlagInfinity),
            ctx) : ((powSign < 0) ? this.RoundToPrecision(
     this.helper.CreateNewWithFlags(EInteger.FromInt32(0), EInteger.FromInt32(0), negflag),
     ctx) : this.RoundToPrecision(
            this.helper.CreateNewWithFlags(EInteger.FromInt32(1), EInteger.FromInt32(0), 0),
            ctx));
      }
      if (powSign == 0) {
        return
          this.RoundToPrecision(
            this.helper.CreateNewWithFlags(EInteger.FromInt32(1), EInteger.FromInt32(0), 0),
            ctx);
      }
      if (isPowIntegral) {
        // Special case for 1
        if (this.compareTo(thisValue, this.helper.ValueOf(1)) == 0) {
          return (!this.IsWithinExponentRangeForPow(pow, ctx)) ?
            this.SignalInvalid(ctx) : this.helper.ValueOf(1);
        }
        if ((Object)powInt == (Object)null) {
          powInt = this.Quantize(
  pow,
  this.helper.CreateNewWithFlags(EInteger.FromInt32(0), EInteger.FromInt32(0), 0),
  EContext.ForRounding(ERounding.Down));
        }
        EInteger signedMant = this.helper.GetMantissa(powInt);
        if (powSign < 0) {
          signedMant = signedMant.Negate();
        }
        // DebugUtility.Log("tv=" + thisValue + " mant=" + signedMant);
        return this.PowerIntegral(thisValue, signedMant, ctx);
      }
      // Special case for 1
      if (this.compareTo(thisValue, this.helper.ValueOf(1)) == 0 && powSign >
          0) {
        return (!this.IsWithinExponentRangeForPow(pow, ctx)) ?
          this.SignalInvalid(ctx) :
          this.ExtendPrecision(this.helper.ValueOf(1), ctx);
      }

      // Special case for 0.5
      if (this.thisRadix == 10 || this.thisRadix == 2) {
        T half = (this.thisRadix == 10) ? this.helper.CreateNewWithFlags(
            EInteger.FromInt64(5),
            ValueMinusOne,
            0) : this.helper.CreateNewWithFlags(
  EInteger.FromInt32(1),
  ValueMinusOne,
  0);
        if (this.compareTo(pow, half) == 0 &&
            this.IsWithinExponentRangeForPow(pow, ctx) &&
            this.IsWithinExponentRangeForPow(thisValue, ctx)) {
          EContext ctxCopy = ctx.WithBlankFlags();
          thisValue = this.SquareRoot(thisValue, ctxCopy);
          ctxCopy.setFlags(ctxCopy.getFlags()|(EContext.FlagInexact));
          ctxCopy.setFlags(ctxCopy.getFlags()|(EContext.FlagRounded));
          if ((ctxCopy.getFlags() & EContext.FlagSubnormal) != 0) {
            ctxCopy.setFlags(ctxCopy.getFlags()|(EContext.FlagUnderflow));
          }
          thisValue = this.ExtendPrecision(thisValue, ctxCopy);
          if (ctx.getHasFlags()) {
            ctx.setFlags(ctx.getFlags()|(ctxCopy.getFlags()));
          }
          return thisValue;
        }
      }
      int guardDigitCount = this.thisRadix == 2 ? 32 : 10;
      EInteger guardDigits = EInteger.FromInt32(guardDigitCount);
      EContext ctxdiv = SetPrecisionIfLimited(
        ctx,
        ctx.getPrecision().Add(guardDigits));
      ctxdiv = ctxdiv.WithRounding(ERounding.OddOrZeroFiveUp).WithBlankFlags();
      T lnresult = this.Ln(thisValue, ctxdiv);
      /* DebugUtility.Log("guard= " + guardDigits + " prec=" + ctx.getPrecision()+
        " newprec= " + ctxdiv.getPrecision());
      DebugUtility.Log("pwrIn " + pow);
      DebugUtility.Log("lnIn " + thisValue);
      DebugUtility.Log("lnOut " + lnresult);
      DebugUtility.Log("lnOut.get(n) "+this.NextPlus(lnresult,ctxdiv));*/
      lnresult = this.Multiply(lnresult, pow, ctxdiv);
      // DebugUtility.Log("expIn " + lnresult);
      // Now use original precision and rounding mode
      ctxdiv = ctx.WithBlankFlags();
      lnresult = this.Exp(lnresult, ctxdiv);
      if ((ctxdiv.getFlags() & (EContext.FlagClamped |
                    EContext.FlagOverflow)) != 0) {
        if (!this.IsWithinExponentRangeForPow(thisValue, ctx)) {
          return this.SignalInvalid(ctx);
        }
        if (!this.IsWithinExponentRangeForPow(pow, ctx)) {
          return this.SignalInvalid(ctx);
        }
      }
      if (ctx.getHasFlags()) {
        ctx.setFlags(ctx.getFlags()|(ctxdiv.getFlags()));
      }
      return lnresult;
    }

    private boolean IsSubnormal(T value, EContext ctx) {
  boolean flag = ctx == null || !ctx.getHasMaxPrecision();
  boolean result;
  if (flag) {
    result = false;
  } else {
 FastInteger fastInteger = FastInteger.FromBig(this.helper.GetExponent(value));
    FastInteger val = FastInteger.FromBig(ctx.getEMin());
    boolean adjustExponent = ctx.getAdjustExponent();
    if (adjustExponent) {
      FastIntegerFixed mantissaFastInt = this.helper.GetMantissaFastInt(value);
      FastInteger digitLength =
        this.helper.CreateShiftAccumulatorWithDigitsFastInt(
  mantissaFastInt,
  0,
  0).GetDigitLength();
      fastInteger.Add(digitLength).SubtractInt(1);
    }
    result = fastInteger.compareTo(val) < 0;
  }
  return result;
}

    public T Quantize(T thisValue, T otherValue, EContext ctx) {
      int thisFlags = this.helper.GetFlags(thisValue);
      int otherFlags = this.helper.GetFlags(otherValue);
      if (((thisFlags | otherFlags) & BigNumberFlags.FlagSpecial) != 0) {
        T result = this.HandleNotANumber(thisValue, otherValue, ctx);
        if ((Object)result != (Object)null) {
          return result;
        }
        if (((thisFlags & otherFlags) & BigNumberFlags.FlagInfinity) != 0) {
          return this.RoundToPrecision(thisValue, ctx);
        }
        // At this point, it's only the case that either value
        // is infinity
        return this.SignalInvalid(ctx);
      }
      EInteger expOther = this.helper.GetExponent(otherValue);
      if (ctx != null && !ctx.ExponentWithinRange(expOther)) {
        // DebugUtility.Log("exp not within range");
        return this.SignalInvalidWithMessage(
  ctx,
  "Exponent not within exponent range: " + expOther);
      }
      EContext tmpctx = (ctx == null ?
  EContext.ForRounding(ERounding.HalfEven) :
                    ctx.Copy()).WithBlankFlags();
      EInteger mantThis = this.helper.GetMantissa(thisValue);
      EInteger expThis = this.helper.GetExponent(thisValue);
      int expcmp = expThis.compareTo(expOther);
      int negativeFlag = this.helper.GetFlags(thisValue) &
        BigNumberFlags.FlagNegative;
      T ret = null;
      if (expcmp == 0) {
        // DebugUtility.Log("exp same");
        ret = this.RoundToPrecision(thisValue, tmpctx);
      } else if (mantThis.isZero()) {
        // DebugUtility.Log("mant is 0");
        ret = this.helper.CreateNewWithFlags(
  EInteger.FromInt32(0),
  expOther,
  negativeFlag);
        ret = this.RoundToPrecision(ret, tmpctx);
      } else if (expcmp > 0) {
        // Other exponent is less
        // DebugUtility.Log("other exp less");
        FastInteger radixPower =
               FastInteger.FromBig(expThis).SubtractBig(expOther);
        if (tmpctx.getPrecision().signum() > 0 &&
            radixPower.compareTo(FastInteger.FromBig(tmpctx.getPrecision())
                .AddInt(10)) > 0) {
          // Radix power is much too high for the current precision
          // DebugUtility.Log("result too high for prec:" +
          // tmpctx.getPrecision() + " radixPower= " + radixPower);
          return this.SignalInvalidWithMessage(
  ctx,
  "Result too high for current precision");
        }
        mantThis = this.TryMultiplyByRadixPower(mantThis, radixPower);
        if (mantThis == null) {
          return this.SignalInvalidWithMessage(
            ctx,
            "Result requires too much memory");
        }
        ret = this.helper.CreateNewWithFlags(mantThis, expOther, negativeFlag);
        ret = this.RoundToPrecision(ret, tmpctx);
      } else {
        // Other exponent is greater
        // DebugUtility.Log("other exp greater");
        FastInteger shift = FastInteger.FromBig(expOther).SubtractBig(expThis);
        ret = this.RoundToPrecisionInternal(
  thisValue,
  0,
  0,
  shift,
  false,
  tmpctx);
      }
      if ((tmpctx.getFlags() & EContext.FlagOverflow) != 0) {
        // DebugUtility.Log("overflow occurred");
        return this.SignalInvalid(ctx);
      }
      if (ret == null || !this.helper.GetExponent(ret).equals(expOther)) {
        // DebugUtility.Log("exp not same "+ret);
        return this.SignalInvalid(ctx);
      }
      ret = this.EnsureSign(ret, negativeFlag != 0);
      if (ctx != null && ctx.getHasFlags()) {
        int flags = tmpctx.getFlags();
        flags &= ~EContext.FlagUnderflow;
        boolean flag12 = expcmp < 0 && !this.helper.GetMantissa(ret).isZero() &&
          this.IsSubnormal(ret, ctx);
            if (flag12) {
              flags |= EContext.FlagSubnormal;
            }
        ctx.setFlags(ctx.getFlags()|(flags));
      }
      return ret;
    }

    public T Reduce(T thisValue, EContext ctx) {
      return this.ReduceToPrecisionAndIdealExponent(thisValue, ctx, null, null);
    }

    public T Remainder(T thisValue, T divisor, EContext ctx) {
      EContext ctx2 = ctx == null ? null : ctx.WithBlankFlags();
      T ret = this.RemainderHandleSpecial(thisValue, divisor, ctx2);
      if ((Object)ret != (Object)null) {
        TransferFlags(ctx, ctx2);
        return ret;
      }
      ret = this.DivideToIntegerZeroScale(thisValue, divisor, ctx2);
      if ((ctx2.getFlags() & EContext.FlagInvalid) != 0) {
        return this.SignalInvalid(ctx);
      }
      ret = this.Add(
  thisValue,
  this.NegateRaw(this.Multiply(ret, divisor, null)),
  ctx2);
      ret = this.EnsureSign(
    ret,
    (this.helper.GetFlags(thisValue) & BigNumberFlags.FlagNegative) != 0);
      TransferFlags(ctx, ctx2);
      return ret;
    }

    public T RemainderNear(T thisValue, T divisor, EContext ctx) {
      EContext ctx2 = ctx == null ?
        EContext.ForRounding(ERounding.HalfEven).WithBlankFlags() :
        ctx.WithRounding(ERounding.HalfEven).WithBlankFlags();
      T ret = this.RemainderHandleSpecial(thisValue, divisor, ctx2);
      if ((Object)ret != (Object)null) {
        TransferFlags(ctx, ctx2);
        return ret;
      }
      ret = this.DivideInternal(
  thisValue,
  divisor,
  ctx2,
  IntegerModeFixedScale,
  EInteger.FromInt32(0));
      if ((ctx2.getFlags() & EContext.FlagInvalid) != 0) {
        return this.SignalInvalid(ctx);
      }
      ctx2 = ctx2.WithBlankFlags();
      ret = this.RoundToPrecision(ret, ctx2);
      if ((ctx2.getFlags() & (EContext.FlagRounded |
                    EContext.FlagInvalid)) != 0) {
        return this.SignalInvalid(ctx);
      }
      ctx2 = ctx == null ? EContext.UnlimitedHalfEven.WithBlankFlags() :
        ctx.WithBlankFlags();
      T ret2 = this.Add(
        thisValue,
        this.NegateRaw(this.Multiply(ret, divisor, null)),
        ctx2);
      if ((ctx2.getFlags() & EContext.FlagInvalid) != 0) {
        return this.SignalInvalid(ctx);
      }
      if (this.helper.GetFlags(ret2) == 0 &&
             this.helper.GetMantissa(ret2).isZero()) {
        ret2 = this.EnsureSign(
  ret2,
  (this.helper.GetFlags(thisValue) & BigNumberFlags.FlagNegative) != 0);
      }
      TransferFlags(ctx, ctx2);
      return ret2;
    }

    public T RoundAfterConversion(T thisValue, EContext ctx) {
      // DebugUtility.Log("RM RoundAfterConversion");
      return this.RoundToPrecision(thisValue, ctx);
    }

    public T RoundToExponentExact(
  T thisValue,
  EInteger expOther,
  EContext ctx) {
      if (this.helper.GetExponent(thisValue).compareTo(expOther) >= 0) {
        return this.RoundToPrecision(thisValue, ctx);
      } else {
        EContext pctx = (ctx == null) ? null :
          ctx.WithPrecision(0).WithBlankFlags();
        T ret = this.Quantize(
        thisValue,
        this.helper.CreateNewWithFlags(EInteger.FromInt32(1), expOther, 0),
        pctx);
        if (ctx != null && ctx.getHasFlags()) {
          ctx.setFlags(ctx.getFlags()|(pctx.getFlags()));
        }
        return ret;
      }
    }

    public T RoundToExponentNoRoundedFlag(
  T thisValue,
  EInteger exponent,
  EContext ctx) {
      EContext pctx = (ctx == null) ? null : ctx.WithBlankFlags();
      T ret = this.RoundToExponentExact(thisValue, exponent, pctx);
      if (ctx != null && ctx.getHasFlags()) {
        ctx.setFlags(ctx.getFlags()|(pctx.getFlags() & ~(EContext.FlagInexact |
                    EContext.FlagRounded)));
      }
      return ret;
    }

    public T RoundToExponentSimple(
  T thisValue,
  EInteger expOther,
  EContext ctx) {
      int thisFlags = this.helper.GetFlags(thisValue);
      if ((thisFlags & BigNumberFlags.FlagSpecial) != 0) {
        T result = this.HandleNotANumber(thisValue, thisValue, ctx);
        if ((Object)result != (Object)null) {
          return result;
        }
        if ((thisFlags & BigNumberFlags.FlagInfinity) != 0) {
          return thisValue;
        }
      }
      if (this.helper.GetExponent(thisValue).compareTo(expOther) >= 0) {
        return this.RoundToPrecision(thisValue, ctx);
      } else {
        if (ctx != null && !ctx.ExponentWithinRange(expOther)) {
          return this.SignalInvalidWithMessage(
  ctx,
  "Exponent not within exponent range: " + expOther);
        }
        FastInteger shift = FastInteger.FromBig(expOther)
          .SubtractBig(this.helper.GetExponent(thisValue));
        if (shift.signum() == 0 && IsSimpleContext(ctx)) {
          return thisValue;
        }
        EInteger bigmantissa = this.helper.GetMantissa(thisValue);
        IShiftAccumulator accum =
             this.helper.CreateShiftAccumulator(bigmantissa);
        if (IsSimpleContext(ctx) && ctx.getRounding() == ERounding.Down) {
          accum.TruncateRight(shift);
          return this.helper.CreateNewWithFlags(
             accum.getShiftedInt(),
             expOther,
             thisFlags);
        } else {
          accum.ShiftRight(shift);
        }
        bigmantissa = accum.getShiftedInt();
        thisValue = this.helper.CreateNewWithFlags(
          bigmantissa,
          expOther,
          thisFlags);
        return this.RoundToPrecisionInternal(
          thisValue,
          accum.getLastDiscardedDigit(),
          accum.getOlderDiscardedDigits(),
          null,
          false,
          ctx);
      }
    }

    public T RoundToPrecision(T thisValue, EContext context) {
      return this.RoundToPrecisionInternal(
  thisValue,
  0,
  0,
  null,
  false,
  context);
    }

    public T SquareRoot(T thisValue, EContext ctx) {
      if (ctx == null) {
        return this.SignalInvalidWithMessage(ctx, "ctx is null");
      }
      if (!ctx.getHasMaxPrecision()) {
        return this.SignalInvalidWithMessage(
  ctx,
  "ctx has unlimited precision");
      }
      T ret = this.SquareRootHandleSpecial(thisValue, ctx);
      if ((Object)ret != (Object)null) {
        return ret;
      }
      EContext ctxtmp = ctx.WithBlankFlags();
      EInteger currentExp = this.helper.GetExponent(thisValue);
      EInteger origExp = currentExp;
      EInteger idealExp;
      idealExp = currentExp;
      idealExp = idealExp.Divide(EInteger.FromInt64(2));
      if (currentExp.signum() < 0 && !currentExp.isEven()) {
        // Round towards negative infinity; BigInteger's
        // division operation rounds towards zero
        idealExp = idealExp.Subtract(EInteger.FromInt32(1));
      }
      // DebugUtility.Log("curr=" + currentExp + " ideal=" + idealExp);
      if (this.helper.GetSign(thisValue) == 0) {
        ret = this.RoundToPrecision(
            this.helper.CreateNewWithFlags(EInteger.FromInt32(0), idealExp, this.helper.GetFlags(thisValue)),
            ctxtmp);
        if (ctx.getHasFlags()) {
          ctx.setFlags(ctx.getFlags()|(ctxtmp.getFlags()));
        }
        return ret;
      }
      EInteger mantissa = this.helper.GetMantissa(thisValue);
      IShiftAccumulator accum = this.helper.CreateShiftAccumulator(mantissa);
      FastInteger digitCount = accum.GetDigitLength();
      FastInteger targetPrecision = FastInteger.FromBig(ctx.getPrecision());
      FastInteger precision = targetPrecision.Copy().Multiply(2).AddInt(2);
      boolean rounded = false;
      boolean inexact = false;
      if (digitCount.compareTo(precision) < 0) {
        FastInteger diff = precision.Copy().Subtract(digitCount);
        // DebugUtility.Log(diff);
        if ((!diff.isEvenNumber()) ^ (!origExp.isEven())) {
          diff.Increment();
        }
        EInteger bigdiff = diff.AsEInteger();
        currentExp = currentExp.Subtract(bigdiff);
        mantissa = this.TryMultiplyByRadixPower(mantissa, diff);
        if (mantissa == null) {
          return this.SignalInvalidWithMessage(
            ctx,
            "Result requires too much memory");
        }
      }
      EInteger[] sr = mantissa.SqrtRem();
      digitCount = this.helper.CreateShiftAccumulator(sr[0]).GetDigitLength();
      EInteger squareRootRemainder = sr[1];
      // DebugUtility.Log("I " + mantissa + " -> " + sr[0] + " [target="+
      // targetPrecision + "], (zero= " + squareRootRemainder.isZero() +") "
      mantissa = sr[0];
      if (!squareRootRemainder.isZero()) {
        rounded = true;
        inexact = true;
      }
      EInteger oldexp = currentExp;
      currentExp = currentExp.Divide(EInteger.FromInt64(2));
      if (oldexp.signum() < 0 && !oldexp.isEven()) {
        // Round towards negative infinity; BigInteger's
        // division operation rounds towards zero
        currentExp = currentExp.Subtract(EInteger.FromInt32(1));
      }
      T retval = this.helper.CreateNewWithFlags(mantissa, currentExp, 0);
      // DebugUtility.Log("idealExp= " + idealExp + ", curr" + currentExp
      // +" guess= " + mantissa);
      retval = this.RoundToPrecisionInternal(
  retval,
  0,
  inexact ? 1 : 0,
  null,
  false,
  ctxtmp);
      currentExp = this.helper.GetExponent(retval);
      // DebugUtility.Log("guess I " + guess + " idealExp=" + idealExp
      // +", curr " + currentExp + " clamped= " +
      // (ctxtmp.getFlags()&PrecisionContext.FlagClamped));
      if ((ctxtmp.getFlags() & EContext.FlagUnderflow) == 0) {
        int expcmp = currentExp.compareTo(idealExp);
        if (expcmp <= 0 || !this.IsFinite(retval)) {
          retval = this.ReduceToPrecisionAndIdealExponent(
            retval,
            ctx.getHasExponentRange() ? ctxtmp : null,
            inexact ? targetPrecision : null,
            FastInteger.FromBig(idealExp));
        }
      }
      if (ctx.getHasFlags()) {
        if (ctx.getClampNormalExponents() &&
            !this.helper.GetExponent(retval).equals(idealExp) && (ctxtmp.getFlags() &
    EContext.FlagInexact) == 0) {
          ctx.setFlags(ctx.getFlags()|(EContext.FlagClamped));
        }
        rounded |= (ctxtmp.getFlags() & EContext.FlagOverflow) != 0;
        // DebugUtility.Log("guess II " + guess);
        currentExp = this.helper.GetExponent(retval);
        if (rounded) {
          ctxtmp.setFlags(ctxtmp.getFlags()|(EContext.FlagRounded));
        } else {
          if (currentExp.compareTo(idealExp) > 0) {
            // Greater than the ideal, treat as rounded anyway
            ctxtmp.setFlags(ctxtmp.getFlags()|(EContext.FlagRounded));
          } else {
            // DebugUtility.Log("idealExp= " + idealExp + ", curr" +
            // currentExp + " (II)");
            ctxtmp.setFlags(ctxtmp.getFlags()&~(EContext.FlagRounded));
          }
        }
        if (inexact) {
          ctxtmp.setFlags(ctxtmp.getFlags()|(EContext.FlagRounded));
          ctxtmp.setFlags(ctxtmp.getFlags()|(EContext.FlagInexact));
        }
        ctx.setFlags(ctx.getFlags()|(ctxtmp.getFlags()));
      }
      return retval;
    }

    private static int CompareToFast(
  int e1int,
  int e2int,
  int expcmp,
  int signA,
  FastIntegerFixed op1Mantissa,
  FastIntegerFixed op2Mantissa,
  int radix) {
      int m1, m2;
      if (Integer.MIN_VALUE + e2int < e1int) {
        int ediff = (e1int > e2int) ? (e1int - e2int) : (e2int - e1int);
        if (ediff <= 9 && radix == 10) {
          int power = ValueTenPowers[ediff];
          int maxoverflow = OverflowMaxes[ediff];
          if (expcmp > 0) {
            m1 = op1Mantissa.AsInt32();
            m2 = op2Mantissa.AsInt32();
            if (m1 <= maxoverflow) {
              m1 *= power;
              return (m1 == m2) ? 0 : ((m1 < m2) ? -signA : signA);
            }
          } else {
            m1 = op1Mantissa.AsInt32();
            m2 = op2Mantissa.AsInt32();
            if (m2 <= maxoverflow) {
              m2 *= power;
              return (m1 == m2) ? 0 : ((m1 < m2) ? -signA : signA);
            }
          }
        } else if (ediff <= 30 && radix == 2) {
          int mask = BitMasks[ediff];
          if (expcmp > 0) {
            m1 = op1Mantissa.AsInt32();
            m2 = op2Mantissa.AsInt32();
            if ((m1 & mask) == m1) {
              m1 <<= ediff;
              return (m1 == m2) ? 0 : ((m1 < m2) ? -signA : signA);
            }
          } else {
            m1 = op1Mantissa.AsInt32();
            m2 = op2Mantissa.AsInt32();
            if ((m2 & mask) == m2) {
              m2 <<= ediff;
              return (m1 == m2) ? 0 : ((m1 < m2) ? -signA : signA);
            }
          }
        }
      }
      return 2;
    }

    private static <TMath> int CompareToSlow(
  EInteger op1Exponent,
  EInteger op2Exponent,
  int expcmp,
  int signA,
  EInteger op1Mantissa,
  EInteger op2Mantissa,
  IRadixMathHelper<TMath> helper,
  boolean reportOOM) {
      FastInteger fastOp1Exp = FastInteger.FromBig(op1Exponent);
      FastInteger fastOp2Exp = FastInteger.FromBig(op2Exponent);

      FastInteger expdiff = fastOp1Exp.Copy().Subtract(fastOp2Exp).Abs();
      // Check if exponent difference is too big for
      // radix-power calculation to work quickly
      if (expdiff.CompareToInt(100) >= 0) {
        EInteger op1MantAbs = op1Mantissa;
        EInteger op2MantAbs = op2Mantissa;
        FastInteger precision1 =
          helper.CreateShiftAccumulator(op1MantAbs).GetDigitLength();
        FastInteger precision2 =
          helper.CreateShiftAccumulator(op2MantAbs).GetDigitLength();
        FastInteger exp1 = fastOp1Exp.Copy().Add(precision1).Decrement();
        FastInteger exp2 = fastOp2Exp.Copy().Add(precision2).Decrement();
        int adjcmp = exp1.compareTo(exp2);
        if (adjcmp != 0) {
          return (signA < 0) ? -adjcmp : adjcmp;
        }
        FastInteger maxPrecision = null;
        maxPrecision = (precision1.compareTo(precision2) > 0) ? precision1 :
          precision2;
        // If exponent difference is greater than the
        // maximum precision of the two operands
        if (expdiff.Copy().compareTo(maxPrecision) > 0) {
          int expcmp2 = fastOp1Exp.compareTo(fastOp2Exp);
          if (expcmp2 < 0) {
            if (!op2MantAbs.isZero()) {
              // first operand's exponent is less
              // and second operand isn't zero
              // second mantissa will be shifted by the exponent
              // difference
              FastInteger digitLength1 =
                helper.CreateShiftAccumulator(op1MantAbs).GetDigitLength();
              if (fastOp1Exp.Copy().Add(digitLength1).AddInt(2)
                .compareTo(fastOp2Exp) < 0) {
                // first operand's mantissa can't reach the
                // second operand's mantissa, so the exponent can be
                // raised without affecting the result
                FastInteger tmp = fastOp2Exp.Copy()
                .SubtractInt(8).Subtract(digitLength1).Subtract(maxPrecision);
                FastInteger newDiff = tmp.Copy().Subtract(fastOp2Exp).Abs();
                if (newDiff.compareTo(expdiff) < 0) {
                  // At this point, both operands have the same sign
                  return (signA < 0) ? 1 : -1;
                }
              }
            }
          } else if (expcmp2 > 0) {
            if (!op1MantAbs.isZero()) {
              // first operand's exponent is greater
              // and second operand isn't zero
              // first mantissa will be shifted by the exponent
              // difference
              FastInteger digitLength2 =
                 helper.CreateShiftAccumulator(op2MantAbs).GetDigitLength();
              if (fastOp2Exp.Copy()
                  .Add(digitLength2).AddInt(2).compareTo(fastOp1Exp) <
                0) {
                // second operand's mantissa can't reach the
                // first operand's mantissa, so the exponent can be
                // raised without affecting the result
                FastInteger tmp = fastOp1Exp.Copy()
                .SubtractInt(8).Subtract(digitLength2).Subtract(maxPrecision);
                FastInteger newDiff = tmp.Copy().Subtract(fastOp1Exp).Abs();
                if (newDiff.compareTo(expdiff) < 0) {
                  // At this point, both operands have the same sign
                  return (signA < 0) ? -1 : 1;
                }
              }
            }
          }
          expcmp = op1Exponent.compareTo(op2Exponent);
        }
      }
      if (expcmp > 0) {
        // if ((op1Exponent-op2Exponent).Abs() > 10) {
        // DebugUtility.Log("" + op1Mantissa + " " + op2Mantissa + " [exp="
        // + op1Exponent + " " + op2Exponent + "]");
        // }
        EInteger newmant = RescaleByExponentDiff(
  op1Mantissa,
  op1Exponent,
  op2Exponent,
  helper);
        if (newmant == null) {
          if (reportOOM) {
            throw new OutOfMemoryError("Result requires too much memory");
          }
          return -2;
        }
        int mantcmp = newmant.compareTo(op2Mantissa);
        return (signA < 0) ? -mantcmp : mantcmp;
      } else {
        // if ((op1Exponent-op2Exponent).Abs() > 10) {
        // DebugUtility.Log("" + op1Mantissa + " " + op2Mantissa + " [exp="
        // + op1Exponent + " " + op2Exponent + "]");
        // }
        EInteger newmant = RescaleByExponentDiff(
            op2Mantissa,
            op1Exponent,
            op2Exponent,
            helper);
        if (newmant == null) {
          if (reportOOM) {
            throw new OutOfMemoryError("Result requires too much memory");
          }
          return -2;
        }
        int mantcmp = op1Mantissa.compareTo(newmant);
        return (signA < 0) ? -mantcmp : mantcmp;
      }
    }

    private static boolean IsNullOrSimpleContext(EContext ctx) {
      return ctx == null || ctx == EContext.UnlimitedHalfEven ||
       (!ctx.getHasExponentRange() && !ctx.getHasMaxPrecision() && ctx.getTraps() == 0 &&
        !ctx.getHasFlags());
    }

    private static boolean IsSimpleContext(EContext ctx) {
      return ctx != null && (ctx == EContext.UnlimitedHalfEven ||
       (!ctx.getHasExponentRange() && !ctx.getHasMaxPrecision() && ctx.getTraps() == 0 &&
        !ctx.getHasFlags()));
    }

    private static EInteger PowerOfTwo(FastInteger fi) {
      if (fi.signum() <= 0) {
        return EInteger.FromInt32(1);
      }
      if (fi.CanFitInInt32()) {
        int val = fi.AsInt32();
        if (val <= 30) {
          val = 1 << val;
          return EInteger.FromInt32(val);
        }
        return EInteger.FromInt32(1).ShiftLeft(val);
      } else {
        EInteger bi = EInteger.FromInt32(1);
        FastInteger fi2 = fi.Copy();
        while (fi2.signum() > 0) {
          int count = 1000000;
          if (fi2.CompareToInt(1000000) < 0) {
            count = bi.ToInt32Checked();
          }
          bi = bi.ShiftLeft(count);
          fi2.SubtractInt(count);
        }
        return bi;
      }
    }

    private static <TMath> EInteger RescaleByExponentDiff(
  EInteger mantissa,
  EInteger e1,
  EInteger e2,
  IRadixMathHelper<TMath> helper) {
      if (mantissa.signum() == 0) {
        return EInteger.FromInt32(0);
      }
      FastInteger diff = FastInteger.FromBig(e1).SubtractBig(e2).Abs();
      if (!diff.CanFitInInt32()) {
        // NOTE: For radix 10, each digit fits less than 1 byte; the
        // supported byte length is thus less than the maximum value
        // of a 32-bit integer (2GB).
        FastInteger fastBI = FastInteger.FromBig(mantissa);
        if (helper.GetRadix() != 10 || diff.compareTo(fastBI) > 0) {
          return null;
        }
      }
      return helper.MultiplyByRadixPower(mantissa, diff);
    }

    private static EContext SetPrecisionIfLimited(
      EContext ctx,
      EInteger bigPrecision) {
      return (ctx == null || !ctx.getHasMaxPrecision()) ? ctx :
        ctx.WithBigPrecision(bigPrecision);
    }

    private static void TransferFlags(
  EContext ctxDst,
  EContext ctxSrc) {
      if (ctxDst != null && ctxDst.getHasFlags()) {
        if ((ctxSrc.getFlags() & (EContext.FlagInvalid |
                    EContext.FlagDivideByZero)) != 0) {
          ctxDst.setFlags(ctxDst.getFlags()|(ctxSrc.getFlags() & (EContext.FlagInvalid |
                    EContext.FlagDivideByZero)));
        } else {
          ctxDst.setFlags(ctxDst.getFlags()|(ctxSrc.getFlags()));
        }
      }
    }

    private T AbsRaw(T value) {
      return this.EnsureSign(value, false);
    }

    // mant1 and mant2 are assumed to be nonnegative
    private T AddCore2(
  FastIntegerFixed mant1,
  FastIntegerFixed mant2,
  FastIntegerFixed exponent,
  int flags1,
  int flags2,
  EContext ctx) {
      boolean neg1 = (flags1 & BigNumberFlags.FlagNegative) != 0;
      boolean neg2 = (flags2 & BigNumberFlags.FlagNegative) != 0;
      boolean negResult = false;
      // DebugUtility.Log("neg1=" + neg1 + " neg2=" + neg2);
      if (neg1 != neg2) {
        // Signs are different, treat as a subtraction
        mant1 = FastIntegerFixed.Subtract(mant1, mant2);
        int mant1Sign = mant1.signum();
        if (mant1Sign < 0) {
          negResult = !neg1;
          mant1 = mant1.Negate();
        } else if (mant1Sign == 0) {
          // Result is negative zero
          negResult = neg1 ^ neg2;
          if (negResult) {
            negResult &= (neg1 && neg2) || ((neg1 ^ neg2) && ctx != null &&
                    ctx.getRounding() == ERounding.Floor);
          }
        } else {
          negResult = neg1;
        }
      } else {
        // Signs are same, treat as an addition
        mant1 = FastIntegerFixed.Add(mant1, mant2);
        negResult = neg1;
        if (negResult && mant1.isValueZero()) {
          // Result is negative zero
          negResult &= (neg1 && neg2) || ((neg1 ^ neg2) && ctx != null &&
                    ctx.getRounding() == ERounding.Floor);
        }
      }
      // DebugUtility.Log("mant1= " + mant1 + " exp= " + exponent +" neg= "+
      // (negResult));
      return this.helper.CreateNewWithFlagsFastInt(
  mant1,
  exponent,
  negResult ? BigNumberFlags.FlagNegative : 0);
    }

    // mant1 and mant2 are assumed to be nonnegative
    private T AddCore(
  EInteger mant1,
  EInteger mant2,
  EInteger exponent,
  int flags1,
  int flags2,
  EContext ctx) {
      boolean neg1 = (flags1 & BigNumberFlags.FlagNegative) != 0;
      boolean neg2 = (flags2 & BigNumberFlags.FlagNegative) != 0;
      boolean negResult = false;
      // DebugUtility.Log("neg1=" + neg1 + " neg2=" + neg2);
      if (neg1 != neg2) {
        // Signs are different, treat as a subtraction
        // DebugUtility.Log("sub " + mant1 + " " + mant2);
        mant1 = mant1.Subtract(mant2);
        int mant1Sign = mant1.signum();
        negResult = neg1 ^ (mant1Sign == 0 ? neg2 : (mant1Sign < 0));
        if (mant1Sign < 0) {
          mant1 = mant1.Negate();
        }
      } else {
        // Signs are same, treat as an addition
        // DebugUtility.Log("add " + mant1 + " " + mant2);
        mant1 = mant1.Add(mant2);
        negResult = neg1;
      }
      if (negResult && mant1.isZero()) {
        // Result is negative zero
        negResult &= (neg1 && neg2) || ((neg1 ^ neg2) && ctx != null &&
                    ctx.getRounding() == ERounding.Floor);
      }
      // DebugUtility.Log("mant1= " + mant1 + " exp= " + exponent +" neg= "+
      // (negResult));
      return this.helper.CreateNewWithFlags(
  mant1,
  exponent,
  negResult ? BigNumberFlags.FlagNegative : 0);
    }

    private FastInteger OverestimateDigitLength(EInteger ei) {
      if (this.thisRadix == 2) {
        return new FastInteger(ei.GetUnsignedBitLength());
      } else if (this.thisRadix == 10) {
        int bitLength = ei.GetUnsignedBitLength();
        if (bitLength <= 2135) {
          // May overestimate by 1
          return new FastInteger(1 + ((bitLength * 631305) >> 21));
        }
        return new FastInteger(bitLength >> 2);
      } else {
        return this.helper.CreateShiftAccumulator(ei)
                .GetDigitLength();
      }
    }

    private static FastInteger valueFastIntegerTwo = new FastInteger(2);

    private T AddExDiffExp(
  T thisValue,
  T other,
  int thisFlags,
  int otherFlags,
  EContext ctx,
  int expcmp,
  boolean roundToOperandPrecision) {
      T retval = null;
      // choose the minimum exponent
      T op1 = thisValue;
      T op2 = other;
      EInteger op1MantAbs = this.helper.GetMantissa(thisValue);
      EInteger op2MantAbs = this.helper.GetMantissa(other);
      EInteger op1Exponent = this.helper.GetExponent(op1);
      EInteger op2Exponent = this.helper.GetExponent(op2);
      EInteger resultExponent = expcmp < 0 ? op1Exponent : op2Exponent;
      if (ctx != null && ctx.getHasMaxPrecision() && ctx.getPrecision().signum() > 0) {
        FastInteger fastOp1Exp = FastInteger.FromBig(op1Exponent);
        FastInteger fastOp2Exp = FastInteger.FromBig(op2Exponent);
        FastInteger expdiff = fastOp1Exp.Copy().Subtract(fastOp2Exp).Abs();
        // Check if exponent difference is too big for
        // radix-power calculation to work quickly
        FastInteger fastPrecision = FastInteger.FromBig(ctx.getPrecision());
        boolean moreDistantThanPrecision = expdiff.compareTo(fastPrecision) > 0;
        // If exponent difference is greater than the precision
                // if (true || moreDistantThanPrecision) {
        if (moreDistantThanPrecision) {
          int expcmp2 = fastOp1Exp.compareTo(fastOp2Exp);
          if (expcmp2 < 0) {
            if (!op2MantAbs.isZero()) {
              // first operand's exponent is less
              // and second operand isn't zero
              // second mantissa will be shifted by the exponent
              // difference
              // _________________________111111111111|_
              // ___222222222222222|____________________
           FastInteger digitLength1 = this.OverestimateDigitLength(op1MantAbs);
              if (fastOp1Exp.Copy().Add(digitLength1).AddInt(2)
              .compareTo(fastOp2Exp) < 0) {
                // first operand's mantissa can't reach the
                // second operand's mantissa, so the exponent can be
                // raised without affecting the result
                FastInteger tmp = fastOp2Exp.Copy().SubtractInt(4)
                  .Subtract(digitLength1).SubtractBig(ctx.getPrecision());
                FastInteger newDiff = tmp.Copy().Subtract(fastOp2Exp).Abs();
                if (newDiff.compareTo(expdiff) < 0) {
                  // First operand can be treated as almost zero
                  boolean sameSign = this.helper.GetSign(thisValue) ==
                  this.helper.GetSign(other);
                  boolean oneOpIsZero = op1MantAbs.isZero();
                  FastInteger digitLength2 =
                  this.helper.CreateShiftAccumulator(op2MantAbs)
                  .GetDigitLength();
                  if (digitLength2.compareTo(fastPrecision) < 0) {
                    // Second operand's precision too short, extend
                    // it to the full precision
                    FastInteger precisionDiff =
                    fastPrecision.Copy().Subtract(digitLength2);
                    if (!oneOpIsZero && !sameSign) {
                    precisionDiff.AddInt(2);
                    }
                    op2MantAbs = this.TryMultiplyByRadixPower(
                    op2MantAbs,
                    precisionDiff);
                    if (op2MantAbs == null) {
                    return this.SignalInvalidWithMessage(
                    ctx,
                    "Result requires too much memory");
                    }
                    EInteger bigintTemp = precisionDiff.AsEInteger();
                    op2Exponent = op2Exponent.Subtract(bigintTemp);
                    if (!oneOpIsZero && !sameSign) {
                    op2MantAbs = op2MantAbs.Subtract(EInteger.FromInt32(1));
                    }
                    other = this.helper.CreateNewWithFlags(
          op2MantAbs,
          op2Exponent,
          this.helper.GetFlags(other));
               FastInteger shift = digitLength2.Copy().Subtract(fastPrecision);
                    if (oneOpIsZero && ctx != null && ctx.getHasFlags()) {
                    ctx.setFlags(ctx.getFlags()|(EContext.FlagRounded));
                    }
                    // DebugUtility.Log("Second op's prec too short:
                    // op2MantAbs=" + op2MantAbs + " precdiff= " +
                    // (precisionDiff));
                    return this.RoundToPrecisionInternal(
          other,
           (oneOpIsZero || sameSign) ? 0 : 1,
           (oneOpIsZero && !sameSign) ? 0 : 1,
           shift,
           false,
                    ctx);
                  }
                  if (!oneOpIsZero && !sameSign) {
                    op2MantAbs = this.TryMultiplyByRadixPower(
                    op2MantAbs,
                    valueFastIntegerTwo);
                    if (op2MantAbs == null) {
                    return this.SignalInvalidWithMessage(
                    ctx,
                    "Result requires too much memory");
                    }
                    op2Exponent = op2Exponent.Subtract(EInteger.FromInt64(2));
                    op2MantAbs = op2MantAbs.Subtract(EInteger.FromInt32(1));
                    other = this.helper.CreateNewWithFlags(
                    op2MantAbs,
                    op2Exponent,
                    this.helper.GetFlags(other));
                    FastInteger shift =
                    digitLength2.Copy().Subtract(fastPrecision);

                    return this.RoundToPrecisionInternal(
          other,
          0,
          0,
          shift,
          false,
          ctx);
                  } else {
                    FastInteger shift2 =
                    digitLength2.Copy().Subtract(fastPrecision);
                    if (!sameSign && ctx != null && ctx.getHasFlags()) {
                    ctx.setFlags(ctx.getFlags()|(EContext.FlagRounded));
                    }

                    return this.RoundToPrecisionInternal(
                    other,
                    0,
                    sameSign ? 1 : 0,
                    shift2,
                    false,
                    ctx);
                  }
                }
              }
            }
          } else if (expcmp2 > 0) {
            if (!op1MantAbs.isZero()) {
              // first operand's exponent is greater
              // and first operand isn't zero
              // first mantissa will be shifted by the exponent
              // difference
              // __111111111111|
              // ____________________222222222222222|
           FastInteger digitLength2 = this.OverestimateDigitLength(op2MantAbs);
              if (fastOp2Exp.Copy().Add(digitLength2).AddInt(2)
              .compareTo(fastOp1Exp) < 0) {
                // second operand's mantissa can't reach the
                // first operand's mantissa, so the exponent can be
                // raised without affecting the result
                FastInteger tmp = fastOp1Exp.Copy().SubtractInt(4)
                .Subtract(digitLength2).SubtractBig(ctx.getPrecision());
                FastInteger newDiff = tmp.Copy().Subtract(fastOp1Exp).Abs();
                if (newDiff.compareTo(expdiff) < 0) {
                  // Second operand can be treated as almost zero
                  boolean sameSign = this.helper.GetSign(thisValue) ==
                  this.helper.GetSign(other);
                  boolean oneOpIsZero = op2MantAbs.isZero();
                  digitLength2 = this.helper.CreateShiftAccumulator(op1MantAbs)
                    .GetDigitLength();
                  if (digitLength2.compareTo(fastPrecision) < 0) {
                    // First operand's precision too short; extend it
                    // to the full precision
                    FastInteger precisionDiff =
                    fastPrecision.Copy().Subtract(digitLength2);
                    if (!oneOpIsZero && !sameSign) {
                    precisionDiff.AddInt(2);
                    }
                    op1MantAbs = this.TryMultiplyByRadixPower(
                    op1MantAbs,
                    precisionDiff);
                    if (op1MantAbs == null) {
                    return this.SignalInvalidWithMessage(
                    ctx,
                    "Result requires too much memory");
                    }
                    EInteger bigintTemp = precisionDiff.AsEInteger();
                    op1Exponent = op1Exponent.Subtract(bigintTemp);
                    if (!oneOpIsZero && !sameSign) {
                    op1MantAbs = op1MantAbs.Subtract(EInteger.FromInt32(1));
                    }
                    thisValue = this.helper.CreateNewWithFlags(
                    op1MantAbs,
                    op1Exponent,
                    this.helper.GetFlags(thisValue));
                    FastInteger shift =
                    digitLength2.Copy().Subtract(fastPrecision);
                    if (oneOpIsZero && ctx != null && ctx.getHasFlags()) {
                    ctx.setFlags(ctx.getFlags()|(EContext.FlagRounded));
                    }

                    return this.RoundToPrecisionInternal(
                    thisValue,
     (oneOpIsZero || sameSign) ? 0 : 1,
     (oneOpIsZero && !sameSign) ? 0 : 1,
     shift,
     false,
                    ctx);
                  }
                  if (!oneOpIsZero && !sameSign) {
                    op1MantAbs = this.TryMultiplyByRadixPower(
                    op1MantAbs,
                    valueFastIntegerTwo);
                    if (op1MantAbs == null) {
                    return this.SignalInvalidWithMessage(
                    ctx,
                    "Result requires too much memory");
                    }
                    op1Exponent = op1Exponent.Subtract(EInteger.FromInt64(2));
                    op1MantAbs = op1MantAbs.Subtract(EInteger.FromInt32(1));
                    thisValue = this.helper.CreateNewWithFlags(
                    op1MantAbs,
                    op1Exponent,
                    this.helper.GetFlags(thisValue));
                    FastInteger shift =
                    digitLength2.Copy().Subtract(fastPrecision);

                    return this.RoundToPrecisionInternal(
          thisValue,
          0,
          0,
          shift,
          false,
          ctx);
                  } else {
                    FastInteger shift2 =
                    digitLength2.Copy().Subtract(fastPrecision);
                    if (!sameSign && ctx != null && ctx.getHasFlags()) {
                    ctx.setFlags(ctx.getFlags()|(EContext.FlagRounded));
                    }

                    return this.RoundToPrecisionInternal(
                    thisValue,
                    0,
                    sameSign ? 1 : 0,
                    shift2,
                    false,
                    ctx);
                  }
                }
              }
            }
          }
          expcmp = op1Exponent.compareTo(op2Exponent);
          resultExponent = expcmp < 0 ? op1Exponent : op2Exponent;
        }
      }
      if (expcmp > 0) {
        op1MantAbs = RescaleByExponentDiff(
  op1MantAbs,
  op1Exponent,
  op2Exponent,
  this.helper);
        if (op1MantAbs == null) {
          return this.SignalInvalidWithMessage(
            ctx,
            "Result requires too much memory");
        }
        retval = this.AddCore(
          op1MantAbs,
          op2MantAbs,
          resultExponent,
          thisFlags,
          otherFlags,
          ctx);
      } else {
        op2MantAbs = RescaleByExponentDiff(
  op2MantAbs,
  op1Exponent,
  op2Exponent,
  this.helper);
        if (op2MantAbs == null) {
          return this.SignalInvalidWithMessage(
            ctx,
            "Result requires too much memory");
        }
        retval = this.AddCore(
  op1MantAbs,
  op2MantAbs,
  resultExponent,
  thisFlags,
  otherFlags,
  ctx);
      }
      if (roundToOperandPrecision && ctx != null && ctx.getHasMaxPrecision()) {
        FastInteger digitLength1 =
          this.helper.CreateShiftAccumulator(op1MantAbs)
          .GetDigitLength();
        FastInteger digitLength2 =
          this.helper.CreateShiftAccumulator(op2MantAbs)
          .GetDigitLength();
        FastInteger maxDigitLength =
          (digitLength1.compareTo(digitLength2) > 0) ? digitLength1 :
          digitLength2;
        maxDigitLength.SubtractBig(ctx.getPrecision());
        // DebugUtility.Log("retval= " + retval + " maxdl=" +
        // maxDigitLength + " prec= " + (ctx.getPrecision()));
        return (maxDigitLength.signum() > 0) ? this.RoundToPrecisionInternal(
            retval,
            0,
            0,
            maxDigitLength,
            false,
            ctx) : this.RoundToPrecision(retval, ctx);
        // DebugUtility.Log("retval now " + retval);
      } else {
        return IsNullOrSimpleContext(ctx) ? retval :
          this.RoundToPrecision(retval, ctx);
      }
    }

    private T CompareToHandleSpecial(
  T thisValue,
  T other,
  boolean treatQuietNansAsSignaling,
  EContext ctx) {
      int thisFlags = this.helper.GetFlags(thisValue);
      int otherFlags = this.helper.GetFlags(other);
      if (((thisFlags | otherFlags) & BigNumberFlags.FlagSpecial) != 0) {
        // Check this value then the other value for signaling NaN
        if ((thisFlags & BigNumberFlags.FlagSignalingNaN) != 0) {
          return this.SignalingNaNInvalid(thisValue, ctx);
        }
        if ((otherFlags & BigNumberFlags.FlagSignalingNaN) != 0) {
          return this.SignalingNaNInvalid(other, ctx);
        }
        if (treatQuietNansAsSignaling) {
          if ((thisFlags & BigNumberFlags.FlagQuietNaN) != 0) {
            return this.SignalingNaNInvalid(thisValue, ctx);
          }
          if ((otherFlags & BigNumberFlags.FlagQuietNaN) != 0) {
            return this.SignalingNaNInvalid(other, ctx);
          }
        } else {
          // Check this value then the other value for quiet NaN
          if ((thisFlags & BigNumberFlags.FlagQuietNaN) != 0) {
            return this.ReturnQuietNaN(thisValue, ctx);
          }
          if ((otherFlags & BigNumberFlags.FlagQuietNaN) != 0) {
            return this.ReturnQuietNaN(other, ctx);
          }
        }
        if ((thisFlags & BigNumberFlags.FlagInfinity) != 0) {
          // thisValue is infinity
          return ((thisFlags & (BigNumberFlags.FlagInfinity |
                    BigNumberFlags.FlagNegative)) == (otherFlags &
  (BigNumberFlags.FlagInfinity |
  BigNumberFlags.FlagNegative))) ? this.ValueOf(0, null) : (((thisFlags &
                BigNumberFlags.FlagNegative) == 0) ? this.ValueOf(
  1,
  null) : this.ValueOf(-1, null));
        }
        if ((otherFlags & BigNumberFlags.FlagInfinity) != 0) {
          // the other value is infinity
          return ((thisFlags & (BigNumberFlags.FlagInfinity |
                    BigNumberFlags.FlagNegative)) == (otherFlags &
  (BigNumberFlags.FlagInfinity |
  BigNumberFlags.FlagNegative))) ? this.ValueOf(0, null) : (((otherFlags &
                    BigNumberFlags.FlagNegative) == 0) ?
                this.ValueOf(-1, null) : this.ValueOf(1, null));
        }
      }
      return null;
    }

    private static <TMath> int CompareToHandleSpecial2(
      TMath thisValue,
      TMath other,
      int thisFlags,
      int otherFlags) {
      // Assumes either value is NaN and/or infinity
      {
        if ((thisFlags & BigNumberFlags.FlagNaN) != 0) {
          if ((otherFlags & BigNumberFlags.FlagNaN) != 0) {
            return 0;
          }
          // Consider NaN to be greater
          return 1;
        }
        if ((otherFlags & BigNumberFlags.FlagNaN) != 0) {
          // Consider this to be less than NaN
          return -1;
        }
        if ((thisFlags & BigNumberFlags.FlagInfinity) != 0) {
          // thisValue is infinity
          return ((thisFlags & (BigNumberFlags.FlagInfinity |
                    BigNumberFlags.FlagNegative)) == (otherFlags &
  (BigNumberFlags.FlagInfinity |
  BigNumberFlags.FlagNegative))) ? 0 :
            (((thisFlags & BigNumberFlags.FlagNegative) == 0) ? 1 : -1);
        }
        if ((otherFlags & BigNumberFlags.FlagInfinity) != 0) {
          // the other value is infinity
          return ((thisFlags & (BigNumberFlags.FlagInfinity |
                    BigNumberFlags.FlagNegative)) == (otherFlags &
  (BigNumberFlags.FlagInfinity |
  BigNumberFlags.FlagNegative))) ? 0 :
            (((otherFlags & BigNumberFlags.FlagNegative) == 0) ? -1 : 1);
        }
      }
      return 2;
    }

    private static <TMath> int CompareToInternal(
      TMath thisValue,
      TMath otherValue,
      boolean reportOOM,
      IRadixMathHelper<TMath> helper) {
      int signA = helper.GetSign(thisValue);
      int signB = helper.GetSign(otherValue);
      if (signA != signB) {
        return (signA < signB) ? -1 : 1;
      }
      if (signB == 0 || signA == 0) {
        // Special case: Either operand is zero
        return 0;
      }
      FastIntegerFixed op1Exponent = helper.GetExponentFastInt(thisValue);
      FastIntegerFixed op2Exponent = helper.GetExponentFastInt(otherValue);
      FastIntegerFixed op1Mantissa = helper.GetMantissaFastInt(thisValue);
      FastIntegerFixed op2Mantissa = helper.GetMantissaFastInt(otherValue);
      int expcmp = op1Exponent.compareTo(op2Exponent);
      // At this point, the signs are equal so we can compare
      // their absolute values instead
      int mantcmp = op1Mantissa.compareTo(op2Mantissa);
      if (mantcmp == 0) {
        // Special case: Mantissas are equal
        return signA < 0 ? -expcmp : expcmp;
      }
      if (expcmp == 0) {
        return signA < 0 ? -mantcmp : mantcmp;
      }
      if (op1Exponent.CanFitInInt32() && op2Exponent.CanFitInInt32() &&
          op1Mantissa.CanFitInInt32() && op2Mantissa.CanFitInInt32()) {
        int e1int = op1Exponent.AsInt32();
        int e2int = op2Exponent.AsInt32();
        int c = CompareToFast(
  e1int,
  e2int,
  expcmp,
  signA,
  op1Mantissa,
  op2Mantissa,
  helper.GetRadix());
        if (c <= 1) {
          return c;
        }
      }
      return CompareToSlow(
        op1Exponent.ToEInteger(),
        op2Exponent.ToEInteger(),
        expcmp,
        signA,
        op1Mantissa.ToEInteger(),
        op2Mantissa.ToEInteger(),
        helper,
        reportOOM);
    }

    private T DivideInternal(
  T thisValue,
  T divisor,
  EContext ctx,
  int integerMode,
  EInteger desiredExponent) {
      T ret = this.DivisionHandleSpecial(thisValue, divisor, ctx);
      if ((Object)ret != (Object)null) {
        return ret;
      }
      int signA = this.helper.GetSign(thisValue);
      int signB = this.helper.GetSign(divisor);
      if (signB == 0) {
        if (signA == 0) {
          return this.SignalInvalid(ctx);
        }
        boolean flagsNeg = ((this.helper.GetFlags(thisValue) &
           BigNumberFlags.FlagNegative) != 0) ^
                  ((this.helper.GetFlags(divisor) &
            BigNumberFlags.FlagNegative) != 0);
        return this.SignalDivideByZero(ctx, flagsNeg);
      }
      int radix = this.thisRadix;
      if (signA == 0) {
        T retval = null;
        if (integerMode == IntegerModeFixedScale) {
          int newflags = (this.helper.GetFlags(thisValue) &
                BigNumberFlags.FlagNegative) ^ (this.helper.GetFlags(divisor) &
             BigNumberFlags.FlagNegative);
          retval = this.helper.CreateNewWithFlags(
            EInteger.FromInt32(0),
            desiredExponent,
            newflags);
        } else {
          EInteger dividendExp = this.helper.GetExponent(thisValue);
          EInteger divisorExp = this.helper.GetExponent(divisor);
          int newflags = (this.helper.GetFlags(thisValue) &
                BigNumberFlags.FlagNegative) ^ (this.helper.GetFlags(divisor) &
             BigNumberFlags.FlagNegative);
          retval =
            this.RoundToPrecision(
              this.helper.CreateNewWithFlags(EInteger.FromInt32(0), dividendExp.Subtract(divisorExp), newflags),
              ctx);
        }
        return retval;
      } else {
        EInteger mantissaDividend = this.helper.GetMantissa(thisValue);
        EInteger mantissaDivisor = this.helper.GetMantissa(divisor);
        FastInteger expDividend =
          this.helper.GetExponentFastInt(thisValue).ToFastInteger();
        FastInteger expDivisor =
          this.helper.GetExponentFastInt(divisor).ToFastInteger();
        FastInteger expdiff = expDividend.Copy().Subtract(expDivisor);
        FastInteger adjust = new FastInteger(0);
        FastInteger result = new FastInteger(0);
        FastInteger naturalExponent = expdiff.Copy();
        boolean hasPrecision = ctx != null && ctx.getPrecision().signum() != 0;
        boolean resultNeg = (this.helper.GetFlags(thisValue) &
                BigNumberFlags.FlagNegative) != (this.helper.GetFlags(divisor) &
           BigNumberFlags.FlagNegative);
        FastInteger fastPrecision = (!hasPrecision) ? new FastInteger(0) :
          FastInteger.FromBig(ctx.getPrecision());
        FastInteger dividendPrecision = null;
        FastInteger divisorPrecision = null;
        if (integerMode == IntegerModeFixedScale) {
          FastInteger shift;
          EInteger rem;
          FastInteger fastDesiredExponent =
              FastInteger.FromBig(desiredExponent);
          if (ctx != null && ctx.getHasFlags() &&
              fastDesiredExponent.compareTo(expdiff) > 0) {
            // Treat as rounded if the desired exponent is greater
            // than the "ideal" exponent
            ctx.setFlags(ctx.getFlags()|(EContext.FlagRounded));
          }
          if (expdiff.compareTo(fastDesiredExponent) <= 0) {
            shift = fastDesiredExponent.Copy().Subtract(expdiff);
            EInteger quo;
            {
              EInteger[] divrem = mantissaDividend.DivRem(mantissaDivisor);
              quo = divrem[0];
              rem = divrem[1];
            }
            return this.RoundToScale(
  quo,
  rem,
  mantissaDivisor,
  desiredExponent,
  shift,
  resultNeg,
  ctx);
          }
          if (ctx != null && ctx.getPrecision().signum() != 0 &&
           expdiff.Copy().SubtractInt(8).compareTo(fastPrecision) >
              0) {
            // NOTE: 8 guard digits
            // Result would require a too-high precision since
            // exponent difference is much higher
            return this.SignalInvalidWithMessage(
  ctx,
  "Result can't fit the precision");
          } else {
            shift = expdiff.Copy().Subtract(fastDesiredExponent);
            mantissaDividend =
              this.TryMultiplyByRadixPower(mantissaDividend, shift);
            if (mantissaDividend == null) {
              return this.SignalInvalidWithMessage(
                ctx,
                "Result requires too much memory");
            }
            EInteger quo;
            {
              EInteger[] divrem = mantissaDividend.DivRem(mantissaDivisor);
              quo = divrem[0];
              rem = divrem[1];
            }
            return this.RoundToScale(
              quo,
 rem,
 mantissaDivisor,
 desiredExponent,
 new FastInteger(0),
 resultNeg,
 ctx);
          }
        }
        if (integerMode == IntegerModeRegular) {
          EInteger rem = null;
          EInteger quo = null;
          EInteger[] divrem = mantissaDividend.DivRem(mantissaDivisor);
          quo = divrem[0];
          rem = divrem[1];
          if (rem.isZero()) {
            // Dividend is divisible by divisor
            // DebugUtility.Log("divisible dividend");
            quo = quo.Abs();
            return this.RoundToPrecision(
  this.helper.CreateNewWithFlagsFastInt(FastIntegerFixed.FromBig(quo), FastIntegerFixed.FromFastInteger(expdiff), resultNeg ? BigNumberFlags.FlagNegative : 0),
  ctx);
          }
          rem = null;
          quo = null;
          if (hasPrecision) {
            // DebugUtility.Log("has precision");
            EInteger divid = mantissaDividend;
            FastInteger shift = FastInteger.FromBig(ctx.getPrecision());
            dividendPrecision =
                   this.helper.CreateShiftAccumulator(mantissaDividend)
                    .GetDigitLength();
            divisorPrecision =
              this.helper.CreateShiftAccumulator(mantissaDivisor)
              .GetDigitLength();
            FastInteger dividPrecision = dividendPrecision.Copy();
            FastInteger divisPrecision = divisorPrecision.Copy();
            if (dividendPrecision.compareTo(divisorPrecision) <= 0) {
              divisorPrecision = divisorPrecision.Copy()
                .Subtract(dividendPrecision);
              divisorPrecision.Increment();
              shift.Add(divisorPrecision);
              divid = this.TryMultiplyByRadixPower(divid, shift);
              dividPrecision.Add(shift);
              if (divid == null) {
                return this.SignalInvalidWithMessage(
                ctx,
                "Result requires too much memory");
              }
            } else {
              // Already greater than divisor precision
              dividendPrecision = dividendPrecision.Copy()
                .Subtract(divisorPrecision);
              if (dividendPrecision.compareTo(shift) <= 0) {
                shift.Subtract(dividendPrecision);
                shift.Increment();
                divid = this.TryMultiplyByRadixPower(divid, shift);
                dividPrecision.Add(shift);
                if (divid == null) {
                  return this.SignalInvalidWithMessage(
                ctx,
                "Result requires too much memory");
                }
              } else {
                // no need to shift
                shift.SetInt(0);
              }
            }
            dividendPrecision = dividPrecision;
            divisorPrecision = divisPrecision;
            if (shift.signum() != 0 || quo == null) {
              // if shift isn't zero, recalculate the quotient
              // and remainder
              EInteger[] divrem2 = divid.DivRem(mantissaDivisor);
              quo = divrem2[0];
              rem = divrem2[1];
            }
            // DebugUtility.Log(String.Format("" + divid + "" +
            // mantissaDivisor + " -> quo= " + quo + " rem= " +
            // (rem)));
            int[] digitStatus = this.RoundToScaleStatus(
              rem,
              mantissaDivisor,
              ctx);
            if (digitStatus == null) {
              return this.SignalInvalidWithMessage(
  ctx,
  "Rounding was required");
            }
            FastInteger natexp = naturalExponent.Copy().Subtract(shift);
            EContext ctxcopy = ctx.WithBlankFlags();
            T retval2 = this.helper.CreateNewWithFlags(
              quo,
              natexp.AsEInteger(),
              resultNeg ? BigNumberFlags.FlagNegative : 0);
            retval2 = this.RoundToPrecisionInternal(
              retval2,
              digitStatus[0],
              digitStatus[1],
              null,
              false,
              ctxcopy);
            if ((ctxcopy.getFlags() & EContext.FlagInexact) != 0) {
              if (ctx.getHasFlags()) {
                ctx.setFlags(ctx.getFlags()|(ctxcopy.getFlags()));
              }
              return retval2;
            }
            if (ctx.getHasFlags()) {
              ctx.setFlags(ctx.getFlags()|(ctxcopy.getFlags() & ~EContext.FlagRounded));
            }
            return this.ReduceToPrecisionAndIdealExponent(
              retval2,
              ctx,
              rem.isZero() ? null : fastPrecision,
              expdiff);
          }
        }
        // Rest of method assumes unlimited precision
        // and IntegerModeRegular
        int mantcmp = mantissaDividend.compareTo(mantissaDivisor);
        if (mantcmp == 0) {
          result = new FastInteger(1);
          mantissaDividend = EInteger.FromInt32(0);
        } else {
          EInteger gcd = mantissaDividend.Gcd(mantissaDivisor);
          // DebugUtility.Log("mgcd/den1=" + mantissaDividend + "/" + (//
          // mantissaDivisor) + "/" + gcd);
          if (gcd.compareTo(EInteger.FromInt32(1)) != 0) {
            mantissaDividend = mantissaDividend.Divide(gcd);
            mantissaDivisor = mantissaDivisor.Divide(gcd);
          }
          // DebugUtility.Log("mgcd/den2=" + mantissaDividend + "/" + (//
          // mantissaDivisor) + "/" + gcd);
          FastInteger divShift = this.helper.DivisionShift(
              mantissaDividend,
              mantissaDivisor);

          if (divShift == null) {
            return this.SignalInvalidWithMessage(
  ctx,
  "Result would have a nonterminating expansion");
          }
          mantissaDividend = this.helper.MultiplyByRadixPower(
            mantissaDividend,
            divShift);
          adjust = divShift.Copy();
          // DebugUtility.Log("mant " + mantissaDividend + " " +
          // (// mantissaDivisor));
          EInteger[] quorem = mantissaDividend.DivRem(mantissaDivisor);

          mantissaDividend = quorem[1];
          result = FastInteger.FromBig(quorem[0]);
        }
        // mantissaDividend now has the remainder
        FastInteger exp = expdiff.Copy().Subtract(adjust);
        ERounding rounding = (ctx == null) ? ERounding.HalfEven : ctx.getRounding();
        int lastDiscarded = 0;
        int olderDiscarded = 0;
        if (!mantissaDividend.isZero()) {
        if (rounding == ERounding.HalfDown || rounding == ERounding.HalfEven ||
            rounding == ERounding.HalfUp) {
            EInteger halfDivisor = mantissaDivisor.ShiftRight(1);
            int cmpHalf = mantissaDividend.compareTo(halfDivisor);
            if ((cmpHalf == 0) && mantissaDivisor.isEven()) {
              // remainder is exactly half
              lastDiscarded = radix / 2;
              olderDiscarded = 0;
            } else if (cmpHalf > 0) {
              // remainder is greater than half
              lastDiscarded = radix / 2;
              olderDiscarded = 1;
            } else {
              // remainder is less than half
              lastDiscarded = 0;
              olderDiscarded = 1;
            }
          } else {
            if (rounding == ERounding.None) {
              return this.SignalInvalidWithMessage(
                ctx,
                "Rounding was required");
            }
            lastDiscarded = 1;
            olderDiscarded = 1;
          }
        }
        EInteger bigResult = result.AsEInteger();
        if (ctx != null && ctx.getHasFlags() && exp.compareTo(expdiff) > 0) {
          // Treat as rounded if the true exponent is greater
          // than the "ideal" exponent
          ctx.setFlags(ctx.getFlags()|(EContext.FlagRounded));
        }
        EInteger bigexp = exp.AsEInteger();
        T retval = this.helper.CreateNewWithFlags(
          bigResult,
          bigexp,
          resultNeg ? BigNumberFlags.FlagNegative : 0);
        return this.RoundToPrecisionInternal(
  retval,
  lastDiscarded,
  olderDiscarded,
  null,
  false,
  ctx);
      }
    }

    private T DivisionHandleSpecial(
  T thisValue,
  T other,
  EContext ctx) {
      int thisFlags = this.helper.GetFlags(thisValue);
      int otherFlags = this.helper.GetFlags(other);
      if (((thisFlags | otherFlags) & BigNumberFlags.FlagSpecial) != 0) {
        T result = this.HandleNotANumber(thisValue, other, ctx);
        if ((Object)result != (Object)null) {
          return result;
        }
        if ((thisFlags & BigNumberFlags.FlagInfinity) != 0 && (otherFlags &
  BigNumberFlags.FlagInfinity) != 0) {
          // Attempt to divide infinity by infinity
          return this.SignalInvalid(ctx);
        }
        if ((thisFlags & BigNumberFlags.FlagInfinity) != 0) {
          return this.EnsureSign(
  thisValue,
  ((thisFlags ^ otherFlags) & BigNumberFlags.FlagNegative) != 0);
        }
        if ((otherFlags & BigNumberFlags.FlagInfinity) != 0) {
          // Divisor is infinity, so result will be epsilon
          if (ctx != null && ctx.getHasExponentRange() && ctx.getPrecision().signum() > 0) {
            if (ctx.getHasFlags()) {
              ctx.setFlags(ctx.getFlags()|(EContext.FlagClamped));
            }
            EInteger bigexp = ctx.getEMin();
            EInteger bigprec = ctx.getPrecision();
            if (ctx.getAdjustExponent()) {
              bigexp = bigexp.Subtract(bigprec);
              bigexp = bigexp.Add(EInteger.FromInt32(1));
            }
            thisFlags = (thisFlags ^ otherFlags) & BigNumberFlags.FlagNegative;
            return this.helper.CreateNewWithFlags(
  EInteger.FromInt32(0),
  bigexp,
  thisFlags);
          }
          thisFlags = (thisFlags ^ otherFlags) & BigNumberFlags.FlagNegative;
          return this.RoundToPrecision(
   this.helper.CreateNewWithFlags(EInteger.FromInt32(0), EInteger.FromInt32(0), thisFlags),
   ctx);
        }
      }
      return null;
    }

    private T EnsureSign(T val, boolean negative) {
      if (val == null) {
        return val;
      }
      int flags = this.helper.GetFlags(val);
      if ((negative && (flags & BigNumberFlags.FlagNegative) == 0) ||
          (!negative && (flags & BigNumberFlags.FlagNegative) != 0)) {
        flags &= ~BigNumberFlags.FlagNegative;
        flags |= negative ? BigNumberFlags.FlagNegative : 0;
        return this.helper.CreateNewWithFlags(
  this.helper.GetMantissa(val),
  this.helper.GetExponent(val),
  flags);
      }
      return val;
    }

    private T ExpInternal(
  T thisValue,
  EInteger workingPrecision,
  EContext ctx) {
      T one = this.helper.ValueOf(1);
      int precisionAdd = this.thisRadix == 2 ? 18 : 12;
      EContext ctxdiv = SetPrecisionIfLimited(
        ctx,
        workingPrecision.Add(EInteger.FromInt32(precisionAdd)))
        .WithRounding(ERounding.OddOrZeroFiveUp);
      EInteger bigintN = EInteger.FromInt64(2);
      EInteger facto = EInteger.FromInt32(1);
      // Guess starts with 1 + thisValue
      T guess = this.Add(one, thisValue, null);
      T lastGuess = guess;
      T pow = thisValue;
      boolean more = true;
      int lastCompare = 0;
      int vacillations = 0;
      while (more) {
        lastGuess = guess;
        // Iterate by:
        // newGuess = guess + (thisValue^n/factorial(n))
        // (n starts at 2 and increases by 1 after
        // each iteration)
        pow = this.Multiply(pow, thisValue, ctxdiv);
        facto = facto.Multiply(bigintN);
        T tmp = this.Divide(
          pow,
          this.helper.CreateNewWithFlags(facto, EInteger.FromInt32(0), 0),
          ctxdiv);
        T newGuess = this.Add(guess, tmp, ctxdiv);
        // DebugUtility.Log("newguess" +
        // this.helper.GetMantissa(newGuess)+" ctxdiv " +
        // ctxdiv.getPrecision());
        // DebugUtility.Log("newguess " + newGuess);
        // DebugUtility.Log("newguessN " + NextPlus(newGuess,ctxdiv));
        {
          int guessCmp = this.compareTo(lastGuess, newGuess);
          if (guessCmp == 0) {
            more = false;
          } else if ((guessCmp > 0 && lastCompare < 0) || (lastCompare > 0 &&
                    guessCmp < 0)) {
            // Guesses are vacillating
            ++vacillations;
            more &= vacillations <= 3 || guessCmp <= 0;
          }
          lastCompare = guessCmp;
        }
        guess = newGuess;
        if (more) {
          bigintN = bigintN.Add(EInteger.FromInt32(1));
        }
      }
      return this.RoundToPrecision(guess, ctx);
    }

    private T ExtendPrecision(T thisValue, EContext ctx) {
      if (ctx == null || !ctx.getHasMaxPrecision()) {
        return this.RoundToPrecision(thisValue, ctx);
      }
      EInteger mant = this.helper.GetMantissa(thisValue);
      FastInteger digits =
        this.helper.CreateShiftAccumulator(mant).GetDigitLength();
      FastInteger fastPrecision = FastInteger.FromBig(ctx.getPrecision());
      EInteger exponent = this.helper.GetExponent(thisValue);
      if (digits.compareTo(fastPrecision) < 0) {
        fastPrecision.Subtract(digits);
        mant = this.TryMultiplyByRadixPower(mant, fastPrecision);
        if (mant == null) {
          return this.SignalInvalidWithMessage(
            ctx,
            "Result requires too much memory");
        }
        EInteger bigPrec = fastPrecision.AsEInteger();
        exponent = exponent.Subtract(bigPrec);
      }
      if (ctx != null && ctx.getHasFlags()) {
        ctx.setFlags(ctx.getFlags()|(EContext.FlagRounded));
        ctx.setFlags(ctx.getFlags()|(EContext.FlagInexact));
      }
      return this.RoundToPrecision(
        this.helper.CreateNewWithFlags(mant, exponent, 0),
        ctx);
    }

    private T HandleNotANumber(T thisValue, T other, EContext ctx) {
      int thisFlags = this.helper.GetFlags(thisValue);
      int otherFlags = this.helper.GetFlags(other);
      // Check this value then the other value for signaling NaN
      if ((thisFlags & BigNumberFlags.FlagSignalingNaN) != 0) {
        return this.SignalingNaNInvalid(thisValue, ctx);
      }
      if ((otherFlags & BigNumberFlags.FlagSignalingNaN) != 0) {
        return this.SignalingNaNInvalid(other, ctx);
      }
      // Check this value then the other value for quiet NaN
      return ((thisFlags & BigNumberFlags.FlagQuietNaN) != 0) ?
        this.ReturnQuietNaN(thisValue, ctx) : (((otherFlags &
                BigNumberFlags.FlagQuietNaN) != 0) ? this.ReturnQuietNaN(
  other,
  ctx) : null);
    }

    private boolean IsFinite(T val) {
      return (this.helper.GetFlags(val) & BigNumberFlags.FlagSpecial) == 0;
    }

    private boolean IsNegative(T val) {
      return (this.helper.GetFlags(val) & BigNumberFlags.FlagNegative) != 0;
    }

    private boolean IsWithinExponentRangeForPow(
      T thisValue,
      EContext ctx) {
      if (ctx == null || !ctx.getHasExponentRange()) {
        return true;
      }
      FastInteger digits =

  this.helper.CreateShiftAccumulator(this.helper.GetMantissa(thisValue))
        .GetDigitLength();
      EInteger exp = this.helper.GetExponent(thisValue);
      FastInteger fi = FastInteger.FromBig(exp);
      if (ctx.getAdjustExponent()) {
        fi.Add(digits);
        fi.Decrement();
      }
      // DebugUtility.Log("" + exp + " -> " + fi);
      if (fi.signum() < 0) {
        fi.Negate().Divide(2).Negate();
        // DebugUtility.Log("" + exp + " II -> " + fi);
      }
      exp = fi.AsEInteger();
      return exp.compareTo(ctx.getEMin()) >= 0 && exp.compareTo(ctx.getEMax()) <= 0;
    }

    private T LnInternal(
  T thisValue,
  EInteger workingPrecision,
  EContext ctx) {
      boolean more = true;
      int lastCompare = 0;
      int vacillations = 0;
      EContext ctxdiv = SetPrecisionIfLimited(
        ctx,
        workingPrecision.Add(EInteger.FromInt64(6)))
        .WithRounding(ERounding.OddOrZeroFiveUp);
      T z = this.Add(this.NegateRaw(thisValue), this.helper.ValueOf(1), null);
      T zpow = this.Multiply(z, z, ctxdiv);
      T guess = this.NegateRaw(z);
      T lastGuess = null;
      EInteger denom = EInteger.FromInt64(2);
      while (more) {
        lastGuess = guess;
        T tmp = this.Divide(
  zpow,
  this.helper.CreateNewWithFlags(denom, EInteger.FromInt32(0), 0),
  ctxdiv);
        T newGuess = this.Add(guess, this.NegateRaw(tmp), ctxdiv);
        {
          int guessCmp = this.compareTo(lastGuess, newGuess);
          if (guessCmp == 0) {
            more = false;
          } else if ((guessCmp > 0 && lastCompare < 0) || (lastCompare > 0 &&
                    guessCmp < 0)) {
            // Guesses are vacillating
            ++vacillations;
            more &= vacillations <= 3 || guessCmp <= 0;
          }
          lastCompare = guessCmp;
        }
        guess = newGuess;
        if (more) {
          zpow = this.Multiply(zpow, z, ctxdiv);
          denom = denom.Add(EInteger.FromInt32(1));
        }
      }
      return this.RoundToPrecision(guess, ctx);
    }

    private T LnTenConstant(EContext ctx) {
      T thisValue = this.helper.ValueOf(10);
      FastInteger error;
      EInteger bigError;
      error = new FastInteger(10);
      bigError = error.AsEInteger();
      EContext ctxdiv = SetPrecisionIfLimited(
        ctx,
        ctx.getPrecision().Add(bigError))
        .WithRounding(ERounding.OddOrZeroFiveUp).WithBlankFlags();
      for (int i = 0; i < 9; ++i) {
        thisValue = this.SquareRoot(thisValue, ctxdiv.WithUnlimitedExponents());
      }
      // Find -Ln(1/thisValue)
      thisValue = this.Divide(this.helper.ValueOf(1), thisValue, ctxdiv);
      thisValue = this.LnInternal(thisValue, ctxdiv.getPrecision(), ctxdiv);
      thisValue = this.NegateRaw(thisValue);
      thisValue = this.Multiply(thisValue, this.helper.ValueOf(1 << 9), ctx);
      if (ctx.getHasFlags()) {
        ctx.setFlags(ctx.getFlags()|(EContext.FlagInexact));
        ctx.setFlags(ctx.getFlags()|(EContext.FlagRounded));
      }
      return thisValue;
    }

    private T MinMaxHandleSpecial(
  T thisValue,
  T otherValue,
  EContext ctx,
  boolean isMinOp,
  boolean compareAbs) {
      int thisFlags = this.helper.GetFlags(thisValue);
      int otherFlags = this.helper.GetFlags(otherValue);
      if (((thisFlags | otherFlags) & BigNumberFlags.FlagSpecial) != 0) {
        // Check this value then the other value for signaling NaN
        if ((thisFlags & BigNumberFlags.FlagSignalingNaN) != 0) {
          return this.SignalingNaNInvalid(thisValue, ctx);
        }
        if ((otherFlags & BigNumberFlags.FlagSignalingNaN) != 0) {
          return this.SignalingNaNInvalid(otherValue, ctx);
        }
        // Check this value then the other value for quiet NaN
        if ((thisFlags & BigNumberFlags.FlagQuietNaN) != 0) {
          if ((otherFlags & BigNumberFlags.FlagQuietNaN) != 0) {
            // both values are quiet NaN
            return this.ReturnQuietNaN(thisValue, ctx);
          }
          // return "other" for being numeric
          return this.RoundToPrecision(otherValue, ctx);
        }
        if ((otherFlags & BigNumberFlags.FlagQuietNaN) != 0) {
          // At this point, "thisValue" can't be NaN,
          // return "thisValue" for being numeric
          return this.RoundToPrecision(thisValue, ctx);
        }
        if ((thisFlags & BigNumberFlags.FlagInfinity) != 0) {
          if (compareAbs && (otherFlags & BigNumberFlags.FlagInfinity) == 0) {
            // treat this as larger
            return isMinOp ? this.RoundToPrecision(otherValue, ctx) : thisValue;
          }
          // This value is infinity
          if (isMinOp) {
            // if negative, will be less than every other number
            return ((thisFlags & BigNumberFlags.FlagNegative) != 0) ?
              thisValue : this.RoundToPrecision(otherValue, ctx);
            // if positive, will be greater
          }
          // if positive, will be greater than every other number
          return ((thisFlags & BigNumberFlags.FlagNegative) == 0) ?
            thisValue : this.RoundToPrecision(otherValue, ctx);
        }
        if ((otherFlags & BigNumberFlags.FlagInfinity) != 0) {
          if (compareAbs) {
            // treat this as larger (the first value
            // won't be infinity at this point
            return isMinOp ? this.RoundToPrecision(thisValue, ctx) : otherValue;
          }
          return isMinOp ? (((otherFlags & BigNumberFlags.FlagNegative) ==
                    0) ? this.RoundToPrecision(thisValue, ctx) :
             otherValue) : (((otherFlags & BigNumberFlags.FlagNegative) !=
                0) ? this.RoundToPrecision(thisValue, ctx) : otherValue);
        }
      }
      return null;
    }

    private T MultiplyAddHandleSpecial(
  T op1,
  T op2,
  T op3,
  EContext ctx) {
      int op1Flags = this.helper.GetFlags(op1);
      // Check operands in order for signaling NaN
      if ((op1Flags & BigNumberFlags.FlagSignalingNaN) != 0) {
        return this.SignalingNaNInvalid(op1, ctx);
      }
      int op2Flags = this.helper.GetFlags(op2);
      if ((op2Flags & BigNumberFlags.FlagSignalingNaN) != 0) {
        return this.SignalingNaNInvalid(op2, ctx);
      }
      int op3Flags = this.helper.GetFlags(op3);
      if ((op3Flags & BigNumberFlags.FlagSignalingNaN) != 0) {
        return this.SignalingNaNInvalid(op3, ctx);
      }
      // Check operands in order for quiet NaN
      if ((op1Flags & BigNumberFlags.FlagQuietNaN) != 0) {
        return this.ReturnQuietNaN(op1, ctx);
      }
      if ((op2Flags & BigNumberFlags.FlagQuietNaN) != 0) {
        return this.ReturnQuietNaN(op2, ctx);
      }
      // Check multiplying infinity by 0 (important to check
      // now before checking third operand for quiet NaN because
      // this signals invalid operation and the operation starts
      // with multiplying only the first two operands)
      if ((op1Flags & BigNumberFlags.FlagInfinity) != 0) {
        // Attempt to multiply infinity by 0
        if ((op2Flags & BigNumberFlags.FlagSpecial) == 0 &&
            this.helper.GetMantissa(op2).isZero()) {
          return this.SignalInvalid(ctx);
        }
      }
      if ((op2Flags & BigNumberFlags.FlagInfinity) != 0) {
        // Attempt to multiply infinity by 0
        if ((op1Flags & BigNumberFlags.FlagSpecial) == 0 &&
            this.helper.GetMantissa(op1).isZero()) {
          return this.SignalInvalid(ctx);
        }
      }
      // Now check third operand for quiet NaN
      return ((op3Flags & BigNumberFlags.FlagQuietNaN) != 0) ?
        this.ReturnQuietNaN(op3, ctx) : null;
    }

    private T NegateRaw(T val) {
      if (val == null) {
        return val;
      }
      int sign = this.helper.GetFlags(val) & BigNumberFlags.FlagNegative;
      return this.helper.CreateNewWithFlags(
  this.helper.GetMantissa(val),
  this.helper.GetExponent(val),
  sign == 0 ? BigNumberFlags.FlagNegative : 0);
    }

    private T PowerIntegral(
  T thisValue,
  EInteger powIntBig,
  EContext ctx) {
      int sign = powIntBig.signum();
      T one = this.helper.ValueOf(1);
      if (sign == 0) {
        // however 0 to the power of 0 is undefined
        return this.RoundToPrecision(one, ctx);
      }
      if (powIntBig.equals(EInteger.FromInt32(1))) {
        return this.RoundToPrecision(thisValue, ctx);
      }
      if (powIntBig.equals(EInteger.FromInt64(2))) {
        return this.Multiply(thisValue, thisValue, ctx);
      }
      if (powIntBig.equals(EInteger.FromInt64(3))) {
        return this.Multiply(
  thisValue,
  this.Multiply(thisValue, thisValue, null),
  ctx);
      }
      boolean retvalNeg = this.IsNegative(thisValue) && !powIntBig.isEven();
      FastInteger error = this.helper.CreateShiftAccumulator(
        powIntBig.Abs()).GetDigitLength();
      error = error.Copy();
      error.AddInt(18);
      EInteger bigError = error.AsEInteger();
      EContext ctxdiv = SetPrecisionIfLimited(
        ctx,
        ctx.getPrecision().Add(bigError))
        .WithRounding(ERounding.OddOrZeroFiveUp).WithBlankFlags();
      if (sign < 0) {
        // Use the reciprocal for negative powers
        thisValue = this.Divide(one, thisValue, ctxdiv);
        if ((ctxdiv.getFlags() & EContext.FlagOverflow) != 0) {
          return this.SignalOverflow2(ctx, retvalNeg);
        }
        powIntBig = powIntBig.Negate();
      }
      T r = one;
      while (!powIntBig.isZero()) {
        if (!powIntBig.isEven()) {
          r = this.Multiply(r, thisValue, ctxdiv);
          if ((ctxdiv.getFlags() & EContext.FlagOverflow) != 0) {
            return this.SignalOverflow2(ctx, retvalNeg);
          }
        }
        powIntBig = powIntBig.ShiftRight(1);
        if (!powIntBig.isZero()) {
          ctxdiv.setFlags(0);
          T tmp = this.Multiply(thisValue, thisValue, ctxdiv);
          if ((ctxdiv.getFlags() & EContext.FlagOverflow) != 0) {
            // Avoid multiplying too huge numbers with
            // limited exponent range
            return this.SignalOverflow2(ctx, retvalNeg);
          }
          thisValue = tmp;
        }
        // DebugUtility.Log("r="+r);
      }
      return this.RoundToPrecision(r, ctx);
    }

    private T ReduceToPrecisionAndIdealExponent(
      T thisValue,
      EContext ctx,
      FastInteger precision,
      FastInteger idealExp) {
      T ret = this.RoundToPrecision(thisValue, ctx);
      if (ret != null && (this.helper.GetFlags(ret) &
                    BigNumberFlags.FlagSpecial) == 0) {
        EInteger bigmant = this.helper.GetMantissa(ret);
        FastInteger exp = FastInteger.FromBig(this.helper.GetExponent(ret));
        int radix = this.thisRadix;
        if (bigmant.isZero()) {
          exp = new FastInteger(0);
        } else {
          FastInteger digits = (precision == null) ? null :
            this.helper.CreateShiftAccumulator(bigmant).GetDigitLength();
          bigmant = NumberUtility.ReduceTrailingZeros(
            bigmant,
            exp,
            radix,
            digits,
            precision,
            idealExp);
        }
        int flags = this.helper.GetFlags(thisValue);
        ret = this.helper.CreateNewWithFlags(
  bigmant,
  exp.AsEInteger(),
  flags);
        if (ctx != null && ctx.getClampNormalExponents()) {
          EContext ctxtmp = ctx.WithBlankFlags();
          ret = this.RoundToPrecision(ret, ctxtmp);
          if (ctx.getHasFlags()) {
            ctx.setFlags(ctx.getFlags()|(ctxtmp.getFlags() & ~EContext.FlagClamped));
          }
        }
        ret = this.EnsureSign(ret, (flags & BigNumberFlags.FlagNegative) != 0);
      }
      return ret;
    }

    private T RemainderHandleSpecial(
  T thisValue,
  T other,
  EContext ctx) {
      int thisFlags = this.helper.GetFlags(thisValue);
      int otherFlags = this.helper.GetFlags(other);
      if (((thisFlags | otherFlags) & BigNumberFlags.FlagSpecial) != 0) {
        T result = this.HandleNotANumber(thisValue, other, ctx);
        if ((Object)result != (Object)null) {
          return result;
        }
        if ((thisFlags & BigNumberFlags.FlagInfinity) != 0) {
          return this.SignalInvalid(ctx);
        }
        if ((otherFlags & BigNumberFlags.FlagInfinity) != 0) {
          return this.RoundToPrecision(thisValue, ctx);
        }
      }
      return this.helper.GetMantissa(other).isZero() ? this.SignalInvalid(ctx) :
        null;
    }

    private T ReturnQuietNaN(T thisValue, EContext ctx) {
      EInteger mant = this.helper.GetMantissa(thisValue);
      boolean mantChanged = false;
      if (!mant.isZero() && ctx != null && ctx.getHasMaxPrecision()) {
        FastInteger compPrecision = FastInteger.FromBig(ctx.getPrecision());
        if (this.helper.CreateShiftAccumulator(mant).GetDigitLength()
            .compareTo(compPrecision) >= 0) {
          // Mant's precision is higher than the maximum precision
          EInteger limit = this.TryMultiplyByRadixPower(
            EInteger.FromInt32(1),
            compPrecision);
          if (limit == null) {
            // Limit can't be allocated
            return this.SignalInvalidWithMessage(
  ctx,
  "Result requires too much memory");
          }
          if (mant.compareTo(limit) >= 0) {
            mant = mant.Remainder(limit);
            mantChanged = true;
          }
        }
      }
      int flags = this.helper.GetFlags(thisValue);
      if (!mantChanged && (flags & BigNumberFlags.FlagQuietNaN) != 0) {
        return thisValue;
      }
      flags &= BigNumberFlags.FlagNegative;
      flags |= BigNumberFlags.FlagQuietNaN;
      return this.helper.CreateNewWithFlags(mant, EInteger.FromInt32(0), flags);
    }

    private boolean RoundGivenAccum(
  IShiftAccumulator accum,
  ERounding rounding,
  boolean neg) {
      boolean incremented = false;
      int radix = this.thisRadix;
      int lastDiscarded = accum.getLastDiscardedDigit();
      int olderDiscarded = accum.getOlderDiscardedDigits();
      if (rounding == ERounding.OddOrZeroFiveUp) {
        rounding = (radix == 2) ? ERounding.Odd : ERounding.ZeroFiveUp;
      }
      if (rounding == ERounding.HalfUp) {
        incremented |= lastDiscarded >= (radix / 2);
      } else if (rounding == ERounding.HalfEven) {
        if (lastDiscarded >= (radix / 2)) {
          if (lastDiscarded > (radix / 2) || olderDiscarded != 0) {
            incremented = true;
          } else {
            incremented |= !accum.getShiftedIntFast().isEvenNumber();
          }
        }
      } else if (rounding == ERounding.Ceiling) {
        incremented |= !neg && (lastDiscarded | olderDiscarded) != 0;
      } else if (rounding == ERounding.Floor) {
        incremented |= neg && (lastDiscarded | olderDiscarded) != 0;
      } else if (rounding == ERounding.HalfDown) {
        incremented |= lastDiscarded > (radix / 2) || (lastDiscarded ==
                (radix / 2) && olderDiscarded != 0);
      } else if (rounding == ERounding.Up) {
        incremented |= (lastDiscarded | olderDiscarded) != 0;
      } else if (rounding == ERounding.Odd) {
        incremented |= (lastDiscarded | olderDiscarded) != 0 &&
          accum.getShiftedIntFast().isEvenNumber();
      } else if (rounding == ERounding.ZeroFiveUp) {
        if ((lastDiscarded | olderDiscarded) != 0) {
          if (radix == 2) {
            incremented = true;
          } else {
            int lastDigit = FastIntegerFixed.FromFastInteger(
              accum.getShiftedIntFast()).Mod(radix);
            if (lastDigit == 0 || lastDigit == (radix / 2)) {
              incremented = true;
            }
          }
        }
      }
      return incremented;
    }

    private boolean RoundGivenDigits(
  int lastDiscarded,
  int olderDiscarded,
  ERounding rounding,
  boolean neg,
  EInteger bigval) {
      boolean incremented = false;
      int radix = this.thisRadix;
      if (rounding == ERounding.OddOrZeroFiveUp) {
        rounding = (radix == 2) ? ERounding.Odd : ERounding.ZeroFiveUp;
      }
      if (rounding == ERounding.HalfUp) {
        incremented |= lastDiscarded >= (radix / 2);
      } else if (rounding == ERounding.HalfEven) {
        if (lastDiscarded >= (radix / 2)) {
          if (lastDiscarded > (radix / 2) || olderDiscarded != 0) {
            incremented = true;
          } else {
            incremented |= !bigval.isEven();
          }
        }
      } else if (rounding == ERounding.Ceiling) {
        incremented |= !neg && (lastDiscarded | olderDiscarded) != 0;
      } else if (rounding == ERounding.Floor) {
        incremented |= neg && (lastDiscarded | olderDiscarded) != 0;
      } else if (rounding == ERounding.HalfDown) {
        incremented |= lastDiscarded > (radix / 2) || (lastDiscarded ==
                (radix / 2) && olderDiscarded != 0);
      } else if (rounding == ERounding.Up) {
        incremented |= (lastDiscarded | olderDiscarded) != 0;
      } else if (rounding == ERounding.Odd) {
        incremented |= (lastDiscarded | olderDiscarded) != 0 && bigval.isEven();
      } else if (rounding == ERounding.ZeroFiveUp) {
        if ((lastDiscarded | olderDiscarded) != 0) {
          if (radix == 2) {
            incremented = true;
          } else {
            EInteger bigdigit = bigval.Remainder(EInteger.FromInt64(radix));
            int lastDigit = bigdigit.ToInt32Checked();
            if (lastDigit == 0 || lastDigit == (radix / 2)) {
              incremented = true;
            }
          }
        }
      }
      return incremented;
    }

    // binaryPrec means whether precision is the number of bits and not
    // digits
    private T RoundToPrecisionInternal(
  T thisValue,
  int lastDiscarded,
  int olderDiscarded,
  FastInteger shift,
  boolean adjustNegativeZero,
  EContext ctx) {
      // If context has unlimited precision and exponent range,
      // and no discarded digits or shifting
      boolean unlimitedPrecisionExp = ctx == null ||
         (!ctx.getHasMaxPrecision() && !ctx.getHasExponentRange());
      int thisFlags = this.helper.GetFlags(thisValue);
      if ((thisFlags & BigNumberFlags.FlagSpecial) != 0) {
        if ((thisFlags & BigNumberFlags.FlagSignalingNaN) != 0) {
          if (ctx != null && ctx.getHasFlags()) {
            ctx.setFlags(ctx.getFlags()|(EContext.FlagInvalid));
          }
          return this.ReturnQuietNaN(thisValue, ctx);
        }
        if ((thisFlags & BigNumberFlags.FlagQuietNaN) != 0) {
          return this.ReturnQuietNaN(thisValue, ctx);
        }
        if ((thisFlags & BigNumberFlags.FlagInfinity) != 0) {
          return thisValue;
        }
      }
      if (unlimitedPrecisionExp &&
         (lastDiscarded | olderDiscarded) == 0 &&
         (shift == null || shift.isValueZero())) {
        if (!(adjustNegativeZero &&
          (thisFlags & BigNumberFlags.FlagNegative) != 0 &&
          this.helper.GetMantissa(thisValue).isZero())) {
          return thisValue;
        }
      }
      if (unlimitedPrecisionExp &&
        (ctx == null || (!ctx.getHasFlags() && ctx.getTraps() == 0)) &&
        (shift == null || shift.isValueZero())) {
        ERounding er = (ctx == null) ? ERounding.HalfDown : ctx.getRounding();
        boolean negative = (thisFlags & BigNumberFlags.FlagNegative) != 0;
        boolean negzero = adjustNegativeZero && negative &&
        this.helper.GetMantissa(thisValue).isZero() &&
        (er != ERounding.Floor);
        if (!negzero) {
          if (er == ERounding.Down) {
            return thisValue;
          }
          if (this.thisRadix == 10 && (er == ERounding.HalfEven) &&
            lastDiscarded < 5) {
            return thisValue;
          }
          if (this.thisRadix == 10 && (er == ERounding.HalfEven) &&
            (lastDiscarded > 5 || olderDiscarded != 0)) {
            FastIntegerFixed bm = this.helper.GetMantissaFastInt(thisValue);
            return this.helper.CreateNewWithFlagsFastInt(
              FastIntegerFixed.Add(bm, FastIntegerFixed.One),
              this.helper.GetExponentFastInt(thisValue),
              thisFlags);
          }
          if (this.thisRadix == 2 && (er == ERounding.HalfEven) &&
            lastDiscarded == 0) {
            return thisValue;
          }
          if (!this.RoundGivenDigits(
 lastDiscarded,
 olderDiscarded,
 er,
 negative,
            this.helper.GetMantissa(thisValue))) {
            return thisValue;
          } else {
            FastIntegerFixed bm = this.helper.GetMantissaFastInt(thisValue);
            return this.helper.CreateNewWithFlagsFastInt(
              FastIntegerFixed.Add(bm, FastIntegerFixed.One),
              this.helper.GetExponentFastInt(thisValue),
              thisFlags);
          }
        }
      }
ctx = (ctx == null) ? (EContext.UnlimitedHalfEven.WithRounding(ERounding.HalfEven)) : ctx;
      boolean binaryPrec = ctx.isPrecisionInBits();
      // get the precision
      FastInteger fastPrecision = ctx.getPrecision().CanFitInInt32() ? new
    FastInteger(ctx.getPrecision().ToInt32Checked()) :
      FastInteger.FromBig(ctx.getPrecision());
      // No need to check if precision is less than 0, since the
      // PrecisionContext Object should already ensure this
      binaryPrec &= this.thisRadix != 2 && !fastPrecision.isValueZero();
      IShiftAccumulator accum = null;
      FastInteger fastAdjustedExp;
      FastInteger fastNormalMin;
      FastInteger fastEMin = null;
      FastInteger fastEMax = null;
      // get the exponent range
      if (ctx != null && ctx.getHasExponentRange()) {
        fastEMax = ctx.getEMax().CanFitInInt32() ? new
       FastInteger(ctx.getEMax().ToInt32Checked()) : FastInteger.FromBig(ctx.getEMax());
        fastEMin = ctx.getEMin().CanFitInInt32() ? new
       FastInteger(ctx.getEMin().ToInt32Checked()) : FastInteger.FromBig(ctx.getEMin());
      }
      ERounding rounding = (ctx == null) ? ERounding.HalfEven : ctx.getRounding();
      boolean unlimitedPrec = fastPrecision.isValueZero();
      if (!binaryPrec) {
        // if (total%100 == 0) {
        // DebugUtility.Log("used=" + used + "/" + eligible + "/" + total);
        // }
        // Fast path to check if rounding is necessary at all
        // NOTE: At this point, the number won't be infinity or NaN
        if (fastPrecision.signum() > 0 && (shift == null || shift.isValueZero())) {
          FastIntegerFixed mantabs = this.helper.GetMantissaFastInt(thisValue);
          if (adjustNegativeZero && (thisFlags & BigNumberFlags.FlagNegative) !=
              0 && mantabs.isValueZero() && (ctx.getRounding() != ERounding.Floor)) {
            // Change negative zero to positive zero
            // except if the rounding mode is Floor
            thisValue = this.EnsureSign(thisValue, false);
            thisFlags = 0;
          }
          accum = this.helper.CreateShiftAccumulatorWithDigitsFastInt(
            mantabs,
            lastDiscarded,
            olderDiscarded);
          FastInteger digitCount = accum.GetDigitLength();
          if (digitCount.compareTo(fastPrecision) <= 0) {
            if (!this.RoundGivenAccum(
  accum,
  ctx.getRounding(),
  (thisFlags & BigNumberFlags.FlagNegative) != 0)) {
              if (ctx.getHasFlags() && (lastDiscarded | olderDiscarded) != 0) {
                ctx.setFlags(ctx.getFlags()|(EContext.FlagInexact | EContext.FlagRounded));
              }
              if (!ctx.getHasExponentRange()) {
                return thisValue;
              }
           FastIntegerFixed bigexp = this.helper.GetExponentFastInt(thisValue);
              if (ctx == null || ctx.getAdjustExponent()) {
                fastAdjustedExp = bigexp.ToFastInteger()
                  .Add(fastPrecision).Decrement();
                fastNormalMin = fastEMin.Copy()
                  .Add(fastPrecision).Decrement();
              } else {
                fastAdjustedExp = bigexp.ToFastInteger();
                fastNormalMin = fastEMin;
              }
              // DebugUtility.Log("{0}->{1},{2}"
              // , fastAdjustedExp, fastEMax, fastNormalMin);
              if (fastAdjustedExp.compareTo(fastEMax) <= 0 &&
                  fastAdjustedExp.compareTo(fastNormalMin) >= 0) {
                return thisValue;
              }
            } else {
              if (ctx.getHasFlags() && (lastDiscarded | olderDiscarded) != 0) {
                ctx.setFlags(ctx.getFlags()|(EContext.FlagInexact | EContext.FlagRounded));
              }
              boolean stillWithinPrecision = false;
              mantabs = mantabs.Increment();
              int precisionCmp = digitCount.compareTo(fastPrecision);
              if (precisionCmp < 0 ||
                  (precisionCmp == 0 && (this.thisRadix & 1) == 0 &&
                  !mantabs.isEvenNumber())) {
                stillWithinPrecision = true;
              } else {
                EInteger radixPower =
                  this.TryMultiplyByRadixPower(EInteger.FromInt32(1), fastPrecision);
                // DebugUtility.Log("now " + mantabs + "," + fastPrecision);
                if (radixPower == null) {
                  return this.SignalInvalidWithMessage(
                ctx,
                "Result requires too much memory");
                }
            stillWithinPrecision =
                  mantabs.ToEInteger().compareTo(radixPower) <
                    0;
              }
              if (stillWithinPrecision) {
                if (!ctx.getHasExponentRange()) {
                  return this.helper.CreateNewWithFlagsFastInt(
                    mantabs,
                    this.helper.GetExponentFastInt(thisValue),
                    thisFlags);
                }
           FastIntegerFixed bigexp = this.helper.GetExponentFastInt(thisValue);
                if (ctx == null || ctx.getAdjustExponent()) {
                  fastAdjustedExp = bigexp.ToFastInteger()
                    .Add(fastPrecision).Decrement();
                  fastNormalMin = fastEMin.Copy()
                    .Add(fastPrecision).Decrement();
                } else {
                  fastAdjustedExp = bigexp.ToFastInteger();
                  fastNormalMin = fastEMin;
                }
                if (fastAdjustedExp.compareTo(fastEMax) <= 0 &&
                    fastAdjustedExp.compareTo(fastNormalMin) >= 0) {
                  return this.helper.CreateNewWithFlagsFastInt(
  mantabs,
  bigexp,
  thisFlags);
                }
              }
            }
          }
        }
      }
      if (adjustNegativeZero && (thisFlags & BigNumberFlags.FlagNegative) !=
          0 && (rounding != ERounding.Floor) &&
         this.helper.GetMantissa(thisValue).isZero()) {
        // Change negative zero to positive zero
        // except if the rounding mode is Floor
        thisValue = this.EnsureSign(thisValue, false);
        thisFlags = 0;
      }
      boolean neg = (thisFlags & BigNumberFlags.FlagNegative) != 0;
      FastIntegerFixed bigmantissa = this.helper.GetMantissaFastInt(thisValue);
      boolean mantissaWasZero = bigmantissa.isValueZero() && (lastDiscarded |
                    olderDiscarded) == 0;
   FastInteger exp = this.helper.GetExponentFastInt(thisValue).ToFastInteger();
      int flags = 0;
      if (accum == null) {
accum = this.helper.CreateShiftAccumulatorWithDigitsFastInt(
          bigmantissa,
          lastDiscarded,
          olderDiscarded);
}

      FastInteger bitLength = fastPrecision;
      if (binaryPrec) {
      fastPrecision = this.MaxDigitLengthForBitLength(fastPrecision);
      }
      boolean nonHalfRounding = rounding != ERounding.HalfEven &&
        rounding != ERounding.HalfUp && rounding != ERounding.HalfDown;
      if (!unlimitedPrec) {
        accum.ShiftToDigits(fastPrecision, shift, nonHalfRounding);
      } else {
        if (shift != null && shift.signum() != 0) {
          if (nonHalfRounding) {
            accum.TruncateRight(shift);
          } else {
            accum.ShiftRight(shift);
          }
        }
        fastPrecision = accum.GetDigitLength();
      }
      if (binaryPrec) {
        while (this.IsHigherThanBitLength(accum.getShiftedInt(), bitLength)) {
          accum.ShiftRightInt(1);
        }
      }
      FastInteger discardedBits = accum.getDiscardedDigitCount().Copy();
      exp.Add(discardedBits);
      FastInteger adjExponent;
      adjExponent = ctx.getAdjustExponent() ?
        exp.Copy().Add(accum.GetDigitLength()).Decrement() : exp.Copy();
      if (binaryPrec && fastEMax != null && adjExponent.compareTo(fastEMax)
          == 0) {
        // May or may not be an overflow depending on the mantissa
        FastInteger expdiff =
          fastPrecision.Copy().Subtract(accum.GetDigitLength());
        EInteger currMantissa = accum.getShiftedInt();
        currMantissa = this.TryMultiplyByRadixPower(currMantissa, expdiff);
        if (currMantissa == null) {
          return this.SignalInvalidWithMessage(
            ctx,
            "Result requires too much memory");
        }
        if (this.IsHigherThanBitLength(currMantissa, bitLength)) {
          // Mantissa too high, treat as overflow
          adjExponent.Increment();
        }
      }
      if (fastEMax != null && adjExponent.compareTo(fastEMax) > 0) {
        if (mantissaWasZero) {
          if (ctx.getHasFlags()) {
            ctx.setFlags(ctx.getFlags()|(flags | EContext.FlagClamped));
          }
          if (ctx.getClampNormalExponents()) {
            // Clamp exponents to eMax + 1 - precision
            // if directed
            FastInteger clampExp = fastEMax.Copy();
            if (ctx.getAdjustExponent()) {
              clampExp.Increment().Subtract(fastPrecision);
            }
            if (fastEMax.compareTo(clampExp) > 0) {
              if (ctx.getHasFlags()) {
                ctx.setFlags(ctx.getFlags()|(EContext.FlagClamped));
              }
              fastEMax = clampExp;
            }
          }
          return this.helper.CreateNewWithFlagsFastInt(
  bigmantissa,
  FastIntegerFixed.FromFastInteger(fastEMax),
  thisFlags);
        }
        // Overflow
        flags |= EContext.FlagOverflow |
          EContext.FlagInexact | EContext.FlagRounded;
        if (rounding == ERounding.None) {
          return this.SignalInvalidWithMessage(ctx, "Rounding was required");
        }
        if (!unlimitedPrec && (rounding == ERounding.Down || rounding ==
                ERounding.ZeroFiveUp ||
              (rounding == ERounding.OddOrZeroFiveUp && this.thisRadix != 2) ||
                (rounding == ERounding.Ceiling && neg) || (rounding ==
                  ERounding.Floor && !neg))) {
          // Set to the highest possible value for
          // the given precision
          EInteger overflowMant = EInteger.FromInt32(0);
          if (binaryPrec) {
            overflowMant = ShiftedMask(bitLength);
          } else {
            overflowMant = this.TryMultiplyByRadixPower(
              EInteger.FromInt32(1),
              fastPrecision);
            if (overflowMant == null) {
              return this.SignalInvalidWithMessage(
                ctx,
                "Result requires too much memory");
            }
            overflowMant = overflowMant.Subtract(EInteger.FromInt32(1));
          }
          if (ctx.getHasFlags()) {
            ctx.setFlags(ctx.getFlags()|(flags));
          }
          FastInteger clamp = ctx.getAdjustExponent() ?
            fastEMax.Copy().Increment().Subtract(fastPrecision) :
            fastEMax;
          return this.helper.CreateNewWithFlagsFastInt(
            FastIntegerFixed.FromBig(overflowMant),
            FastIntegerFixed.FromFastInteger(clamp),
            neg ? BigNumberFlags.FlagNegative : 0);
        }
        if (ctx.getHasFlags()) {
          ctx.setFlags(ctx.getFlags()|(flags));
        }
        return this.SignalOverflow(neg);
      }
      if (fastEMin != null && adjExponent.compareTo(fastEMin) < 0) {
        // Subnormal
        FastInteger fastETiny = fastEMin;
        if (ctx.getAdjustExponent()) {
          fastETiny = fastETiny.Copy().Subtract(fastPrecision).Increment();
        }
        if (ctx.getHasFlags()) {
          if (!accum.getShiftedInt().isZero()) {
            FastInteger newAdjExponent = adjExponent;
            if (this.RoundGivenAccum(accum, rounding, neg)) {
              EInteger earlyRounded = accum.getShiftedInt().Add(EInteger.FromInt32(1));
          if (!unlimitedPrec && (earlyRounded.isEven() || (this.thisRadix & 1) !=
                0)) {
                IShiftAccumulator accum2 =
                  this.helper.CreateShiftAccumulator(earlyRounded);
                FastInteger newDigitLength = accum2.GetDigitLength();
                // Ensure newDigitLength doesn't exceed precision
                if (binaryPrec || newDigitLength.compareTo(fastPrecision) > 0) {
                  newDigitLength = fastPrecision.Copy();
                }
                newAdjExponent = ctx.getAdjustExponent() ?
                  exp.Copy().Add(newDigitLength).Decrement() : exp;
              }
            }
            if (newAdjExponent.compareTo(fastEMin) < 0) {
              // DebugUtility.Log("subnormal");
              flags |= EContext.FlagSubnormal;
            }
          }
        }
        // DebugUtility.Log("exp=" + exp + " eTiny=" + fastETiny);
        FastInteger subExp = exp.Copy();
        // DebugUtility.Log("exp=" + subExp + " eTiny=" + fastETiny);
        if (subExp.compareTo(fastETiny) < 0) {
          // DebugUtility.Log("Less than ETiny");
          FastInteger expdiff = fastETiny.Copy().Subtract(exp);
          // DebugUtility.Log("<ETiny: " + (accum.getShiftedInt()));
          if (nonHalfRounding) {
            accum.TruncateRight(expdiff);
          } else {
            accum.ShiftRight(expdiff);
          }
          // DebugUtility.Log("<ETiny2: " + (accum.getShiftedInt()));
          FastInteger newmantissa = accum.getShiftedIntFast();
          boolean nonZeroDiscardedDigits = (accum.getLastDiscardedDigit() |
            accum.getOlderDiscardedDigits()) != 0;
          // DebugUtility.Log("<nzdd= " + nonZeroDiscardedDigits);
          if (nonZeroDiscardedDigits && rounding == ERounding.None) {
            return this.SignalInvalidWithMessage(
              ctx,
              "Rounding was required");
          }
          if (accum.getDiscardedDigitCount().signum() != 0 || nonZeroDiscardedDigits) {
            if (ctx.getHasFlags()) {
              if (!mantissaWasZero) {
                flags |= EContext.FlagRounded;
              }
              if (nonZeroDiscardedDigits) {
                flags |= EContext.FlagInexact |
                  EContext.FlagRounded;
              }
            }
            if (this.RoundGivenAccum(accum, rounding, neg)) {
              newmantissa.Increment();
            }
          }
          if (ctx.getHasFlags()) {
            if (newmantissa.isValueZero()) {
              flags |= EContext.FlagClamped;
            }
            // DebugUtility.Log("" + flags + "," + (flags &
            // (EContext.FlagSubnormal |
            // EContext.FlagInexact)));
            if ((flags & (EContext.FlagSubnormal |
            EContext.FlagInexact)) == (EContext.FlagSubnormal |
                 EContext.FlagInexact)) {
              flags |= EContext.FlagUnderflow |
                EContext.FlagRounded;
            }
            ctx.setFlags(ctx.getFlags()|(flags));
          }
          if (ctx.getClampNormalExponents()) {
            // Clamp exponents to eMax + 1 - precision
            // if directed
            FastInteger clampExp = fastEMax.Copy();
            if (ctx.getAdjustExponent()) {
              clampExp.Increment().Subtract(fastPrecision);
            }
            if (fastETiny.compareTo(clampExp) > 0) {
              if (!newmantissa.isValueZero()) {
                expdiff = fastETiny.Copy().Subtract(clampExp);
                // Change bigmantissa for use
                // in the return value
                bigmantissa = this.TryMultiplyByRadixPowerFastInt(
                FastIntegerFixed.FromFastInteger(newmantissa),
                expdiff);
                if (bigmantissa == null) {
                  return this.SignalInvalidWithMessage(
                ctx,
                "Result requires too much memory");
                }
                if (ctx.getHasFlags()) {
                  ctx.setFlags(ctx.getFlags()|(EContext.FlagClamped));
                }
                return this.helper.CreateNewWithFlagsFastInt(
                  bigmantissa,
                  FastIntegerFixed.FromFastInteger(clampExp),
                  neg ? BigNumberFlags.FlagNegative : 0);
              }
              if (ctx.getHasFlags()) {
                ctx.setFlags(ctx.getFlags()|(EContext.FlagClamped));
              }
              fastETiny = clampExp;
            }
          }
          if (ctx.getHasFlags()) {
                ctx.setFlags(ctx.getFlags()|(flags));
          }
          return this.helper.CreateNewWithFlagsFastInt(
  FastIntegerFixed.FromFastInteger(newmantissa),
  FastIntegerFixed.FromFastInteger(fastETiny),
  neg ? BigNumberFlags.FlagNegative : 0);
        }
      }
      // DebugUtility.Log("" + accum.getShiftedInt() + ", exp=" + (//
      // adjExponent) + "/" + fastEMin);
      boolean recheckOverflow = false;
      boolean doRounding = accum.getDiscardedDigitCount().signum() != 0 ||
        (accum.getLastDiscardedDigit() | accum.getOlderDiscardedDigits()) != 0;
      if (doRounding) {
        if (!bigmantissa.isValueZero()) {
          flags |= EContext.FlagRounded;
        }
        bigmantissa = FastIntegerFixed.FromFastInteger(accum.getShiftedIntFast());
        if ((accum.getLastDiscardedDigit() | accum.getOlderDiscardedDigits()) != 0) {
          flags |= EContext.FlagInexact | EContext.FlagRounded;
          if (rounding == ERounding.None) {
            return this.SignalInvalidWithMessage(ctx, "Rounding was required");
          }
        }
        if (this.RoundGivenAccum(accum, rounding, neg)) {
          // DebugUtility.Log("recheck overflow {0} {1} / {2}"
          // , adjExponent, fastEMax, accum.getShiftedInt());
          bigmantissa = bigmantissa.Increment();
          recheckOverflow |= binaryPrec;
          // Check if mantissa's precision is now greater
          // than the one set by the context
          if (!unlimitedPrec && (bigmantissa.isEvenNumber() || (this.thisRadix &
                1) != 0) && (binaryPrec ||
                accum.GetDigitLength().compareTo(fastPrecision) >=
                    0)) {
            accum = this.helper.CreateShiftAccumulatorWithDigitsFastInt(
              bigmantissa,
              0,
              0);
            FastInteger newDigitLength = accum.GetDigitLength();
            if (binaryPrec || newDigitLength.compareTo(fastPrecision) > 0) {
              FastInteger neededShift =
                newDigitLength.Copy().Subtract(fastPrecision);
              if (nonHalfRounding) {
                accum.TruncateRight(neededShift);
              } else {
                accum.ShiftRight(neededShift);
              }
              if (binaryPrec) {
              while (
  this.IsHigherThanBitLength(
  accum.getShiftedInt(),
  bitLength)) {
                  accum.ShiftRightInt(1);
                }
              }
              if (accum.getDiscardedDigitCount().signum() != 0) {
                exp.Add(accum.getDiscardedDigitCount());
                discardedBits.Add(accum.getDiscardedDigitCount());
                bigmantissa = FastIntegerFixed.FromFastInteger(
                  accum.getShiftedIntFast());
                recheckOverflow |= !binaryPrec;
              }
            }
          }
        }
      }
      if (fastEMax != null && recheckOverflow) {
        // Check for overflow again
        // DebugUtility.Log("recheck overflow2 {0} {1} / {2}"
        // , adjExponent, fastEMax, accum.getShiftedInt());
        adjExponent = exp.Copy();
        if (ctx.getAdjustExponent()) {
          adjExponent.Add(accum.GetDigitLength()).Decrement();
        }
        if (binaryPrec && fastEMax != null &&
            adjExponent.compareTo(fastEMax) == 0) {
          // May or may not be an overflow depending on the mantissa
          // (uses accumulator from previous steps, including the check
          // if the mantissa now exceeded the precision)
          FastInteger expdiff =
            fastPrecision.Copy().Subtract(accum.GetDigitLength());
          EInteger currMantissa = accum.getShiftedInt();
          currMantissa = this.TryMultiplyByRadixPower(currMantissa, expdiff);
          if (currMantissa == null) {
            return this.SignalInvalidWithMessage(
              ctx,
              "Result requires too much memory");
          }
          if (this.IsHigherThanBitLength(currMantissa, bitLength)) {
            // Mantissa too high, treat as overflow
            adjExponent.Increment();
          }
        }
        if (adjExponent.compareTo(fastEMax) > 0) {
          flags |= EContext.FlagOverflow |
            EContext.FlagInexact | EContext.FlagRounded;
          if (!unlimitedPrec && (rounding == ERounding.Down || rounding ==
                   ERounding.ZeroFiveUp ||
        (rounding == ERounding.OddOrZeroFiveUp || rounding == ERounding.Odd) ||
            (rounding == ERounding.Ceiling &&
                neg) || (rounding == ERounding.Floor && !neg))) {
            // Set to the highest possible value for
            // the given precision
            EInteger overflowMant = EInteger.FromInt32(0);
            if (binaryPrec) {
              overflowMant = ShiftedMask(bitLength);
            } else {
              overflowMant = this.TryMultiplyByRadixPower(
                EInteger.FromInt32(1),
                fastPrecision);
              if (overflowMant == null) {
                return this.SignalInvalidWithMessage(
                ctx,
                "Result requires too much memory");
              }
              overflowMant = overflowMant.Subtract(EInteger.FromInt32(1));
            }
            if (ctx.getHasFlags()) {
              ctx.setFlags(ctx.getFlags()|(flags));
            }
            FastInteger clamp;
            clamp = ctx.getAdjustExponent() ?
              fastEMax.Copy().Increment().Subtract(fastPrecision) :
              fastEMax;
            return this.helper.CreateNewWithFlags(
              overflowMant,
              clamp.AsEInteger(),
              neg ? BigNumberFlags.FlagNegative : 0);
          }
          if (ctx.getHasFlags()) {
            ctx.setFlags(ctx.getFlags()|(flags));
          }
          return this.SignalOverflow(neg);
        }
      }
      if (ctx.getHasFlags()) {
        ctx.setFlags(ctx.getFlags()|(flags));
      }
      if (ctx.getClampNormalExponents()) {
        // Clamp exponents to eMax + 1 - precision
        // if directed
        FastInteger clampExp = fastEMax.Copy();
        if (ctx.getAdjustExponent()) {
          clampExp.Increment().Subtract(fastPrecision);
        }
        if (exp.compareTo(clampExp) > 0) {
          if (!bigmantissa.isValueZero()) {
            FastInteger expdiff = exp.Copy().Subtract(clampExp);
            // DebugUtility.Log("Clamping " + exp + " to " + clampExp);
            bigmantissa = this.TryMultiplyByRadixPowerFastInt(
              bigmantissa,
              expdiff);
            if (bigmantissa == null) {
              return this.SignalInvalidWithMessage(
                ctx,
                "Result requires too much memory");
            }
          }
          if (ctx.getHasFlags()) {
            ctx.setFlags(ctx.getFlags()|(EContext.FlagClamped));
          }
          exp = clampExp;
        }
      }
      return this.helper.CreateNewWithFlagsFastInt(
  bigmantissa,
  FastIntegerFixed.FromFastInteger(exp),
  neg ? BigNumberFlags.FlagNegative : 0);
    }

    private T RoundToScale(
  EInteger mantissa,
  EInteger remainder,
  EInteger divisor,
  EInteger desiredExponent,
  FastInteger shift,
  boolean neg,
  EContext ctx) {
      IShiftAccumulator accum;
      ERounding rounding = (ctx == null) ? ERounding.HalfEven : ctx.getRounding();
      int lastDiscarded = 0;
      int olderDiscarded = 0;
      if (!remainder.isZero()) {
        if (rounding == ERounding.HalfDown || rounding == ERounding.HalfUp ||
            rounding == ERounding.HalfEven) {
          EInteger halfDivisor = divisor.ShiftRight(1);
          int cmpHalf = remainder.compareTo(halfDivisor);
          if ((cmpHalf == 0) && divisor.isEven()) {
            // remainder is exactly half
            lastDiscarded = this.thisRadix / 2;
            olderDiscarded = 0;
          } else if (cmpHalf > 0) {
            // remainder is greater than half
            lastDiscarded = this.thisRadix / 2;
            olderDiscarded = 1;
          } else {
            // remainder is less than half
            lastDiscarded = 0;
            olderDiscarded = 1;
          }
        } else {
          // Rounding mode doesn't care about
          // whether remainder is exactly half
          if (rounding == ERounding.None) {
            return this.SignalInvalidWithMessage(ctx, "Rounding was required");
          }
          lastDiscarded = 1;
          olderDiscarded = 1;
        }
      }
      int flags = 0;
      EInteger newmantissa = mantissa;
      if (shift.isValueZero()) {
        if ((lastDiscarded | olderDiscarded) != 0) {
          flags |= EContext.FlagInexact | EContext.FlagRounded;
          if (rounding == ERounding.None) {
            return this.SignalInvalidWithMessage(ctx, "Rounding was required");
          }
          if (
            this.RoundGivenDigits(
  lastDiscarded,
  olderDiscarded,
  rounding,
  neg,
  newmantissa)) {
            newmantissa = newmantissa.Add(EInteger.FromInt32(1));
          }
        }
      } else {
        accum = this.helper.CreateShiftAccumulatorWithDigits(
          mantissa,
          lastDiscarded,
          olderDiscarded);
        accum.ShiftRight(shift);
        newmantissa = accum.getShiftedInt();
        if (accum.getDiscardedDigitCount().signum() != 0 ||
            (accum.getLastDiscardedDigit() | accum.getOlderDiscardedDigits()) !=
            0) {
          if (!mantissa.isZero()) {
            flags |= EContext.FlagRounded;
          }
          if ((accum.getLastDiscardedDigit() | accum.getOlderDiscardedDigits()) != 0) {
            flags |= EContext.FlagInexact | EContext.FlagRounded;
            if (rounding == ERounding.None) {
              return this.SignalInvalidWithMessage(
                ctx,
                "Rounding was required");
            }
          }
          if (this.RoundGivenAccum(accum, rounding, neg)) {
            newmantissa = newmantissa.Add(EInteger.FromInt32(1));
          }
        }
      }
      if (ctx.getHasFlags()) {
        ctx.setFlags(ctx.getFlags()|(flags));
      }
      return this.helper.CreateNewWithFlags(
        newmantissa,
        desiredExponent,
        neg ? BigNumberFlags.FlagNegative : 0);
    }

    private int[] RoundToScaleStatus(
  EInteger remainder,
  EInteger divisor,
  EContext ctx) {
      ERounding rounding = (ctx == null) ? ERounding.HalfEven : ctx.getRounding();
      int lastDiscarded = 0;
      int olderDiscarded = 0;
      if (!remainder.isZero()) {
        if (rounding == ERounding.HalfDown || rounding == ERounding.HalfUp ||
            rounding == ERounding.HalfEven) {
          EInteger halfDivisor = divisor.ShiftRight(1);
          int cmpHalf = remainder.compareTo(halfDivisor);
          if ((cmpHalf == 0) && divisor.isEven()) {
            // remainder is exactly half
            lastDiscarded = this.thisRadix / 2;
            olderDiscarded = 0;
          } else if (cmpHalf > 0) {
            // remainder is greater than half
            lastDiscarded = this.thisRadix / 2;
            olderDiscarded = 1;
          } else {
            // remainder is less than half
            lastDiscarded = 0;
            olderDiscarded = 1;
          }
        } else {
          // Rounding mode doesn't care about
          // whether remainder is exactly half
          if (rounding == ERounding.None) {
            // Rounding was required
            return null;
          }
          lastDiscarded = 1;
          olderDiscarded = 1;
        }
      }
      return new int[] { lastDiscarded, olderDiscarded };
    }

    private T SignalDivideByZero(EContext ctx, boolean neg) {
      if (ctx != null && ctx.getHasFlags()) {
        ctx.setFlags(ctx.getFlags()|(EContext.FlagDivideByZero));
      }
      if (this.support == BigNumberFlags.FiniteOnly) {
        throw new ArithmeticException("Division by zero");
      }
      return this.helper.CreateNewWithFlags(
        EInteger.FromInt32(0),
        EInteger.FromInt32(0),
        BigNumberFlags.FlagInfinity | (neg ? BigNumberFlags.FlagNegative : 0));
    }

    private T SignalingNaNInvalid(T value, EContext ctx) {
      if (ctx != null && ctx.getHasFlags()) {
        ctx.setFlags(ctx.getFlags()|(EContext.FlagInvalid));
      }
      return this.ReturnQuietNaN(value, ctx);
    }

    private T SignalInvalid(EContext ctx) {
      if (ctx != null && ctx.getHasFlags()) {
        ctx.setFlags(ctx.getFlags()|(EContext.FlagInvalid));
      }
      if (this.support == BigNumberFlags.FiniteOnly) {
        throw new ArithmeticException("Invalid operation");
      }
      return this.helper.CreateNewWithFlags(
        EInteger.FromInt32(0),
        EInteger.FromInt32(0),
        BigNumberFlags.FlagQuietNaN);
    }

    private T SignalInvalidWithMessage(EContext ctx, String str) {
      if (ctx != null && ctx.getHasFlags()) {
        ctx.setFlags(ctx.getFlags()|(EContext.FlagInvalid));
      }
      if (this.support == BigNumberFlags.FiniteOnly) {
        throw new ArithmeticException(str);
      }
      return this.helper.CreateNewWithFlags(
  EInteger.FromInt32(0),
  EInteger.FromInt32(0),
  BigNumberFlags.FlagQuietNaN);
    }

    private T SignalOverflow(boolean neg) {
      return this.support == BigNumberFlags.FiniteOnly ? null :
        this.helper.CreateNewWithFlags(
  EInteger.FromInt32(0),
  EInteger.FromInt32(0),
  (neg ? BigNumberFlags.FlagNegative : 0) | BigNumberFlags.FlagInfinity);
    }

    private T SignalOverflow2(EContext ctx, boolean neg) {
      if (ctx != null) {
        ERounding roundingOnOverflow = ctx.getRounding();
        if (ctx.getHasFlags()) {
          ctx.setFlags(ctx.getFlags()|(EContext.FlagOverflow |
            EContext.FlagInexact | EContext.FlagRounded));
        }
        if (ctx.getHasMaxPrecision() && ctx.getHasExponentRange() &&
            (roundingOnOverflow == ERounding.Down || roundingOnOverflow ==
             ERounding.ZeroFiveUp ||
                (roundingOnOverflow == ERounding.OddOrZeroFiveUp ||
                  roundingOnOverflow == ERounding.Odd) ||
             (roundingOnOverflow == ERounding.Ceiling && neg) ||
             (roundingOnOverflow == ERounding.Floor && !neg))) {
          // Set to the highest possible value for
          // the given precision
          EInteger overflowMant = EInteger.FromInt32(0);
          FastInteger fastPrecision = FastInteger.FromBig(ctx.getPrecision());
          overflowMant = this.TryMultiplyByRadixPower(
            EInteger.FromInt32(1),
            fastPrecision);
          if (overflowMant == null) {
            return this.SignalInvalidWithMessage(
              ctx,
              "Result requires too much memory");
          }
          overflowMant = overflowMant.Subtract(EInteger.FromInt32(1));
          FastInteger clamp = FastInteger.FromBig(ctx.getEMax());
          if (ctx.getAdjustExponent()) {
            clamp.Increment().Subtract(fastPrecision);
          }
          return this.helper.CreateNewWithFlags(
            overflowMant,
            clamp.AsEInteger(),
            neg ? BigNumberFlags.FlagNegative : 0);
        }
      }
      return this.SignalOverflow(neg);
    }

    private T SquareRootHandleSpecial(T thisValue, EContext ctx) {
      int thisFlags = this.helper.GetFlags(thisValue);
      if ((thisFlags & BigNumberFlags.FlagSpecial) != 0) {
        if ((thisFlags & BigNumberFlags.FlagSignalingNaN) != 0) {
          return this.SignalingNaNInvalid(thisValue, ctx);
        }
        if ((thisFlags & BigNumberFlags.FlagQuietNaN) != 0) {
          return this.ReturnQuietNaN(thisValue, ctx);
        }
        if ((thisFlags & BigNumberFlags.FlagInfinity) != 0) {
          // Square root of infinity
          return ((thisFlags & BigNumberFlags.FlagNegative) != 0) ?
            this.SignalInvalid(ctx) : thisValue;
        }
      }
      int sign = this.helper.GetSign(thisValue);
      return (sign < 0) ? this.SignalInvalid(ctx) : null;
    }

    private EInteger TryMultiplyByRadixPower(
      EInteger bi,
      FastInteger radixPower) {
      if (bi.isZero()) {
        return bi;
      }
      if (!radixPower.CanFitInInt32()) {
        // NOTE: For radix 10, each digit fits less than 1 byte; the
        // supported byte length is thus less than the maximum value
        // of a 32-bit integer (2GB).
        FastInteger fastBI = FastInteger.FromBig(valueMaxDigits);
        if (this.thisRadix != 10 || radixPower.compareTo(fastBI) > 0) {
          return null;
        }
      }
      return this.helper.MultiplyByRadixPower(bi, radixPower);
    }

    private FastIntegerFixed TryMultiplyByRadixPowerFastInt(
      FastIntegerFixed bi,
      FastInteger radixPower) {
      if (bi.isValueZero()) {
        return bi;
      }
      if (!radixPower.CanFitInInt32()) {
        // NOTE: For radix 10, each digit fits less than 1 byte; the
        // supported byte length is thus less than the maximum value
        // of a 32-bit integer (2GB).
        FastInteger fastBI = FastInteger.FromBig(valueMaxDigits);
        if (this.thisRadix != 10 || radixPower.compareTo(fastBI) > 0) {
          return null;
        }
      }
      return FastIntegerFixed.FromBig(this.helper.MultiplyByRadixPower(
        bi.ToEInteger(),
        radixPower));
    }

    private T ValueOf(int value, EContext ctx) {
      return (ctx == null || !ctx.getHasExponentRange() ||
              ctx.ExponentWithinRange(EInteger.FromInt32(0))) ?
        this.helper.ValueOf(value) :
        this.RoundToPrecision(this.helper.ValueOf(value), ctx);
    }
  }
