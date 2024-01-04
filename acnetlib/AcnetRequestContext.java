// $Id: AcnetRequestContext.java,v 1.2 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import gov.fnal.controls.servers.dpm.pools.Node;

final public class AcnetRequestContext implements AcnetConstants, AcnetErrors
{
	final AtomicReference<AcnetConnection> connection;
	final String task;
	final int taskId;
	final Node node;
	final RequestId requestId;
	final boolean isMult;
	final long timeout;

	//ByteBuffer data;

	protected volatile AcnetReplyHandler replyHandler;

	public AcnetRequestContext()
	{
		this.connection = new AtomicReference<>();
		this.task = "";
		this.taskId = -1;
		this.node = null;
		this.requestId = new RequestId(0);
		this.isMult = false;
		this.timeout = -1;
		this.replyHandler = null;
	}

/*
    AcnetRequestContext(AcnetConnection c, int reqid, int node, AcnetReplyHandler replyHandler) 
	{
		this.c = new AtomicReference<>(c);
		this.requestId = new RequestId(reqid);
		this.node = node;
		this.replyHandler = replyHandler;
    }
	*/

    AcnetRequestContext(AcnetConnection connection, String task, int node, RequestId requestId, boolean isMult, long timeout, AcnetReplyHandler replyHandler) throws AcnetStatusException
	{
		//System.out.println("create new acnet request context 1");

		this.connection = new AtomicReference<>(connection);
		this.task = task;
		this.taskId = connection.taskId;
		//System.out.println("create new acnet request context 2");
		this.node = Node.get(node);
		//System.out.println("create new acnet request context 3");
		this.requestId = requestId;
		this.isMult = isMult;
		this.timeout = timeout;
		this.replyHandler = replyHandler;

		//System.out.println("create new acnet request context 5");
    }

	int taskId() throws AcnetStatusException
	{
		final AcnetConnection cLocal = connection.get();
		
		if (cLocal != null)
			return cLocal.taskId;

		throw new AcnetStatusException(ACNET_NO_SUCH);
	}

	void sendEndMult()
	{
		//final AcnetConnection cLocal = c.getAndSet(null);

		//if (cLocal != null)
		//	replyHandler.handle(new AcnetReply(cLocal, ACNET_ENDMULT, requestId, node.value()));
		replyHandler.handle(new AcnetReply(ACNET_ENDMULT, requestId, node.value()));
	}
	
	void localCancel()
	{
		connection.set(null);
	}

	public void setReplyHandler(AcnetReplyHandler replyHandler)
	{
		this.replyHandler = replyHandler;
	}

	public RequestId requestId()
	{
		return requestId;
	}

	public int hashCode()
	{
		return requestId.hashCode();
	}

    public boolean isCancelled() 
	{
		return connection.get() == null;
    }

    public void cancel() throws AcnetStatusException 
	{
		final AcnetConnection cLocal = connection.getAndSet(null);

		if (cLocal != null)
			cLocal.sendCancel(this);
    }

	public void cancelNoEx()
	{
		try {
			cancel();
		} catch (Exception ignore) { }
	}

	public String toString()
	{
		return "AcnetRequestContext:" + requestId + (isCancelled() ? " (cancelled)" : " (active)");
	}
}
