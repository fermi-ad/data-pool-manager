// $Id: AdditionalDeviceInfo.java,v 1.3 2023/12/13 21:08:42 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.sql.ResultSet;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;

import gov.fnal.controls.servers.dpm.events.DeviceDatabaseListObserver;
import gov.fnal.controls.service.proto.Dbnews;

import gov.fnal.controls.db.DbServer;
import static gov.fnal.controls.db.DbServer.getDbServer;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

import gov.fnal.controls.servers.dpm.pools.acnet.DBNews;

class AdditionalDeviceInfo implements DeviceDatabaseListObserver, AcnetErrors
{
	private static class Holder {
		long permissions;
		String alarmListName;
	}

	private final static Map<Integer, String> alarmListNames = new HashMap<>();
	private final static Map<Integer, Holder> byDi = new HashMap<>(8 * 1024);

	static void init()
	{
		new AdditionalDeviceInfo();
	}

	private AdditionalDeviceInfo()
	{
		update(null);
		DBNews.addObserver(this);
	}

	private synchronized void update(final Set<Integer> diSet)
	{
		try {

			// Get the alarm list name mapping

			{
				final String query = "SELECT list_number,name FROM hendricks.alarm_list_info";
				final ResultSet rs = getDbServer("adbs").executeQuery(query);

				while (rs.next()) {
					alarmListNames.put(rs.getInt("list_number"), rs.getString("name"));
				}

				rs.close();
			}

			// Get di to additional info mapping

			{
				final String where;

				if (diSet != null && !diSet.isEmpty()) {
					String separator = "";
					StringBuilder buf = new StringBuilder(" where di in (");

					for (int di : diSet) {
						buf.append(separator);
						buf.append(di);
						separator = ",";
					}

					buf.append(")");
					where = buf.toString();
				} else
					where = "";

				final String query = "SELECT di, COALESCE(protection_mask,0) mask, alarm_list_id FROM accdb.device" + where;
				final ResultSet rs = getDbServer("adbs").executeQuery(query);

				while (rs.next()) {
					final Holder h = new Holder();

					h.permissions = rs.getLong("mask");
					h.alarmListName = alarmListNames.get(rs.getInt("alarm_list_id"));
					if (h.alarmListName == null)
						h.alarmListName = "";
					
					byDi.put(rs.getInt("di"), h);
				}

				rs.close();
			}

			logger.log(Level.FINE, "additional device info: " + byDi.size() + " entries");
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception updating additional database info", e);
		}
	}

	@Override
	synchronized public void deviceDatabaseListChange(Dbnews.Request.Info[] dbnews)
	{
		Set<Integer> diSet = new HashSet<>();

		for (Dbnews.Request.Info dbEdit : dbnews) {
			byDi.remove(dbEdit.di);
			diSet.add(dbEdit.di);
		}

		update(diSet);
	}

	public synchronized static String alarmListName(int di)
	{
		final Holder h = byDi.get(di);

		return (h != null) ? h.alarmListName : "";
	}

	public synchronized static long permissions(int di)
	{
		final Holder h = byDi.get(di);

		return (h != null) ? h.permissions : 0;
	}

	public static void main(String[] args) throws Exception
	{
		String query;
		long start;
		ResultSet rs;
		//AdditionalDeviceInfo info = new AdditionalDeviceInfo();

		final DbServer server = getDbServer("adbs");
		//for (Map.Entry<Integer, Holder> entry : byDi.entrySet())
		//	System.out.printf("%10d 0x%08x %s\n", entry.getKey(), entry.getValue().permissions,
		//											entry.getValue().alarmListName)
		start = System.currentTimeMillis();

        query = "SELECT di, pi, value, short_name, long_name " +
       	 "FROM accdb.read_set_enum_sets S " +
       	 "JOIN accdb.read_set_enum_values V " +
         "ON S.enum_set_id = V.enum_set_id";

		rs = server.executeQuery(query);
		while(rs.next()) {
		}
		System.out.println("enum string " + (System.currentTimeMillis() - start));



		start = System.currentTimeMillis();
        query = "SELECT di, pi, value, short_name, long_name " +
					"FROM accdb.read_set_enum_sets S " +
					"JOIN accdb.read_set_enum_values V " +
					"ON S.enum_set_id = V.enum_set_id";
		rs = server.executeQuery(query);
		while(rs.next()) {
		}
		System.out.println("foreign map " + (System.currentTimeMillis() - start));
	


		query = "SELECT D.name, D.di, D.flags, D.description, P.pi, P.ssdn, P.size, P.atomic_size, P.def_size, P.trunk, " +
					"P.node, P.ftd, P.addressing_mode, P.default_data_event, S.di, S.primary_index, S.common_index, " +
					"S.primary_text, S.common_text, S.minimum, S.maximum, S.display_format, S.display_length, " +
					"S.scaling_length, S.scaling_offset, S.num_constants, S.const1, S.const2, S.const3, S.const4, " +
					"S.const5, S.const6, S.const7, S.const8, S.const9, S.const10, F.is_step_motor, F.is_contr_setting, " +
					"ALM.status, ALM.min_or_nom, ALM.max_or_tol, ALM.tries_needed, ALM.clock_event_no, ALM.subfunction_code, ALM.specific_data, " +
					"ALM.segment, TA.text, DDA.condition, DDA.mask, TD.text, DDA.condition, DDA.mask, TD.text, AUX.control_system_type, " +
					"AUX.long_name, AUX.long_description " +
					"FROM accdb.device D " +
					"LEFT OUTER JOIN accdb.property P ON D.di = P.di " +
					"LEFT OUTER JOIN accdb.device_analog_alarm DAA ON D.di = DAA.di " +
					"LEFT OUTER JOIN accdb.alarm_text TA ON DAA.alarm_text_id = TA.alarm_text_id " +
					"LEFT OUTER JOIN accdb.device_digital_alarm DDA ON D.di = DDA.di " +
					"LEFT OUTER JOIN accdb.alarm_text TD ON DDA.alarm_text_id = TD.alarm_text_id " +
					"LEFT OUTER JOIN accdb.alarm_block ALM ON (D.di = ALM.di and P.pi = ALM.pi) " +
					"LEFT OUTER JOIN accdb.device_scaling S ON (D.di = S.di and P.pi = S.pi) " +
					"LEFT OUTER JOIN accdb.device_flags F ON (D.di = F.di and P.pi = F.pi) " +
					"LEFT OUTER JOIN accdb.device_aux AUX ON (D.di = AUX.di)";

		//query = query + " WHERE D.name in ('M:OUTTMP')";	
		//query = query + " LIMIT 10000";	

		//for (int ii = 0; ii < 10; ii++) {
			start = System.currentTimeMillis();
			rs = server.executeQuery(query);

			while (rs.next()) {
				//System.out.println(rs.getString("name") + " " + rs.getInt("di") + " " + rs.getInt("pi"));
			}

			System.out.println("done " + (System.currentTimeMillis() - start));

			//Thread.sleep(2000);
		//}
	}
}
