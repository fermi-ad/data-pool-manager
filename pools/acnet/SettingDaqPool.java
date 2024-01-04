// $Id: SettingDaqPool.java,v 1.8 2023/11/01 20:56:57 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.List;
import java.util.Iterator;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;

import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;
import gov.fnal.controls.servers.dpm.pools.Node;

public class SettingDaqPool extends OneShotDaqPool implements ReceiveData, Completable, AcnetErrors
{
	public SettingDaqPool(Node node)
	{
		super(node);
	}

	@Override
	public boolean isSetting()
	{
		return true;
	}

	@Override
	public void insert(WhatDaq whatDaq)
	{
		if (whatDaq.getLength() > DaqDefinitions.MaxReplyDataLength()) {
			PoolSegmentAssembly.insert(whatDaq, this, 8192, false);
			return;
		}

		Iterator<WhatDaq> url = queuedReqs.iterator();

		WhatDaq userPerhaps;
		while (url.hasNext()) {
			userPerhaps = (WhatDaq) url.next();
			if (userPerhaps.isEqual(whatDaq)) {
				userPerhaps.getReceiveData().receiveData(DAE_SETTING_SUPERCEDED, 0, null, System.currentTimeMillis());
				url.remove();
				break;
			}
		}
		queuedReqs.add(whatDaq);
	}

	@Override
	public String toString()
	{
		return "SettingDaqPool " + super.toString();
	}
}
