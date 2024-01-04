// $Id: ExternalTrigger.java,v 1.3 2023/11/01 20:56:57 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.List;
import java.util.StringTokenizer;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.events.DataEvent;

/**
 * Describes external triggers for the snapshot functionality of the ACNET fast
 * time plot system.
 * 
 * @author Kevin Cahill
 * @version 0.01<br>
 *          Class created: 20 Jan 1999
 * 
 */
public class ExternalTrigger implements Trigger, Cloneable, AcnetErrors
{
	/**
	 * @serial trigger device
	 */
	private WhatDaq armDevice;

	/**
	 * @serial trigger mask
	 */
	private int armMask;

	/**
	 * @serial external source arm when true
	 */
	private boolean armExternalSource;

	/**
	 * @serial external source number (0-3)
	 */
	private int armSourceModifier;

	/**
	 * @serial delay from arming events in microseconds
	 */
	private int armDelay;

	/**
	 * @serial device offset
	 */
	private int armOffset;

	/**
	 * @serial device value
	 */
	private int armValue;

	/**
	 * Constructs an ExternalTrigger object describing the device to serve as
	 * the arming trigger to a snapshot plot collection of a device.
	 * 
	 * @param device
	 *            the arming external trigger device and property.
	 * @param deviceOffset
	 *            the offset of the arming device.
	 * @param deviceMask
	 *            the mask to apply to the arming device.
	 * @param deviceValue
	 *            the value for the arming device.
	 * @param delay
	 *            the delay in microseconds or sample periods.
	 */
	public ExternalTrigger(WhatDaq device, int deviceOffset, int deviceMask,
			int deviceValue, int delay) {
		this(device, deviceOffset, deviceMask, deviceValue, delay, false, 0);
	}

	/**
	 * Constructs an ExternalTrigger object describing an external source to
	 * serve as the arming trigger to a snapshot plot collection of a device.
	 * 
	 * 
	 * @param armSourceModifier
	 *            external source modifier (0-3).
	 */
	public ExternalTrigger(int armSourceModifier) {
		this(null, 0, 0, 0, 0, true, armSourceModifier);
	}

	/**
	 * Constructs an ExternalTrigger object from a database saved string.
     * The delay in a reconstruction string is measured in milliseconds.
	 * 
	 * @param reconstructionString
	 *            string returned by getReconstruction().
	 * @throws AcnetStatusException
	 *             if the reconstructionString is invalid
	 */
	public ExternalTrigger(String reconstructionString) throws AcnetStatusException {
		this(0);
		StringTokenizer tok = null;
		String token = null;
		try {
			int modifierIndex = reconstructionString.indexOf("x,mod=");
			if (modifierIndex != -1) {
				armSourceModifier = Integer.parseInt(reconstructionString
						.substring(modifierIndex + 1));
				armExternalSource = true;
				return;
			}
			armExternalSource = false;
			tok = new StringTokenizer(reconstructionString.substring(5), ",=",
					false); // skip trig=
			tok.nextToken(); // skip d,
			String triggerDeviceName = tok.nextToken();

			//AcceleratorDevice triggerDevice = new AcceleratorDevice(
			//		triggerDeviceName);
			//AcceleratorDevicesItem adi = new AcceleratorDevicesItem();
			//adi.addDevice(triggerDevice);
			//armDevice = (WhatDaq) adi.whatDaqs(0, null, null).get(0);

			armDevice = new WhatDaq(triggerDeviceName);


			armOffset = armDevice.getOffset();

			token = tok.nextToken(); // look for ,mask=
			if (token.startsWith("mask")) {
				String armMaskString = tok.nextToken();
				armMask = Integer.parseInt(armMaskString, 16);
				token = tok.nextToken(); // look for ,val=
			}
			if (token.startsWith("val")) {
				String armValueString = tok.nextToken();
				armValue = Integer.parseInt(armValueString, 16);
				token = tok.nextToken(); // look for ,dly=
			}
			if (token.startsWith("dly")) {
				String armDelayString = tok.nextToken();
				armDelay = Integer.parseInt(armDelayString) * 1000;
			}
			return;
		} catch (Exception e) {
			System.out.println("ExternalTrigger.reconstructionString, "
					+ reconstructionString + "\r\n" + token);
			e.printStackTrace();
			throw new AcnetStatusException(DPM_BAD_EVENT, "ExternalTrigger.reconstructionString, "
					+ reconstructionString, e);
		}
	}

	/**
	 * Constructs an ExternalTrigger object describing the device or external
	 * source to serve as the arming trigger to a snapshot plot collection of a
	 * device.
	 * 
	 * @param device
	 *            the arming external trigger device and property.
	 * @param deviceOffset
	 *            the offset of the arming device.
	 * @param deviceMask
	 *            the mask to apply to the arming device.
	 * @param deviceValue
	 *            the value for the arming device.
	 * @param delay
	 *            the delay in microseconds or sample periods.
	 * @param armExternalSource
	 *            the snapshot class.
	 * @param armSourceModifier
	 *            error when retrieving class codes.
	 */
	private ExternalTrigger(WhatDaq device, int deviceOffset, int deviceMask,
			int deviceValue, int delay, boolean armExternalSource,
			int armSourceModifier) {
		armDevice = device;
		armOffset = deviceOffset;
		armMask = deviceMask;
		armValue = deviceValue;
		armDelay = delay;
		this.armExternalSource = armExternalSource;
		this.armSourceModifier = armSourceModifier & 0x03;
	}

	/**
	 * Compare an ExternalTrigger for equality.
	 * 
	 * @return true when the external trigger represents the same arming
	 *         conditions
	 */
	public boolean equals(Object arg) {
		if ((arg != null) && (arg instanceof ExternalTrigger)) {
			ExternalTrigger compare = (ExternalTrigger) arg;
			if (compare.armExternalSource || armExternalSource) // no devices,
																// just sources
			{
				if (compare.armExternalSource != armExternalSource)
					return false;
				if (compare.armSourceModifier != armSourceModifier)
					return false;
				return true;
			}
			if (compare.armDevice.getDeviceIndex() != armDevice
					.getDeviceIndex()
					|| compare.armDevice.getPropertyIndex() != armDevice
							.getPropertyIndex())
				return false;
			if (compare.armMask != armMask
					|| compare.armExternalSource != armExternalSource
					|| compare.armSourceModifier != armSourceModifier
					|| compare.armDelay != armDelay
					|| compare.armOffset != armOffset
					|| compare.armValue != armValue)
				return false;
			return true;
		}
		return false;
	}

	/**
	 * Return the trigger mask.
	 * 
	 * @return trigger mask
	 */
	public int getArmMask() {
		return armMask;
	}

	/**
	 * Return if it is an external source arm.
	 * 
	 * @return true if it is an external source arm
	 */
	public boolean isArmExternalSource() {
		return armExternalSource;
	}

	/**
	 * Inquire if arm immediately.
	 * 
	 * @return true if arm immediately
	 * 
	 */
	public boolean isArmImmediately() {
		return false;
	}

	protected WhatDaq getArmDevice() { return armDevice; }

	/**
	 * Return external source number (1-3).
	 * 
	 * @return external source number (1-3)
	 */
	public int getArmSourceModifier() {
		return armSourceModifier;
	}

	/**
	 * Return delay from arming events.
	 * 
	 * @return delay from arming events in microseconds
	 */
	public int getArmDelay() {
		return armDelay;
	}

	/**
	 * Return device offset.
	 * 
	 * @return device offset
	 */
	public int getArmOffset() {
		return armOffset;
	}

	/**
	 * Return device value.
	 * 
	 * @return device value
	 */
	public int getArmValue() {
		return armValue;
	}

	/**
	 * Return a clone of an ExternalTrigger.
	 * 
	 * @return a clone of an ExternalTrigger
	 * 
	 */
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			System.out.println("Cannot clone ExternalTrigger" + e);
		}
		;
		return null;
	}

	/**
	 * Return the arming events.
	 * 
	 * @return the arming events
	 * 
	 */
	public List<DataEvent> getArmingEvents() {
		return null;
	}

	/**
	 * Return a string representing this ExternalTrigger.
	 * 
	 * @return a string representing this ExternalTrigger
	 * 
	 */
	public String toString() {
		StringBuffer returnString = new StringBuffer();
		if (armExternalSource)
			returnString.append("x,mod=" + armSourceModifier);
		else {
		/*
			String triggerName = DataLoggerDisposition.getLoggedName(armDevice
					.getDeviceName(), armDevice.getPropertyIndex(), armDevice
					.getArrayElement());
			returnString.append("d," + triggerName + ",mask="
					+ Integer.toHexString(getArmMask()) + ",val="
					+ Integer.toHexString(getArmValue()) + ",dly="
					+ getArmDelay()/1000);
		*/
		}
		return returnString.toString();
	}

	/**
	 * Return a String useful for reconstructing an ExternalTrigger.
	 * 
	 * @return String for reconstructing this ExternalTrigger
	 */
	public String getReconstructionString() {
		return "trig=" + toString();
	}
} // end ExternalTrigger class
