// $Id: AcnetMessage.java,v 1.2 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.logging.Level;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class AcnetMessage extends AcnetPacket
{
	//AcnetMessage(AcnetConnection connection, Buffer buf)
	AcnetMessage(Buffer buf)
	{
		//super(connection, buf);
		super(buf);
	}

	@Override
	void handle(AcnetConnection connection)
	{
		try {
			connection.messageHandler.handle(this);

			AcnetInterface.stats.messageReceived();
			connection.stats.messageReceived();
		} catch (Exception e) {
			logger.log(Level.WARNING, "ACNET message callback exception for connection '" + connection.connectedName() + "'", e);
		} finally {
			data = null;
			buf.free();
		}
	}

	@Override
	final public int status()
	{
		return 0;
	}
}
