package gov.fnal.controls.servers.dpm.scaling;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.service.proto.Dbnews;

import gov.fnal.controls.servers.dpm.pools.acnet.DBNews;
import gov.fnal.controls.servers.dpm.drf3.Property;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.events.DeviceDatabaseListObserver;

import static gov.fnal.controls.db.DbServer.getDbServer;


class EnumeratedStrings implements AcnetErrors, DeviceDatabaseListObserver
{
	private static final HashMap<Integer, EnumeratedString> dipiMap = new HashMap<>();

	static {
		DBNews.addObserver(new EnumeratedStrings());
	}

	private EnumeratedStrings()
	{
	}

	synchronized private static EnumeratedString getEnumeratedString(int di, int pi) throws AcnetStatusException
	{		
		final int dipi = WhatDaq.dipi(di, pi);

		EnumeratedString es = dipiMap.get(dipi);

		if (es == null) {
			es = new EnumeratedString(di, pi);
			dipiMap.put(dipi, es);
		}

		return es;
	}
	
	@Override
	public void deviceDatabaseListChange(Dbnews.Request.Info[] dbnews)
	{
		update(dbnews);
	}
	
	private synchronized void update(Dbnews.Request.Info[] dbnews)
	{
		for (Dbnews.Request.Info info : dbnews) {
			dipiMap.remove(WhatDaq.dipi(info.di, Property.READING.indexValue));
			dipiMap.remove(WhatDaq.dipi(info.di, Property.SETTING.indexValue));
		}
	}

	static int getEnumeratedSetting(int di, int pi, String string) throws AcnetStatusException
	{
		final Integer value = getEnumeratedString(di, pi).stringMap.get(string);

		if (value == null)
			throw new AcnetStatusException(DIO_NOSCALE);

		return value.intValue();
	}

	static String getEnumeratedString(int di, int pi, int value) throws AcnetStatusException
	{
		final String string = getEnumeratedString(di, pi).valueMap.get(value);

		if (string == null)
			throw new AcnetStatusException(DIO_NOSCALE);

		return string;
	}

	static class EnumeratedString
	{
		final HashMap<Integer, String> valueMap = new HashMap<>();
		final HashMap<String, Integer> stringMap = new HashMap<>();

		EnumeratedString(int di, int pi)
		{
			ResultSet rs = null;

			try {
				final String query = "SELECT long_name,value" +
										" FROM accdb.read_set_enum_sets s,accdb.read_set_enum_values v" +
										" WHERE s.enum_set_id=v.enum_set_id and di=" + di + " and pi=" + pi;


				rs = getDbServer("adbs").executeQuery(query);

				while (rs.next()) {
					final String name = rs.getString(1);
					final int value = rs.getInt(2);

					valueMap.put(value, name);
					stringMap.put(name, value);
				}
			} catch (SQLException e) {
			} finally {
				try {
					rs.close();
				} catch (Exception ignore) { }
			}
		}
	}
}
