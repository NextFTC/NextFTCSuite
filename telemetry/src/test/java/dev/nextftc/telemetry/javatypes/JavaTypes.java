/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.javatypes;

import dev.nextftc.telemetry.struct.StructIgnore;
import dev.nextftc.telemetry.struct.StructName;
import dev.nextftc.telemetry.struct.StructOrder;
import dev.nextftc.telemetry.struct.StructSize;
import java.util.List;

/** Java classes for reflecting into struct schemas, written the way Java code usually is. */
public final class JavaTypes {
  private JavaTypes() {}

  public static class PublicFields {
    public double x = 1.0;
    public double y = 2.0;
  }

  public static class Getters {
    private final double x;
    private final double y;

    public Getters(double x, double y) {
      this.x = x;
      this.y = y;
    }

    public double getX() {
      return x;
    }

    public double getY() {
      return y;
    }
  }

  /** Declared z first, but fields are alphabetical because Java doesn't keep method order. */
  public static class Unordered {
    public double getZ() {
      return 3.0;
    }

    public double getM() {
      return 2.0;
    }

    public double getA() {
      return 1.0;
    }
  }

  @StructOrder({"z", "a"})
  public static class Ordered {
    public double getA() {
      return 1.0;
    }

    public double getB() {
      return 2.0;
    }

    public double getZ() {
      return 3.0;
    }
  }

  @StructOrder("missing")
  public static class BadOrder {
    public double getA() {
      return 1.0;
    }
  }

  public static class Flags {
    public boolean isActive() {
      return true;
    }

    public Boolean isBoxed() {
      return false;
    }

    public boolean getEnabled() {
      return true;
    }

    /** Not a getter: "is" only counts for booleans. */
    public int isCount() {
      return 7;
    }
  }

  public static class Acronyms {
    public double getRPM() {
      return 1.0;
    }

    public double getName() {
      return 2.0;
    }
  }

  public static class Boxed {
    public Double getValue() {
      return 1.5;
    }

    public Integer getCount() {
      return 3;
    }
  }

  public static class NullBoxed {
    public Double getValue() {
      return null;
    }
  }

  public static class Annotated {
    @StructIgnore public double ignoredField = 9.0;

    public double kept = 1.0;

    @StructIgnore
    public String getLabel() {
      return "not a number";
    }

    @StructSize(2)
    public double[] getPair() {
      return new double[] {4.0, 5.0};
    }
  }

  public static class WrongSize {
    @StructSize(3)
    public double[] getPair() {
      return new double[] {1.0};
    }
  }

  public static class MissingSize {
    public double[] getPair() {
      return new double[] {1.0, 2.0};
    }
  }

  /** The public field wins over the getter of the same name. */
  public static class FieldAndGetter {
    public double x = 1.0;

    public double getX() {
      return 99.0;
    }
  }

  public static class NotGetters {
    public double getX() {
      return 1.0;
    }

    public static double getInstance() {
      return 2.0;
    }

    public double getWithArgument(int argument) {
      return 3.0;
    }

    public void getNothing() {}

    public double get() {
      return 4.0;
    }

    public double getlower() {
      return 5.0;
    }

    private double getPrivate() {
      return 6.0;
    }
  }

  public static class Base {
    public double getX() {
      return 1.0;
    }
  }

  public static class Derived extends Base {
    public double getY() {
      return 2.0;
    }
  }

  @StructName("Translation2d")
  public static class Vec {
    private final double x;
    private final double y;

    public Vec(double x, double y) {
      this.x = x;
      this.y = y;
    }

    public double getX() {
      return x;
    }

    public double getY() {
      return y;
    }
  }

  public static class Pose {
    private final Vec translation;
    private final double heading;

    public Pose(Vec translation, double heading) {
      this.translation = translation;
      this.heading = heading;
    }

    public Vec getTranslation() {
      return translation;
    }

    public double getHeading() {
      return heading;
    }
  }

  public static class HasStringGetter {
    public String getName() {
      return "x";
    }
  }

  public static class HasListGetter {
    public List<Double> getItems() {
      return null;
    }
  }

  public static class HasCharGetter {
    public char getLetter() {
      return 'a';
    }
  }

  public static class HasIntArrayGetter {
    public int[] getValues() {
      return new int[0];
    }
  }

  public static class NoGetters {
    private double hidden = 1.0;
  }

  /** A package-private class with public getters, which needs setAccessible to read. */
  static class Hidden {
    public double getX() {
      return 1.0;
    }
  }

  public static Class<?> hiddenClass() {
    return Hidden.class;
  }

  public static Object hidden() {
    return new Hidden();
  }
}
