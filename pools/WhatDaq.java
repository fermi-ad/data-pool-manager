// $Id: WhatDaq.java,v 1.7 2023/11/02 16:36:15 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

import java.util.EnumSet;
import java.util.Arrays;
import java.text.ParseException;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.NeverEvent;
import gov.fnal.controls.servers.dpm.events.DefaultDataEvent;
import gov.fnal.controls.servers.dpm.events.DataEventFactory;
import gov.fnal.controls.servers.dpm.events.DeltaTimeEvent;

import gov.fnal.controls.servers.dpm.drf3.Range;
import gov.fnal.controls.servers.dpm.drf3.DeviceFormatException;
import gov.fnal.controls.servers.dpm.drf3.TimeFreq;
import gov.fnal.controls.servers.dpm.drf3.TimeFreqUnit;
import gov.fnal.controls.servers.dpm.drf3.PeriodicEvent;
import gov.fnal.controls.servers.dpm.drf3.Event;

import gov.fnal.controls.servers.dpm.drf3.Field;
import gov.fnal.controls.servers.dpm.drf3.Property;
import gov.fnal.controls.servers.dpm.drf3.EventFactory;
import gov.fnal.controls.servers.dpm.drf3.AcnetRequest;
import gov.fnal.controls.servers.dpm.drf3.FieldFormatException;
import gov.fnal.controls.servers.dpm.drf3.PropertyFormatException;

import gov.fnal.controls.servers.dpm.DPMRequest;
//import gov.fnal.controls.servers.dpm.AdditionalDeviceInfo;

import gov.fnal.controls.servers.dpm.pools.acnet.Lookup;
import gov.fnal.controls.servers.dpm.pools.acnet.DeviceNotFoundException;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class WhatDaq implements AcnetErrors, ReceiveData
{
	public enum Option { FLOW_CONTROL }

	final EnumSet<Option> options;

	public final boolean defaultEvent;

	public final long refId;
	public final Lookup.Reply.DeviceInfo dInfo;
	public final Field field;
	public final Lookup.PropertyInfo pInfo;
	public final Property property;
	public final Event event;
	public final String alarmListName;
	public final String daqName;
	public final String loggedName;

	final protected int di;

	/**
	 * @serial property index
	 */
	final protected int pi;

	/**
	 * @serial encoded device and property index
	 */
	final protected int dipi;

	/**
	 * @serial device name
	*/
	protected String name;

	/**
	 * @serial ACNET trunk
	 */
	//protected int trunk;

	/**
	 * @serial ACNET node
	 */
	//protected int node;

	/**
	 * @serial ACNET error code
	 */
	protected int error;

	/**
	 * @serial device length
	 */
	protected int length;

	/**
	 * @serial device offset
	 */
	protected int offset;

	/**
	 * @serial maximum device length
	 */
	protected int maxLength;

	/**
	 * @serial data type, 0 is undefined
	 */
	protected int dataTypeId;

	protected int rawOffset;

	/**
	 * @serial number of array elements
	 */
	protected int numElements;

	/**
	 * @serial array element
	 */
	protected int arrayElement = 0;

	/**
	 * @serial True for writing setting data, false for reading data
	 */
	//protected boolean isSetting = false;

	/**
	 * @serial optional raw setting data stored as bytes
	 */
	protected byte setting[];

	/**
	 * @serial True when new setting data
	 */
	//protected boolean settingReady = false;

	/**
	 * @serial protection code
	 */
	protected long protection;

	/**
	 * @serial frequency time descriptor
	 */
	protected int ftd;

	/**
	 * @serial when to acquire the data or null
	 */
	protected DataEvent when;

	/**
	 * @serial to receive the data
	 */
	protected volatile ReceiveData receiveData;

	/**
	 * @serial id to pass to receive data
	 */
	protected int receiveDataId;

	/**
	 * @serial owner
	 */
	protected PoolUser user;

	/**
	 * @serial scaling information
	 */
	//protected Scaling scaling;

	/**
	 * @serial number of bytes
	 */
	public final static int SSDN_SIZE = 8;

	/**
	 * @serial subsystem device number
	 */
	protected byte[] ssdn = new byte[SSDN_SIZE];

	/**
	 * @serial marked for delete
	 */
	protected boolean delete;

	/**
	 * @serial one, two, or four
	 */
	protected int defaultLength;

	/**
	 * @serial mask of broadcast pools
	 */
	protected int poolMask;

	Node node;

	public static int dipi(int di, int pi)
	{
		return (di & 0xffffff) | (pi << 24);
	}

	public WhatDaq(String drf) throws IllegalArgumentException, AcnetStatusException, DeviceNotFoundException
	{
		this(0, new DPMRequest(drf));
	}

	public WhatDaq(long refId, DPMRequest req, DataEvent event) throws AcnetStatusException, DeviceNotFoundException
	{
		this(refId, req);
		this.when = event;
	}

	public WhatDaq(WhatDaq whatDaq, int length, int offset, int receiveDataId, ReceiveData receiveData)
	{
		this(whatDaq);

		this.length = length;
		this.offset = offset;
		this.receiveDataId = receiveDataId;
		this.receiveData = receiveData;
	}

	public WhatDaq(WhatDaq whatDaq, int id)
	{
		this(whatDaq, whatDaq.length, whatDaq.offset, id, null);
	}

	public WhatDaq(long refId, DPMRequest req) throws AcnetStatusException, DeviceNotFoundException
	{
		final AcnetRequest acnetReq;

		try {
			acnetReq = new AcnetRequest(req);
		} catch (DeviceFormatException e) {
			throw new AcnetStatusException(DPM_BAD_REQUEST, e);
		} catch (PropertyFormatException e) {
			throw new AcnetStatusException(DPM_BAD_REQUEST, e);
		} catch (FieldFormatException e) {
			throw new AcnetStatusException(DPM_BAD_REQUEST, e);
		} catch (Exception e) {
			throw new AcnetStatusException(ACNET_NXE, e);
		}
	
		this.receiveDataId = 0;
		this.options = EnumSet.noneOf(Option.class);
		this.refId = refId;
		this.property = acnetReq.getProperty();
		this.pi = (this.property.indexValue & 0xff);

		this.field = acnetReq.getField();
		this.dInfo = Lookup.getDeviceInfo(acnetReq.getDevice());
		this.pInfo = propInfo();

		this.di = dInfo.di;
		this.dipi = dipi(di, pi);
		this.name = dInfo.name;
		//this.node = this.pInfo.node;
		this.event = acnetReq.getEvent();

		this.node = Node.get(this.pInfo.node);

		this.error = 0;
		this.defaultLength = pInfo.atomicSize;
		this.maxLength = pInfo.size;
		this.protection = AdditionalDeviceInfo.permissions(this.di) & 0xffffffff;
		System.arraycopy(pInfo.ssdn, 0, this.ssdn, 0, pInfo.ssdn.length);
		this.ftd = (pInfo.ftd & 0xffff);
		this.poolMask = 0;
		this.receiveData = null; 
		this.when = EventFactory.createEvent(this.event);
		this.defaultEvent = (this.when instanceof DefaultDataEvent);
		this.alarmListName = AdditionalDeviceInfo.alarmListName(this.di);

		if (this.defaultEvent) {
			try {
				this.when = DataEventFactory.stringToEvent(pInfo.defEvent);
			} catch (ParseException e) {
				throw new AcnetStatusException(DPM_BAD_EVENT);
			}
		}

		// Setup length and offset

		final Range range = acnetReq.getRange();
		
		switch (range.getType()) {
		 	case ARRAY:
		 		offset = range.getStartIndex() * pInfo.atomicSize;
				length = range.getLength() * pInfo.atomicSize;
				break;

			case BYTE:
				offset = range.getOffset();
				length = range.getLength();
				break;

			case FULL:
				offset = 0;
				length = pInfo.size;
				break;

			default:
				throw new AcnetStatusException(DPM_BAD_REQUEST);
		}

		switch (this.property) {
			case ANALOG:
			case DIGITAL:
				if (field != Field.RAW) {
					length = pInfo.size;
					offset = 0;
				}
				break;
		}

		if (length == 0)
			length = pInfo.atomicSize;

		if (pInfo.nonLinear) {
			if (offset >= pInfo.size || length > pInfo.size)
				throw new AcnetStatusException(DPM_OUT_OF_BOUNDS);
		} else {
			if ((offset % pInfo.atomicSize != 0) || (length % pInfo.atomicSize) != 0)
				throw new AcnetStatusException(DPM_BAD_FRAMING);

			if (offset + length > pInfo.size)
				throw new AcnetStatusException(DPM_OUT_OF_BOUNDS);
		}

		this.rawOffset = pInfo.atomicSize;

		// Setup array info

       	if (offset != 0 && defaultLength != 0 && (length % pInfo.atomicSize) == 0)
	    	this.arrayElement = offset / defaultLength;

		switch (this.property) {
			case READING:
		 	case SETTING:
				this.numElements = length / pInfo.atomicSize;
				break;
		}

		final String f = this.pInfo.foreignName;

		if (f != null && !f.isEmpty())
			this.daqName = f + "@" + this.event;
		else
			this.daqName = this.name + "@" + this.event;

		this.loggedName = acnetReq.toLoggedString();
	}

	public WhatDaq(WhatDaq whatDaq)
	{
		this.options = whatDaq.options;
		this.refId = whatDaq.refId;
		this.property = whatDaq.property;
		this.pi = whatDaq.pi;

		this.field = whatDaq.field;
		this.dInfo = whatDaq.dInfo;
		this.pInfo = whatDaq.pInfo;
		this.node = whatDaq.node;

		this.di = whatDaq.di;
		this.dipi = whatDaq.dipi;
		this.name = whatDaq.name;
		this.event = whatDaq.event;

		this.error = 0;
		this.defaultLength = whatDaq.defaultLength;
		this.maxLength = whatDaq.maxLength;
		this.protection = whatDaq.protection;
		this.ssdn = whatDaq.ssdn;
		this.ftd = whatDaq.ftd;
		this.poolMask = 0;
		this.receiveDataId = whatDaq.receiveDataId;
		this.receiveData = this; 
		this.when = whatDaq.when;
		this.defaultEvent = whatDaq.defaultEvent;
		this.alarmListName = whatDaq.alarmListName;
		this.rawOffset = whatDaq.rawOffset;
		this.daqName = whatDaq.daqName;
		this.loggedName = whatDaq.loggedName;
	}

	public final int di()
	{
		return di;
	}

	public final int pi()
	{
		return pi;
	}

	public final int dipi()
	{
		return dipi;
	}

	public final int length()
	{
		return length;
	}

	public final int defaultLength()
	{
		return defaultLength;
	}

	public final boolean lengthIsOdd()
	{
		return (length & 1) == 1;
	}

	public Property property()
	{
		return property;
	}

	public final int offset()
	{
		return offset;
	}

	public final byte[] ssdn()
	{
		return ssdn;
	}

	public byte[] setting()
	{
		return setting;
	}

	//public AcnetNodeInfo nodeInfo()
	//{
	//	return nodeInfo;
	//}

	public void setOption(Option opt)
	{
		options.add(opt);
	}

	public void clearOption(Option opt)
	{
		options.remove(opt);
	}

	public boolean getOption(Option opt)
	{
		return options.contains(opt);
	}

	public boolean isSettableProperty()
	{
		switch (property) {
			case SETTING:
			case CONTROL:
		 		return true;

		 	default:
		 		return false;
		}
	}

	public boolean isStateDevice()
	{
		return name.charAt(0) == 'V';		
	}

	public boolean hasEvent()
	{
		return !(when instanceof NeverEvent);		
	}

	public boolean isRepetitive()
	{
		return when.isRepetitive();
	}

	//public DPMReceiveData getDPMReceiveData()
	//{
	//	return dpmReceiveData;
	//}

	public void setReceiveData(ReceiveData receiveData)
	{
		this.receiveData = (receiveData == null ? this : receiveData);
		//super.setReceiveData(receiveData);

	//	if (receiveData instanceof DPMReceiveData)
	//		this.dpmReceiveData = (DPMReceiveData) receiveData;
	}

	//public void setReceiveData(DPMReceiveData dpmReceiveData)
	//{
	//	this.receiveData = (dpmReceiveData == null ? this : dpmReceiveData);
		//super.setReceiveData((ReceiveData) dpmReceiveData);

	//	this.dpmReceiveData = dpmReceiveData;
	//}

	public void setUser(PoolUser user)
	{
		this.user = user;
	}

    private Lookup.PropertyInfo propInfo() throws AcnetStatusException
    {
        Lookup.PropertyInfo pInfo = null;

        switch (property) {
            case STATUS:
                if (dInfo.status != null)
                    return dInfo.status.prop;
                break;

            case CONTROL:
                if (dInfo.control != null)
                    return dInfo.control.prop;
                break;

            case READING:
                if (dInfo.reading != null)
                    return dInfo.reading.prop;
                break;

            case SETTING:
                if (dInfo.setting != null)
                    return dInfo.setting.prop;
                break;

            case ANALOG:
                if (dInfo.analogAlarm != null)
                    return dInfo.analogAlarm.prop;
                break;

            case DIGITAL:
                if (dInfo.digitalAlarm != null)
                    return dInfo.digitalAlarm.prop;
                break;

			case DESCRIPTION:
				{
					pInfo = new Lookup.PropertyInfo();

					pInfo.pi = (short) this.pi;
					pInfo.node = getNodeFromProperty();
					pInfo.ftd = 0;
					pInfo.ssdn = new byte[0];
					pInfo.size = 128;
					pInfo.defSize = 1;
					pInfo.atomicSize = 1;
					pInfo.defEvent = "i";
					pInfo.nonLinear = false;
					pInfo.foreignName = null;	

					return pInfo;
				}

			case INDEX:
				{
					pInfo = new Lookup.PropertyInfo();

					pInfo.pi = (short) this.pi;
					pInfo.node = getNodeFromProperty();
					pInfo.ftd = 0;
					pInfo.ssdn = new byte[0];
					pInfo.size = 4;
					pInfo.defSize = 4;
					pInfo.atomicSize = 4;
					pInfo.defEvent = "i";
					pInfo.nonLinear = false;
					pInfo.foreignName = null;	

					return pInfo;
				}

			case LONG_NAME:
				{
					pInfo = new Lookup.PropertyInfo();

					pInfo.pi = (short) this.pi;
					pInfo.node = getNodeFromProperty();
					pInfo.ftd = 0;
					pInfo.ssdn = new byte[0];
					pInfo.size = 128;
					pInfo.defSize = 1;
					pInfo.atomicSize = 1;
					pInfo.defEvent = "i";
					pInfo.nonLinear = false;
					pInfo.foreignName = null;	

					return pInfo;
				}

			case ALARM_LIST_NAME:
				{
					pInfo = new Lookup.PropertyInfo();

					pInfo.pi = (short) this.pi;
					pInfo.node = getNodeFromProperty();
					pInfo.ftd = 0;
					pInfo.ssdn = new byte[0];
					pInfo.size = 128;
					pInfo.defSize = 1;
					pInfo.atomicSize = 1;
					pInfo.defEvent = "i";
					pInfo.nonLinear = false;
					pInfo.foreignName = null;	

					return pInfo;
				}
        }

        throw new AcnetStatusException(DBM_NOPROP);
    }

	private short getNodeFromProperty() throws AcnetStatusException
	{
		if (dInfo.reading != null)
			return dInfo.reading.prop.node;

		if (dInfo.setting != null)
			return dInfo.setting.prop.node;

		if (dInfo.status != null)
			return dInfo.status.prop.node;

		if (dInfo.control != null)
			return dInfo.control.prop.node;

		if (dInfo.analogAlarm != null)
			return dInfo.analogAlarm.prop.node;

		if (dInfo.digitalAlarm != null)
			return dInfo.digitalAlarm.prop.node;

        throw new AcnetStatusException(DBM_NOPROP);
	}

	private String getUnits(Field f, Lookup.Reading r)
	{
		if (r.scaling != null) {
			if (f == Field.PRIMARY && r.scaling.primary != null)
				return r.scaling.primary.units;
			else if (f == Field.SCALED && r.scaling.common != null)
				return r.scaling.common.units;
		}

		return null;
	}

	public String getUnits()
	{
		switch (property) {
		 case READING:
		 	return getUnits(field, dInfo.reading);

		 case SETTING:
		 	return getUnits(field, dInfo.setting);
		}

		return null;
	}

/*
    public int raw(final byte[] data, int offset) throws AcnetStatusException
    {
        if (defaultLength == 2)
            return (data[offset++] & 0xff) | 
                    (data[offset] << 8);
        else if (defaultLength == 4)
            return (data[offset++] & 0xff) |
                    ((data[offset++] & 0xff) << 8) |
                    ((data[offset++] & 0xff) << 16) |
                    ((data[offset] & 0xff) << 24);
        else if (defaultLength == 1)
            return data[offset];

        throw new AcnetStatusException(DIO_SCALEFAIL);
    }

    public byte[] raw(int value, int length) throws AcnetStatusException
    {
        if (length <= 0 || length > 4)
            throw new AcnetStatusException(DIO_SCALEFAIL);

        byte[] raw = new byte[length];

        for (int ii = 0; ii < length; ii++, value >>= 8)
            raw[ii] = (byte) (value & 0xff);

        return raw;
    }

    public double scale(final byte[] data, int offset) throws AcnetStatusException
    {
		final Scaling scaling = getScaling();
		//final ReadSetScaling scaling = (ReadSetScaling) getScaling();

		switch (field) {
		 case RAW:
			return raw(data, offset);

		 case PRIMARY:
			return scaling.pdudpu(raw(data, offset));

		 case SCALED:
			return scaling.pdudcu(raw(data, offset));
		}

       	throw new AcnetStatusException(DIO_NOSCALE);
    }
*/

	//public boolean usesAlternateScaling()
	//{
	//	if (scaling instanceof ReadSetScaling)
	//	    return ((ReadSetScaling) scaling).usesAlternateScaling();

	//	return false;
	//}

	void clearSetting()
	{
		setting = null;
		//isSetting = false;
		//settingReady = false;
	}

    final public void setSetting(byte data[]) throws AcnetStatusException
	{
        if (data.length == length) {
			setting = data;
        	//isSetting = true;
         	//settingReady = true;
		} else
			throw new AcnetStatusException(DIO_INVLEN);
    }

/*
	final public void setSettingData(double[] s) throws AcnetStatusException
	{
		Scaling scaling = getScaling();

		if (s.length == 1)
			setSettingData(scaling.unscaleRaw(s[0], getLength()));
		else if (s.length > 1) {
			byte[] raw = new byte[defaultLength * s.length];

            for (int ii = 0, offset = 0; ii < s.length; ii++, offset += defaultLength)
                System.arraycopy(scaling.unscaleRaw(s[ii], defaultLength), 0, raw, offset, defaultLength);

			setSettingData(raw);
		}
	}

	final public void setSettingData(String[] s) throws AcnetStatusException
	{
		Scaling scaling = getScaling();

		if (s.length == 1)
			setSettingData(scaling.unscaleRaw(s[0], getLength()));
		else if (s.length > 1) {
			byte[] raw = new byte[defaultLength * s.length];

            for (int ii = 0, offset = 0; ii < s.length; ii++, offset += defaultLength)
                System.arraycopy(scaling.unscaleRaw(s[ii], defaultLength), 0, raw, offset, defaultLength);

			setSettingData(raw);
		}
	}
*/

	final private int end()
	{
		return offset + length;
	}

	final public double getEventFrequency()
	{
		if (when instanceof DeltaTimeEvent && when.isRepetitive())
			return 1.0 / (((DeltaTimeEvent) when).getRepeatRate()) * 1000.0;

		return 0;
	}

	final public boolean contains(WhatDaq whatDaq)
	{
		return di == whatDaq.di && pi == whatDaq.pi && 
				whatDaq.offset >= offset && whatDaq.end() <= end();
	}

	/**
	 * Compare WhatDaqs for equality.
	 * @param whatDaq
	 * @return true if same DI, PI, length, offset, setting flag
	 */
	public boolean isEqual(WhatDaq whatDaq)
	{
		if (di != whatDaq.di || pi != whatDaq.pi
				//|| isSetting != whatDaq.isSetting
				|| length != whatDaq.length
				|| offset != whatDaq.offset)
			return false;


		//for (int ii = 0; ii < SSDN_SIZE; ii++)
		//	if (this.ssdn[ii] != whatDaq.ssdn[ii])
		//		return false;
		return true;
	}

	@Override
	public void receiveData(int error, int offset, byte[] data, long timeststamp)
	{
		delete = true;
	}

	/**
	 * Support cloning. Useful for SDA to arm one WhatDaq for collection while
	 * waiting for another to finish.
	 * 
	 * @return clone of this WhatDaq
	 * 
	 */
	public Object clone()
	{
		return new WhatDaq(this);
	}

	/**
	 * Inquire if this is a setting.
	 * 
	 * @return true if this is a setting
	 * 
	 */
	//public boolean isSetting()
	//{
	//	return isSetting;
	//}

	/**
	 * Set the state of the setting boolean.
	 * 
	 * @param state
	 *            of the setting boolean
	 * 
	 */
	//public void setIsSetting(boolean state)
	//{
	//	isSetting = state;
	//}

	//public boolean settingData(byte data[]) throws AcnetStatusException
	//public void putSetting(byte data[]) throws AcnetStatusException
	//{
	//	if (data.length != length) {
			//System.out.println("WhatDaq.settingData(): ERROR, data array length of "
			//				+ data.length
			//				+ " does not match expected data size of " + length);
			//return false;
	//		throw new AcnetStatusException(DIO_SCALEFAIL);
	//	}

	//	setting = data;
		//if (setting == null) // Allocate array for setting data if needed
			//setting = new byte[length];
		// for (int i=0; i < length; i++)
		// setting[i] = data[i];
		//System.arraycopy(data, 0, setting, 0, length);
		//isSetting = true;
	//	settingReady = true;

		//return true;
	//}

	/**
	 * Set the setting data.
	 * 
	 * @param data
	 *            the setting data.
	 * @param dataoff
	 *            the offset into the setting data.
	 * @param datalen
	 *            the length of the setting data.
	 * @return false if setting's length is incorrect
	 * @exception AcnetException
	 *                if no scaling information is available
	 */
	 /*
	public boolean settingData(byte data[], int dataoff, int datalen)
			throws AcnetStatusException
	{
		if (datalen > length) {
			System.out
					.println("WhatDaq.settingData(): ERROR, data array length of "
							+ data.length
							+ " greater than expected data size of " + length);
			return (false);
		}
		if (setting == null) // Allocate array for setting data if needed
			setting = new byte[length];
		for (int i = 0; i < datalen; i++)
			setting[i] = data[i + dataoff];
		//isSetting = true;
		settingReady = true;

		return true;
	}
*/	

	/**
	 * Set the setting data.
	 * 
	 * @param scaledData
	 *            the setting data.
	 * @return false if length is incorrect.
	 * @exception AcnetStatusException
	 *                if no scaling information is available
	 */
	////public boolean settingData(double scaled) throws AcnetStatusException
	////{
		//int raw;
		//if (length > 4) // Otherwise, this couldn't be a float.
			//return (false);
		//if (setting == null) // Allocate array for setting data if needed
		//	setting = new byte[length];

		//if (scaling == null)
		//	throw new AcnetStatusException(DIO_NOSCALE, "No scaling data available for WhatDaq setting data.");

		//raw = ((ReadSetScaling) scaling).pdcuud(scaledData);

		////setting = DPMReadSetScaling.get().commonToRaw(scaled);

			//System.out.println("HERE " + raw);
		// raw = scaling.unscale(new Double(scaledData));
		//for (int ii = 0; ii < length; ii++) {
		//	setting[ii] = (byte) (raw & (int) 0xff);
		//	raw >>= 8;
		//}

		////isSetting = true;
		////settingReady = true;

		////return true;
	////}

	/**
	 * Return a description of this device.
	 * 
	 * @return the device description
	 * 
	 */
	public String toString()
	{
		//final AcnetNodeInfo nodeInfo = AcnetNodeInfo.get(trunk, node);
		//final String nodeName = nodeInfo == null ? String.format("(%02x,%02x)", trunk, node) :
		//											nodeInfo.name();

		return String.format("%-14s %-8s %-8d %-16s L:%-6d O:%-6d %02x%02x/%02x%02x/%02x%02x/%02x%02x %s",
								name, node.name(), di, property, length, offset,
								ssdn[1], ssdn[0], ssdn[3], ssdn[2], ssdn[5], ssdn[4], ssdn[7], ssdn[6],
								setting != null ? "SettingReady" : "");
	}

	/**
	 * Determine if this WhatDaq describes an array device.
	 * 
	 * @return <CODE>true</CODE> if this WhatDaq is a reading or setting array
	 */
	public boolean isArray()
	{
		return (pi == Property.READING.indexValue || pi == Property.SETTING.indexValue)
					&& length != defaultLength && numElements > 1 && rawOffset == defaultLength;
	}

	/**
	 * Return the device index.
	 * 
	 * @return the device index
	 */
	public int getDeviceIndex()
	{
		return di;
	}

	/**
	 * Return the property index.
	 * 
	 * @return the property index
	 */
	public int getPropertyIndex()
	{
		return pi;
	}

	/**
	 * Set the property index.
	 * 
	 * @param property
	 *            the property index.
	 */
	//public void setPropertyIndex(int property) {
	//	pi = (byte) property;
	//}

	/**
	 * Return the device name.
	 * 
	 * @return the device name
	 */
	public String getDeviceName()
	{
		return name;
	}

	public final Node node()
	{
		return node;
	}

	/**
	 * Return the ACNET node information.
	 * 
	 * @return the ACNET node information
	 */
	//public AcnetNodeInfo getNodeInfo()
	//{
	//	return AcnetNodeInfo.get((trunk & 0xff), (node & 0xff));
	//}

	/**
	 * Return error code.
	 * 
	 * @return ACNET error code
	 */
	public int getError()
	{
		return error;
	}

	/**
	 * Set the ACNET error code.
	 * 
	 * @param code
	 *            ACNET error code.
	 */
	public void setError(int error)
	{
		this.error = error;
	}

	/**
	 * Return the length.
	 * 
	 * @return the length
	 */
	public int getLength()
	{
		return length;
	}

	/**
	 * Return the offset.
	 * 
	 * @return the offset
	 */
	public int getOffset()
	{
		return offset;
	}

	/**
	 * Return the maximum length.
	 * 
	 * @return the maximum length
	 */
	public int getMaxLength()
	{
		return maxLength;
	}

	/**
	 * Return the device's device type id. Zero for undefined device types. The
	 * rawOffset and the numElements of an undefined are always zero.
	 * 
	 * @return the device's device type id
	 */
	public int getDeviceTypeId()
	{
		return dataTypeId;
	}

	/**
	 * Return the devices's raw offset to the next array element. Whge
	 * 
	 * @return the devices's raw offset to the next array element
	 */
	public int getRawOffset()
	{
		return rawOffset;
	}

	/**
	 * Return the devices total number of addressable elements.
	 * 
	 * @return the devices total number of addressable elements
	 */
	public int getNumberElements()
	{
		return numElements;
	}

	/**
	 * Set the devices total number of addressable elements.
	 * 
	 * @param num
	 *            the devices total number of addressable elements
	 */
	//public void setNumberElements(int num) {
	//	numElements = num;
	//}

	/**
	 * Return the array element.
	 * 
	 * @return the array element
	 */
	public int getArrayElement()
	{
		return arrayElement;
	}

	/**
	 * Return the setting.
	 * 
	 * @return the setting
	 */
	public byte[] getSetting()
	{
		return setting;
	}

	/**
	 * Inquire if this setting is ready.
	 * 
	 * @return true if this setting is ready
	 */
	public boolean isSettingReady()
	{
		//return settingReady;
		return setting != null;
	}

	/**
	 * Return the protection.
	 * 
	 * @return the protection
	 */
	public long getProtection()
	{
		return protection;
	}

	/**
	 * Return the FTD.
	 * 
	 * @return the FTD
	 */
	public int getFTD()
	{
		return ftd;
	}

	public DataEvent event()
	{
		return when;
	}

	/**
	 * Return the data event.
	 * 
	 * @return the data event
	 */
	public DataEvent getEvent()
	{
		return when;
	}

	/**
	 * Set the data collection event.
	 * 
	 * @param event
	 *            the data event.
	 */
	public void setEvent(DataEvent event)
	{
		when = event;
	}

	/**
	 * Return the collector of data.
	 * 
	 * @return the collector of data
	 */
	public ReceiveData getReceiveData()
	{
		return receiveData;
	}

	/**
	 * Return the collection id.
	 * 
	 * @return the collection id
	 */
	public int getReceiveDataId()
	{
		return receiveDataId;
	}

	public int id()
	{
		return receiveDataId;
	}

	public PoolUser getUser()
	{
		return user;
	}

	/**
	 * Return the scaling.
	 * 
	 * @return the scaling
	 */
	//public Scaling getScaling() throws AcnetStatusException
	//{
	//	if (scaling == null)
	//		throw new AcnetStatusException(DIO_NOSCALE);

	//	return scaling;
	//}

	/**
	 * Return the SSDN.
	 * 
	 * @return the SSDN
	 */
	public byte[] getSSDN()
	{
		return ssdn;
	}

	/**
	 * Whack the SSDN (for model and test support).
	 * 
	 * @param index
	 *            the SSDN index
	 * @param value
	 *            the value to push
	 */
	 /*
	public void whackSSDN(int index, byte value) {
		if (index >= 0 && index <= 3)
			ssdn[index] = value;
	}
	*/

	/**
	 */
	public void redirect(Node node)
	{
		this.node = node;
		//this.trunk = trunk;
		//this.node = node;
	}

	/**
	 * Inquire if this is marked for delete.
	 * 
	 * @return true if this is marked for delete
	 */
	public boolean isMarkedForDelete()
	{
		return delete;
	}

	/**
	 * Set the marked for delete boolean.
	 */
	public void setMarkedForDelete()
	{
		receiveData = this;
		delete = true;
	}

	/**
	 * Return the default length.
	 * 
	 * @return the default length
	 */
	public int getDefaultLength()
	{
		return defaultLength;
	}

	/**
	 * Set the length and offset (used by Save/Restore) to match saved data.
	 * 
	 * @param actualLength
	 * @param actualOffset
	 */
	public void setLengthOffset(int length, int offset)
	{
		this.length = length;
		this.offset = offset;

		if (offset != 0 && defaultLength != 0)
			arrayElement = offset / defaultLength;
	}

	/**
	 * Release resources by breaking links.
	 */
	public void releaseResources()
	{
		receiveData = null;
		user = null;
	}
}
