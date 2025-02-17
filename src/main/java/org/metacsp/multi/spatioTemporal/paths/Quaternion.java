package org.metacsp.multi.spatioTemporal.paths;

import java.io.Serializable;

/**
 * Class for maintaining and converting from/to Quaternion representations.
 * @author Federico Pecora
 *
 */
public class Quaternion implements Serializable {
	
	private static final long serialVersionUID = -5541257184074383188L;
	private double x, y, z, w;
	
	/**
	 * Create a {@link Quaternion} given its parameters.
	 * @param x X component of this {@link Quaternion}.
	 * @param y Y component of this {@link Quaternion}.
	 * @param z Z component of this {@link Quaternion}.
	 * @param w W component of this {@link Quaternion}.
	 */
	public Quaternion(double x, double y, double z, double w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	/**
	 * Creates a {@link Quaternion} from given angles (roll, pitch and yaw).
	 * @param roll The angle around the X axis
	 * @param pitch The angle around the Y axis
	 * @param yaw The angle around the Z axis
	 */
	public Quaternion(double roll, double pitch, double yaw) {
		double cy = Math.cos(yaw * 0.5);
		double sy = Math.sin(yaw * 0.5);
		double cr = Math.cos(roll * 0.5);
		double sr = Math.sin(roll * 0.5);
		double cp = Math.cos(pitch * 0.5);
		double sp = Math.sin(pitch * 0.5);
		
		this.w = cy * cr * cp + sy * sr * sp;
		this.x = cy * sr * cp - sy * cr * sp;
		this.y = cy * cr * sp + sy * sr * cp;
		this.z = sy * cr * cp - cy * sr * sp;
	}
	
	/**
	 * Creates a {@link Quaternion} from a given angle (assumed to be yaw, for 2D poses).
	 * @param theta The angle to encode as a {@link Quaternion}.
	 */
	public Quaternion(double theta) {
		this.x = 0;
		this.y = 0;
		this.z = Math.sin(theta/2.0);			
		this.w = Math.cos(theta/2.0);	
	}
	
	/**
	 * Get the roll, pitch and yaw angles in radians represented by this {@link Quaternion}.
	 * @return The roll, pitch and yaw angles (in that order) in radians represented by this {@link Quaternion}.
	 */
	public double[] getRollPitchYaw() {
		double[] ret = new double[3];
		
		// roll (x-axis rotation)
		double sinr = +2.0 * (w * x + y * z);
		double cosr = +1.0 - 2.0 * (x * x + y * y);
		ret[0] = Math.atan2(sinr, cosr);

		// pitch (y-axis rotation)
		double sinp = +2.0 * (w * y - z * x);
		// use 90 degrees if out of range
		if (Math.abs(sinp) >= 1) ret[1] = Math.copySign(Math.PI / 2.0, sinp);
		else ret[1] = Math.asin(sinp);

		// yaw (z-axis rotation)
		double siny = +2.0 * (w * z + x * y);
		double cosy = +1.0 - 2.0 * (y * y + z * z);  
		ret[2] = Math.atan2(siny, cosy);
		
		return ret;
	}
	
	/**
	 * Get the angle in radians represented by this {@link Quaternion}.
	 * @return The angle in radians represented by this {@link Quaternion}.
	 */
	public double getTheta() {
		return getRollPitchYaw()[2];
		//return Math.atan2(2.0*(this.x*this.y+this.z*this.w),1.0-2*(this.y*this.y+this.z*this.z));
	}
	
	/**
	 * Get the X component of this {@link Quaternion}.
	 * @return The X component of this {@link Quaternion}.
	 */
	public double getX() {
		return x;
	}

	/**
	 * Get the Y component of this {@link Quaternion}.
	 * @return The Y component of this {@link Quaternion}.
	 */
	public double getY() {
		return y;
	}
	
	/**
	 * Get the Z component of this {@link Quaternion}.
	 * @return The Z component of this {@link Quaternion}.
	 */
	public double getZ() {
		return z;
	}
	
	/**
	 * Get the W component of this {@link Quaternion}.
	 * @return The W component of this {@link Quaternion}.
	 */
	public double getW() {
		return w;
	}
	
	/**
	 * Get the norm of this {@link Quaternion}.
	 * @return The norm of this {@link Quaternion}.
	 */
	public double norm() {
        return Math.sqrt(x*x + y*y + z*z + w*w);
    }
	
	/**
	 * Get the conjugate of this {@link Quaternion}.
	 * @return The conjugate of this {@link Quaternion}.
	 */
	public Quaternion conjugate() {
        return new Quaternion(x, -y, -z, -w);
    }
	
	/**
	 * Add this and another {@link Quaternion}s.
	 * @param other A {@link Quaternion} to add to this {@link Quaternion}.
	 * @return The sum of this and the given {@link Quaternion}.
	 */
	public Quaternion plus(Quaternion other) {
        return new Quaternion(this.x+other.x, this.y+other.y, this.z+other.z, this.w+other.w);
    }
	
	/**
	 * Get a new {@link Quaternion} whose value is (<code>this</code> * <code>other</code>).
	 * @param other A {@link Quaternion} to multiply by.
	 * @return A new {@link Quaternion} whose value is (<code>this</code> * <code>other</code>).
	 */
	public Quaternion times(Quaternion other) {
        double x = this.x*other.x - this.y*other.y - this.z*other.z - this.w*other.w;
        double y = this.x*other.y + this.y*other.x + this.z*other.w - this.w*other.z;
        double z = this.x*other.z - this.y*other.w + this.z*other.x + this.w*other.y;
        double w = this.x*other.w + this.y*other.z - this.z*other.y + this.w*other.x;
        return new Quaternion(x,y,z,w);
    }

	/**
	 * Get this inverse of this {@link Quaternion}.
	 * @return The inverse of this {@link Quaternion}.
	 */
	public Quaternion inverse() {
		double d = x*x + y*y + z*z + w*w;
		return new Quaternion(x/d, -y/d, -z/d, -w/d);
	}
	
	/**
	 * Get a new {@link Quaternion} whose value is <code>this</code> * <code>inverse(other)</code>.
	 * @param other A {@link Quaternion} to divide by.
	 * @return A new {@link Quaternion} whose value is <code>this</code> * <code>inverse(other)</code>.
	 */
	public Quaternion divide(Quaternion other) {
        return this.times(other.inverse());
    }
	
	public String toString() {
        return x + " + " + y + "i + " + z + "j + " + w + "k";
    }

}
