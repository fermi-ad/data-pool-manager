// $Id: JobInfo.java,v 1.14 2023/11/02 16:36:15 kingc Exp $
package gov.fnal.controls.servers.dpm;

import java.util.Map;
import java.util.Collection;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class JobInfo
{
	final Map<Long, DPMRequest> requests = new ConcurrentHashMap<>();
	Job job = NullJob.instance;

	private JobModel model = LiveDataModel.instance;
	private boolean needsStart = true;

	protected JobInfo()
	{
	}

	protected JobInfo(JobModel model)
	{
		this.model = model;
	}

	void addRequest(long refId, DPMRequest req)
	{
		final DPMRequest old = requests.get(refId);

		if (!req.equals(old)) {
			requests.put(refId, req);
			needsStart = true;
		}
	}

	void setModel(JobModel newModel)
	{
		if (!newModel.equals(model)) {
			model = newModel;
			needsStart = true;
		}
	}

	void setModel(String newModelStr)
	{
		setModel(JobModel.parse(newModelStr));
	}

	void removeRequest(long refId)
	{
		if (requests.remove(refId) != null)
			needsStart = true;
	}

	void clearRequests()
	{
		if (!requests.isEmpty()) {
			requests.clear();
			needsStart = true;
		}
	}

	//void start(Model model, DPMList list) throws InterruptedException, IOException, AcnetStatusException
	void start(DPMList list) throws InterruptedException, IOException, AcnetStatusException
	{
		if (needsStart) {
			needsStart = false;

			final Job newJob = model.createJob(list);
			newJob.start(requests);
			job.stop();
			job = newJob;
		}
	}

	void stop()
	{
		job.stop();
	}

	boolean completed()
	{
		return job.completed();
	}

	final public WhatDaq whatDaq(int id)
	{
		return job.whatDaq(id);
	}

	final public  Collection<WhatDaq> whatDaqs()
	{
		return job.whatDaqs();
	}

	final public long devicePropertyCount(int dipi)
	{
		return job.whatDaqs().stream().filter(w -> w.dipi() == dipi).count();
	}

	final public long nodeCount(int node)
	{
		return job.whatDaqs().stream().filter(w -> w.node().value() == node).count();
	}
}
