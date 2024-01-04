// $Id: ClassCode.java,v 1.4 2023/11/01 21:24:25 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

public class ClassCode
{
	/**
	 * engine class code
	 */
	public final static int DAE_FTP_CLASS_CODE = 23;

	/**
	 * engine class code
	 */
	public final static int DAE_SNAP_CLASS_CODE = 23;

	/**
	 * minimum class code for clipping points
	 */
	public final static int SNAP_MINIMUM_CLIP_POINTS = 11; // until CAMAC is
															// fixed (should be
															// 11)

	// I added these class codes so I can use them other places. They should also
	// be used in this code,
	// but I didn't write it, so there you are.

	/**
	 * Continuous plot 720 Hz
	 */
	public final static int FTP_720 = 11; 

	/**
	 * Continuous plot 1000 Hz
	 */
	public final static int FTP_1000 = 12; 

	/**
	 * 15 hz internal data class code
	 */
	public final static int FTP_15 = 15;  

	/**
	 * 1440 hz internal data class code
	 */
	public final static int FTP_1440_290 = 16;  

	/**
	 * 60 hz internal data class code
	 */
	public final static int FTP_60 = 18; 

	/**
	 * Continuous plot 1440 Hz
	 */
	public final static int FTP_1440 = 19;  

	/**
	 * continuous plot at 240Hz
	 */
	public final static int FTP_240 = 20;  

	/**
	 * DAE at one hertz
	 */
	public final static int FTP_DAE_1 = 22; 
    
    // These are the older protocol versions.
	/**
	 * Continuous plot 720 Hz
	 */
	public final static int FTP_720_OLD = 1;

	/**
	 * Continuous plot 1000 Hz
	 */
	public final static int FTP_1000_OLD = 2; 

	/**
	 * 15 hz internal data class code
	 */
	public final static int FTP_15_OLD = 5; 

	/**
	 * 1440 hz internal data class code
	 */
	public final static int FTP_1440_290_OLD = 6;

	/**
	 * 60 hz internal data class code
	 */
	public final static int FTP_60_OLD = 8; 

	/**
	 * Continuous plot 1440 Hz
	 */
	public final static int FTP_1440_OLD = 9; 

	/**
	 * Continuous plot at 240Hz
	 */
	public final static int FTP_240_OLD = 10;

	/**
	 * @serial snapshot class code
	 */
	private final int snap;

	/**
	 * @serial fast time plot class code
	 */
	private final int ftp;

	/**
	 * @serial description
	 */
	private final String snapDescription;

	/**
	 * @serial description
	 */
	private final String ftpDescription;

	/**
	 * @serial supports type code 7
	 */
	//private final boolean supportsTC7;

	/**
	 * @serial supports type code 6
	 */
	//private final boolean supportsTC6;

	/**
	 * @serial get fast time plot from pool
	 */
	private final boolean ftpFromPool;

	/**
	 * @serial get snapshot from pool
	 */
	private final boolean snapFromPool;

	/**
	 * @serial error acquiring class code
	 */
	private final int error;

	/**
	 * @serial maximum number of points (includes skipped)
	 */
	private final int maxPoints;

	/**
	 * @serial maximum rate in hertz
	 */
	private final int maxSnapRate;

	/**
	 * @serial maximum continuous rate in hertz
	 */
	private final int maxContinuousRate;

	/**
	 * @serial supports snapshots
	 */
	private final boolean supportsSnapshots;

	/**
	 * @serial has time stamps
	 */
	private final boolean hasTimeStamps;

	/**
	 * @serial supports trigger devices
	 */
	private final boolean supportsTriggers;

	/**
	 * @serial skip first point
	 */
	private final boolean skipFirstPoint;

	/**
	 * @serial is suitable trigger device
	 */
	private final boolean isTriggerDevice;

	/**
	 * @serial number to retrieve
	 */
	private final int retrievalMax;

	private final boolean isFixedNumberPoints;

	/**
	 * Constructs a ClassCode object describing the FTP and snap capabilities of
	 * a device.  See the BigSave protocol coverage file for examples of devices
	 * that support various codes.
	 * 
	 * @param ftp
	 *            the fast time plot class.
	 * @param snap
	 *            the snapshot class.
	 * @param err
	 *            error when retrieving class codes.
	 */
	public ClassCode(int ftp, int snap, int error)
	{
		this.ftp = ftp;
		this.snap = snap;
		this.error = error;
		//snapFromPool = false;

		boolean ftpFromPool = false;

		switch (ftp) {
		case 1: // di = 2
		case 11: // di =
			this.ftpDescription = "C190 MADC channel";
			this.maxContinuousRate = 720;
			//this.ftpFromPool = false;
			//if (ftp == 1)
			//	supportsTC6 = false;
			//else
			//	supportsTC6 = true;
			break;

		case 2: // 94042
		case 12: // di =
			this.ftpDescription = "InternetRackMonitor";
			this.maxContinuousRate = 1000;
			//this.ftpFromPool = false;
			//if (ftp == 2)
			//	supportsTC6 = false;
			//else
			//	supportsTC6 = true;
			break;

		case 3: // di =
		case 13: // di =
			this.ftpDescription = "MRRF MAC MADC channel";
			this.maxContinuousRate = 100;
			//this.ftpFromPool = false;
			//if (ftp == 3)
			//	supportsTC6 = false;
			//else
			//	supportsTC6 = true;
			break;

		case 4: // di =
		case 14: // di =
			this.ftpDescription = "Booster MAC MADC channel";
			this.maxContinuousRate = 15;
			//if (ftp == 4)
			//	supportsTC6 = false;
			//else
			//	supportsTC6 = true;
			ftpFromPool = true;
			break;

		case 5: // di = 3679
		case 15: // di =
			this.ftpDescription = "15 Hz (Linac, D/A's etc.)";
			this.maxContinuousRate = 15;
			//this.ftpFromPool = false;
			//if (ftp == 5)
			//	supportsTC6 = false;
			//else
			//	supportsTC6 = true;
			break;

		case 6: // di = 104251
			this.ftpDescription = "1 Hz from data pool (FRIG)";
			this.maxContinuousRate = 1;
			//supportsTC6 = false;
			ftpFromPool = true;
			break;

		case 16: // di =
			this.ftpDescription = "C290 MADC channel";
			this.maxContinuousRate = 1440;
			//this.ftpFromPool = false;
			//supportsTC6 = true;
			break;

		case 7: // di = 10
		case 17: // di =
			this.ftpDescription = "15 Hz from data pool";
			this.maxContinuousRate = 15;
			//if (ftp == 7)
			//	supportsTC6 = false;
			//else
			//	supportsTC6 = true;
			ftpFromPool = true;
			break;

		case 8: // di =
		case 18: // di =
			ftpDescription = "60 Hz internal";
			maxContinuousRate = 60;
			//ftpFromPool = false;
			//if (ftp == 8)
			//	supportsTC6 = false;
			//else
			//	supportsTC6 = true;
			break;

		case 9: // di =
		case 19: // di =
			ftpDescription = "68K (MECAR)";
			maxContinuousRate = 1440;
			//ftpFromPool = false;
			//if (ftp == 9)
			//	supportsTC6 = false;
			//else
			//	supportsTC6 = true;
			break;

		case 10: // di =
		case 20: // di =
			ftpDescription = "Tev Collimators";
			maxContinuousRate = 240;
			//ftpFromPool = false;
			//if (ftp == 10)
			//	supportsTC6 = false;
			//else
			//	supportsTC6 = true;
			break;

		case 21: // di =
			ftpDescription = "IRM 1KHz Digitizer";
			maxContinuousRate = 1000;
			//ftpFromPool = false;
			////supportsTC7 = true;
			break;

		case 22:
			ftpDescription = "DAE 1 Hz";
			maxContinuousRate = 1;
			ftpFromPool = true;
			//supportsTC6 = true;
			break;

		case 23:
			ftpDescription = "DAE 15 Hz";
			maxContinuousRate = 15;
			//supportsTC6 = true;
			ftpFromPool = true;
			break;

		default:
			ftpDescription = "Unknown ftp class: " + ftp;
			maxContinuousRate = 15;
			//supportsTC6 = false;
			ftpFromPool = true;
			break;
		}

		this.ftpFromPool = ftpFromPool;
		
		boolean snapFromPool = false;
		boolean isTriggerDevice = false;
		boolean isFixedNumberPoints = false;

		switch (snap) {
		case 1: // di =
		case 11: // di = 2
			snapDescription = "C190 MADC channel";
			maxSnapRate = 66666;
			if (snap == 1) {
				//supportsTC7 = false;
				isFixedNumberPoints = true;
			} //else
				//supportsTC7 = true;
			maxPoints = 2048;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 2: // di =
		case 12: // di =
			snapDescription = "1440 Hz internal";
			maxSnapRate = 1440;
			if (snap == 2) {
				//supportsTC7 = false;
				isFixedNumberPoints = true;
			} //else
			//	;
				//supportsTC7 = true;
			maxPoints = 2048;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 13: // di =
			snapDescription = "C290 MADC channel";
			maxSnapRate = 90000;
			//supportsTC7 = true;
			maxPoints = 2048;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 4: // di =
		case 14: // di =
			snapDescription = "15 Hz internal";
			maxSnapRate = 15;
			if (snap == 4) {
				//supportsTC7 = false;
				isFixedNumberPoints = true;
			} //else
			//	;
				//supportsTC7 = true;
			maxPoints = 2048;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 5: // di =
		case 15: // di =
			snapDescription = "60 Hz internal";
			maxSnapRate = 60;
			if (snap == 5) {
				//supportsTC7 = false;
				isFixedNumberPoints = true;
			} //else
			//	;
				//supportsTC7 = true;
			maxPoints = 2048;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 6: // di =
		case 16: // di =
			snapDescription = "Quick Digitizer (Linac)";
			maxSnapRate = 10000000;
			if (snap == 6) {
				//supportsTC7 = false;
				isFixedNumberPoints = true;
			}// else
			//	;
				//supportsTC7 = true;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 7: // di =
		case 17: // di =
			snapDescription = "720 Hz internal";
			maxSnapRate = 720;
			if (snap == 7) {
				//supportsTC7 = false;
				isFixedNumberPoints = true;
			}// else
			//	;
				//supportsTC7 = true;
			maxPoints = 2048;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 8: // di = 92440
			snapDescription = "New FRIG Trigger device";
			maxSnapRate = 0;
			//supportsTC7 = false;
			isFixedNumberPoints = true;
			maxPoints = 0;
			supportsSnapshots = false;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = false;
			retrievalMax = 0;
			isTriggerDevice = true;
			break;

		case 9: // di = 2907
		case 18: // di =
			snapDescription = "New FRIG circ buffer";
			maxSnapRate = 1000;
			if (snap == 9)
				isFixedNumberPoints = true;
			//supportsTC7 = true;
			maxPoints = 16384;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = true;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 19: // di = 116587
			snapDescription = "Swift Digitizer";
			maxSnapRate = 800000;
			//supportsTC7 = true;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 20: // di =
			snapDescription = "IRM 20 MHz Quick Digitizer";
			maxSnapRate = 20000000;
			//supportsTC7 = true;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 21: // di =
			snapDescription = "IRM 1KHz Digitizer";
			maxSnapRate = 1000;
			//supportsTC7 = true;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 22: // di = 116587
			snapDescription = "DAE 1 Hz";
			maxSnapRate = 1;
			//supportsTC7 = true;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = true;
			skipFirstPoint = true;
			retrievalMax = 4096;
			snapFromPool = true;
			break;

		case 23: // di = 116587
			snapDescription = "DAE 15 Hz";
			maxSnapRate = 15;
			//supportsTC7 = true;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = true;
			supportsTriggers = true;
			skipFirstPoint = true;
			retrievalMax = 4096;
			snapFromPool = true;
			break;

		case 24: // di =
			snapDescription = "IRM 12.5KHz Digitizer";
			maxSnapRate = 12500;
			//supportsTC7 = true;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 25: // di =
			snapDescription = "IRM 10KHz Digitizer";
			maxSnapRate = 10000;
			//supportsTC7 = true;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 26: // di =
			snapDescription = "IRM 10MHz Digitizer";
			maxSnapRate = 10000000;
			//supportsTC7 = true;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 28: // di =
			snapDescription = "New Booster BLM";
			maxSnapRate = 12500;
			//supportsTC7 = true;
			maxPoints = 4096;
			supportsSnapshots = true;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = true;
			retrievalMax = 512;
			break;

		case 0:
		default:
			snapDescription = "Not Snapshotable";
			maxSnapRate = 0;
			//supportsTC7 = false;
			maxPoints = 0;
			supportsSnapshots = false;
			hasTimeStamps = false;
			supportsTriggers = false;
			skipFirstPoint = false;
			retrievalMax = 0;
			//isTriggerDevice = false;
			break;
		}

		this.snapFromPool = snapFromPool;
		this.isTriggerDevice = isTriggerDevice;
		this.isFixedNumberPoints = isFixedNumberPoints;
	}

	/**
	 * Return a clone of a ClassCode object.
	 * 
	 * @return a clone of a ClassCode object
	 * 
	 */
	//public Object clone()
	//{
		//try {
			//return super.clone();
		//} catch (CloneNotSupportedException e) {
		//	System.out.println("Cannot clone ClassCode" + e);
		//}
		
		//return null;
	//}

	/**
	 * Gets the snapshot class code.
	 * 
	 * @return the snapshot class code
	 */
	public int snap()
	{
		return snap;
	}

	/**
	 * Gets the FTP class code.
	 * 
	 * @return the FTP class code
	 */
	public int ftp()
	{
		return ftp;
	}

	/**
	 * Return the FTP description.
	 * 
	 * @return description
	 */
	public String ftpDescription()
	{
		return ftpDescription;
	}

	/**
	 * Return the snapshot description.
	 * 
	 * @return description
	 */
	public String snapDescription()
	{
		return snapDescription;
	}

	/**
	 * Inquire if snapshot return a fixed number of points.
	 * 
	 * @return true if snapshots return a fixed number of points
	 */
	//public boolean isFixedNumberPoints()
	//{
	//	return isFixedNumberPoints;
	//}

	/**
	 * Inquire if 1996 fast time plot protocol is supported.
	 * 
	 * @param isEngine
	 *            true if an engine.
	 * @return true if the protocol is supported
	 */
	//public boolean supports96FTPProtocol(boolean isEngine) {
	//	if ((ftpClassCode == 22 || ftpClassCode == 23) && !isEngine)
	//		return false;
	//	else
	//		return supportsTC6;
	//}

	/**
	 * Inquire if 1996 snapshot protocol is supported.
	 * 
	 * @param isEngine
	 *            true if an engine.
	 * @return true if the protocol is supported
	 */
	//public boolean supports96SnapshotProtocol(boolean isEngine) {
	//	if ((snapShotClassCode == 22 || snapShotClassCode == 23) && !isEngine)
	//		return false;
	//	return supportsTC7;
	//}

	/**
	 * Determines if snapshot raw buffer returns from the front ends contain an
	 * initial time stamp representing the time at which the arming event occurred
	 * and a data value representing an offset in bytes within the snapshot
	 * buffer to find the first data point after the arming event. This is of
	 * interest to the retrieval code only.
	 * 
	 * @return true when the first time stamp and data point should be skipped
	 */
	public boolean skipFirstPoint()
	{
		return skipFirstPoint;
	}

	/**
	 * Return the maximum number of points supported by this snapshot plot
	 * class.
	 * 
	 * @return the maximum number of points
	 */
	public int maxPoints()
	{
		return maxPoints;
	}

	/**
	 * Return whether this class returns time stamps.
	 * 
	 * @return whether time stamps are returned
	 */
	public boolean hasTimeStamps()
	{
		return hasTimeStamps;
	}

	/**
	 * Decide is plot collection is from the pool.
	 * 
	 * @return true if collection from the pool
	 */
	public boolean ftpFromPool()
	{
		return ftpFromPool;
	}
	
	/**
	 * Decide if snapshots are supported.
	 * 
	 * @return true if snapshots are supported
	 */
	public boolean supportsSnapshots()
	{
		return supportsSnapshots;
	}
	
	/**
	 * Decide if triggers are supported.
	 * 
	 * @return true if triggers are supported
	 */
	public boolean supportsTriggers()
	{
		return supportsTriggers;
	}
	
	/**
	 * Decide if this is a trigger device.
	 * 
	 * @return true if this is a trigger device
	 */
	public boolean isTriggerDevice()
	{
		return isTriggerDevice;
	}
	
	/**
	 * Return the maximum number of points to be retrieved.
	 * 
	 * @return the maximum number of points to be retrieved
	 */
	public int retrievalMax()
	{
		return retrievalMax;
	}

	/**
	 * Decide is plot collection is from the pool.
	 * 
	 * @return true if collection from the pool
	 */
	public boolean snapFromPool()
	{
		return snapFromPool;
	}

	/**
	 * Return the maximum continuous rate.
	 * 
	 * @return the maximum continuous rate
	 */
	public int maxContinuousRate()
	{
		return maxContinuousRate;
	}

	/**
	 * Return the maximum snapshot rate.
	 * 
	 * @return the maximum snapshot rate
	 */
	public int maxSnapRate()
	{
		return maxSnapRate;
	}

	/**
	 * Inquire if this snapshot supports a clipped number of points.
	 * 
	 * @return true if clipped points supported
	 */
	public boolean supportsClippedPoints()
	{
		return snap >= SNAP_MINIMUM_CLIP_POINTS;
	}

	/**
	 * Return the error code associated with ClassCode collection.
	 * 
	 * @return an ACNET error code
	 */
	public int error()
	{
		return error;
	}

	/**
	 * Return a potentially substituted class code given the snapshot collection
	 * rate. If the rate is slow enough, the engine will be used to build the
	 * snapshot.
	 * 
	 * @param rate
	 *            the collection rate in hertz
	 * @return this or an engine class code
	 */
	//public ClassCode sdaSubstituedSnapShotClassCode(int rate) {
		//if (rate >= 15 && maxSnapRate > 15)
		//	return this;
		//return new ClassCode(getFTP(), DAE_SNAP_CLASS_CODE, getError());
	//}

	/**
	 * Return a string representing this ClassCode.
	 */
	@Override
	public String toString()
	{
		return "ClassCode: ftp=" + ftp + " snap=" + snap + " error=" + error;
	}

	/**
	 * Inquire if the front-end implements the following code.
	 * 
	 * @param isEngine
	 *            front-end is engine when true
	 * @param code
	 *            fast time plot or snapshot code
	 * @param isSnap
	 *            snapshot code when true, else FTP
	 * @return true if a front-end implementation
	 */
	 /*
	public static boolean isFrontEndImplementation(boolean isEngine, int code,
			boolean isSnap) {
		if (!isSnap) {
			switch (code) {
			case 1:
			case 11:
			case 2:
			case 12:
			case 3:
			case 13:
			case 4:
			case 14:
			case 5:
			case 15:
			case 16:
				return true;
			case 6:
			case 7:
			case 17:
				return false;
			case 8:
			case 18:
			case 9:
			case 19:
			case 10:
			case 20:
			case 21:
				return true;
			case 22:
			case 23:
				if (isEngine)
					return true;
				else
					return false;
			default:
				return false;
			}
		} else {
			switch (code) {
			case 1:
			case 11:
			case 2:
			case 12:
			case 13:
			case 4:
			case 14:
			case 5:
			case 15:
			case 6:
			case 16:
			case 7:
			case 17:
			case 8:
			case 9:
			case 18:
			case 19:
			case 20:
			case 21:
				return true;
			case 22:
			case 23:
				if (isEngine)
					return true;
				else
					return false;
			case 0:
			default:
				return false;
			}
		}
	}
	*/
}

