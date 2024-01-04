// $Id: FTPPool.java,v 1.11 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.TimerTask;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.TimeNow;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequestContext;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReply;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReplyHandler;

//import gov.fnal.controls.servers.dpm.events.DataEvent;
//import gov.fnal.controls.servers.dpm.events.DataEventObserver;
//import gov.fnal.controls.servers.dpm.events.DeltaTimeEvent;
//import gov.fnal.controls.servers.dpm.events.OnceImmediateEvent;

import gov.fnal.controls.servers.dpm.scaling.DPMReadSetScaling;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.PoolUser;
import gov.fnal.controls.servers.dpm.pools.Node;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

//public class FTPPool implements AcnetReplyHandler, DaqPoolUserRequests<FTPRequest>, DataEventObserver, AcnetErrors, TimeNow
public class FTPPool extends TimerTask implements AcnetReplyHandler, DaqPoolUserRequests<FTPRequest>, AcnetErrors, TimeNow
{
/*
	private static int numFTPOldProtocol = 0;

	private static int numFTPNewProtocol = 0;

	private static int numFTPCancel = 0;

	private static int numFTPCancelRequests = 0;

	private static int numFTPCancelProcs = 0;

	private static int numFTPCollectionCompleted = 0;

	private static int numFTPInserts = 0;

	private static int numFTPDuplicateInserts = 0;

	private static int numFTPSharedInserts = 0;

	private static int numFTPProcs = 0;

	private static int numFTPDeleteRequests = 0;

	private static int numFTPDeleteShared = 0;

	private static int numFTPAcnetCancel = 0;

	private static int numFTPProcEnd = 0;

	private static int numFTPProcQuiet = 0;

	private static int numFTPFromPoolProc = 0;

	private static int numFTPProcSend = 0;

	private static int numFTPReplies = 0;

	private static int numFTPReplyMismatches = 0;

	private static int numFTPReplyErrors = 0;

	private static int numFTPFailed = 0;

	private static int numFTPSanity = 0;

	private static int numFTPScaleFail = 0;

	private static int numFTPEmptyPackets = 0;

	private static int numFTPCallbacks = 0;

	private static int numFTPReplyTimeouts = 0;

	private static int numFTPReplyTimeoutMismatches = 0;

	private static int numFTPReplyTimeoutSetup = 0;

	private static int numFTPReplyTimeoutSetupDelivered = 0;

	private static int numFTPReissueRequests = 0;

	private static int numFTPServicesAsks = 0;

	private static int numFTPServicesMatches = 0;

	private static int numFTPAcnetStartCancel = 0;

	private static int numFTPStartSetup = 0;

	private static int numFTPStartSetupErrors = 0;

	private static int numFTPReportStatistics = 0;

	private static String lastSetupError = null;

	private static int numTimeIncrementingComplaint = 0;

	private static int numIncrementing200 = 0;

	private static int numIncrementing1000 = 0;

	private static int numIncrementing2000 = 0;

	private static int numIncrementing4000 = 0;

	private static int numIncrementing6000 = 0;

	private static int numIncrementingLarge = 0;

	private static String lastTimeIncrementingComplaint = null;

	private static String lastFTPSanity = null;
*/

	//public final static int FTP_SPARE = 5;

	/**
	 * ACNET task name
	 */
	//private static int taskName;

	/**
	 * fast time plot connection
	 */
	//private static DaemonAcnetConnection ftpMgr;
	private static AcnetConnection acnetConnection;
	private static List<FTPPool> pools = new LinkedList<>();

	private List<SharedFTPRequest> userRequestList = new LinkedList<>();
	private List<FastScaling> activeRequestList;

	/**
	 * number of active devices
	 */
	private int numActive = 0;

	/**
	 * number of timeouts
	 */
	//public final static int TIMEOUT_SETUP = 3;

	/**
	 * in 15 hz ticks
	 */
	public final static int FTP_RETURN_PERIOD = 3;

	/**
	 * front end trunk
	 */
	//private int ftpTrunk;

	/**
	 * front end node
	 */
	//private int ftpNode;
	Node node;
	//private String ftpNodeName;

	/**
	 * scope for comparison
	 */
	private FTPScope ftpScope;

	/**
	 * class code
	 */
	private ClassCode ftpCode;

	/**
	 * class code for comparison
	 */
	private int ftpClassCode;

	/**
	 * new plot protocol when true
	 */
	private boolean newProtocol;

	/**
	 * ACNET send request object
	 */
	//private DaemonAcnetSendRequest acnetRequest = null;
	AcnetRequestContext context;

	/**
	 * outstanding setup request
	 */
	private boolean setupInProgress;

	/**
	 * flag indicating that request list should be rebuilt ASAP; error recovery
	 */
	private boolean reissueRequests;

	//private DeltaTimeEvent reissueRequestsEvent;

	//private DataEvent processFalseEvent = new OnceImmediateEvent();

	/**
	 * number of inserts since last processing
	 */
	//private int numInserts;

	private boolean modified;

	/**
	 * track device collection completion
	 */
	//private int numCollectionCompleted;

	/**
	 * getSnapShotPool success
	 */
	//private static int numFTPExists = 0;

	/**
	 * getSnapShotPool created pool
	 */
	//private static int numFTPCreates = 0;

	/**
	 * number of user inserts
	 */
	//private int totalDuplicateInserts = 0;

	/**
	 * number of duplicate shared user inserts
	 */
	//private int totalSharedInserts = 0;

	/**
	 * number of user cancels
	 */
	//private int totalCancels = 0;

	/**
	 * number of process
	 */
	//private int totalProcs = 0;

	/**
	 * number of deleted requests
	 */
	//private int totalProcDeletes = 0;

	/**
	 * number of shared deletions
	 */
	//private int totalProcSharedDeletes = 0;

	/**
	 * number of process resulting in ACNET cancels
	 */
	//private int totalProcEnd = 0;

	/**
	 * number of time stamps > 50000
	 */
	//private int totalPlus50KTimestamps = 0;

	//private int largestPlus50KTimestamp = 0;

	/**
	 * number of process deferred with active collection
	 */
	//private int totalProcDefer = 0;

	/**
	 * number of process resulting in no change
	 */
	//private int totalProcQuiet = 0;

	/**
	 * number of process resulting in ACNET traffic
	 */
	//private int totalProcSend = 0;

	/**
	 * number of cancel the previous
	 */
	//private int totalStartCancel = 0;

	/**
	 * number of setup messages send
	 */
	//private int totalStartSetup = 0;

	/**
	 * number of setup messages in error
	 */
	//private int totalStartSetupErrors = 0;

	/**
	 * number of collection completed
	 */
	//private int totalCollectionCompleted = 0;

	/**
	 * number of restarts
	 */
	//private int totalCollectionCompletedRestarts = 0;

	/**
	 * number of cancels
	 */
	//private int totalCollectionCompletedCancels = 0;

	/**
	 * number of queued failure recoveries
	 */
	//private int totalCollectionCompletedFailWaits = 0;

	/**
	 * number of wakes for failure recover
	 */
	//private int totalFailWaitWakes = 0;

	/**
	 * number of updates skipped
	 */
	//private int totalFailWaitSkips = 0;

	/**
	 * number of process for failure recovery
	 */
	//private int totalFailWaitProcs = 0;

	/**
	 * number of restart requests
	 */
	//private int totalRestartRequests = 0;

	/**
	 * number of restart request errors
	 */
	//private int totalRestartRequestErrors = 0;

	/**
	 * number of restart reply timeouts
	 */
	//private int totalRestartReplyTimeouts = 0;

	/**
	 * number of request reply mismatches
	 */
	//private int totalRestartReplyTimeoutMismatches = 0;

	/**
	 * number of restart replies
	 */
	//private int totalRestartReplies = 0;

	/**
	 * number of request reply mismatches
	 */
	//private int totalRestartReplyMismatches = 0;

	/**
	 * number of restart replies in error
	 */
	//private int totalRestartReplyErrors = 0;

	/**
	 * number of restart devices
	 */
	//private int totalRestartShared = 0;

	/**
	 * number of services matches
	 */
	//private int totalServicesMatches = 0;

	/**
	 * number of services queries
	 */
	//private int totalServicesAsks = 0;

	/**
	 * number of reply timeouts
	 */
	//private int totalReplyTimeouts = 0;

	/**
	 * number of request reply mismatches
	 */
	//private int totalReplyTimeoutMismatches = 0;

	/**
	 * number of timeouts on setup
	 */
	//private int totalReplyTimeoutSetup = 0;

	/**
	 * number of setup timeouts delivered
	 */
	//private int totalReplyTimeoutSetupDelivered = 0;

	/**
	 * number of reply timeouts after setup
	 */
	//private int totalReplyTimeoutAwaitingDelivered = 0;

	/**
	 * number pending
	 */
	//private int totalPending = 0;

	/**
	 * number wait on event
	 */
	//private int totalWaitEvent = 0;

	/**
	 * number wait on delay from event
	 */
	//private int totalWaitDelay = 0;

	/**
	 * number wait on collecting
	 */
	//private int totalWaitCollecting = 0;

	/**
	 * number ready to collect
	 */
	//private int totalOk = 0;

	/**
	 * number of strange success
	 */
	//private int totalOtherOk = 0;

	/**
	 * number of bad status
	 */
	//private int totalFailed = 0;

	/**
	 * number of replies delivered
	 */
	//private int totalReplies = 0;

	/**
	 * number of request reply mismatches
	 */
	//private int totalReplyMismatches = 0;

	/**
	 * number of replies in error
	 */
	//private int totalReplyErrors = 0;

	/**
	 * number of sanity rejections
	 */
	//private int totalSanity = 0;

	/**
	 * number of scaling failures
	 */
	//private int totalScaleFail = 0;

	/**
	 * number of points scaled
	 */
	//private int totalPoints = 0;

	/**
	 * number of packets with no points
	 */
	//private int totalEmptyPackets = 0;

	/**
	 * number of callbacks
	 */
	//private int totalCallbacks = 0;

	/**
	 * type code
	 */
	private short typecode;

	/**
	 * requesting task name
	 */
	private int r50Name;

	/**
	 * in 15 Hz ticks
	 */
	private short returnPeriod;

	/**
	 * start data return time in 15 Hz cycles
	 */
	private short startReturnTime;

	/**
	 * stop data return time in 15 Hz cycles
	 */
	private short stopReturnTime;

	/**
	 * priority
	 */
	//private int priority;

	/**
	 * The PLOT_PRIORITY_SDA constant.
	 */
	public final static int PLOT_PRIORITY_SDA = 3;

	/**
	 * The PLOT_PRIORITY_MCR constant.
	 */
	public final static int PLOT_PRIORITY_MCR = 2;

	/**
	 * The PLOT_PRIORITY_REMOTE_MCR constant.
	 */
	public final static int PLOT_PRIORITY_REMOTE_MCR = 1;

	/**
	 * The PLOT_PRIORITY_USER constant.
	 */
	public final static int PLOT_PRIORITY_USER = 0;

	/**
	 * current time (15 Hz) in Tevatron super cycle
	 */
	private short currentTime;

	/**
	 * for future use
	 */
	//private short[] spare = new short[FTP_SPARE];

/*
	private long[] isTimeIncrementing;

	private long[] isTimeIncrementingPriorBase02;

	private long[] isTimeIncrementingPriorBase02Root;

	private long[] isTimeIncrementingBase02Root;

	private long[] isTimeIncrementingPriorFeMillis;

	private long[] isTimeIncrementingFeMillis;

	private long[] isTimeIncrementingPriorTimeStampMillis;

	private long[] isTimeIncrementingTimeStampMillis;

	private long[] isTimeIncrementingPriorError;

	private long[] isTimeIncrementingError;
*/

	// The protocol is centered around the ACNET task name of the requester.
	// So, each pool must have a unique task name.
	static {
		//Rad50 r50 = new Rad50("FTP001");
		//byte[] tsk = r50.toByteArray();
		//int[] task = new int[4];

		//for (int ii = 0; ii < 4; ii++)
		//	task[ii] = (tsk[ii] & 0xff);

		//taskName = task[0] | (task[1] << 8) | (task[2] << 16) | (task[3] << 24);
		//ftpMgr = AcnetInterface.connect("FTPPLT"); // create new ACNET connection
		//ftpMgr = DaemonAcnetConnection.open("FTPPLT");
		acnetConnection = AcnetInterface.open("FTPPLT");
	}

	/**
	 * Constructs a FTPPool object with the FTP characteristics of this FTP
	 * request.
	 * 
	 * @param ftpRequest
	 *            a ftp request.
	 */
	FTPPool(FTPRequest ftpRequest)
	{
		this.ftpScope = ftpRequest.getScope();
		this.ftpCode = ftpRequest.getClassCode();
		this.ftpClassCode = ftpRequest.getFTPClassCode();
		this.context = new AcnetRequestContext();
		//ftpTrunk = ftpRequest.getDevice().getTrunk();
		//ftpNode = ftpRequest.getDevice().getNode();

		this.node = ftpRequest.getDevice().node();
		this.modified = false;

/*
        try {
            ftpNodeName = AcnetNodeInfo.get(ftpTrunk, ftpNode).name();
        } catch (Exception e) { 
            ftpNodeName = Integer.toString(ftpTrunk & 0xff) + 
                Integer.toString(ftpNode & 0xff); 
            }
*/

		if (ftpClassCode >= 11) {
			//++numFTPNewProtocol;
			this.newProtocol = true;
			this.typecode = 6;
		} else {
			//++numFTPOldProtocol;
			this.newProtocol = false;
			this.typecode = 2;
		}
		//r50Name = taskName++;
		this.returnPeriod = 0;
		this.startReturnTime = 0;
		this.stopReturnTime = 0;
		//priority = 0;
		this.currentTime = 0;
		//for (int ii = 0; ii < FTP_SPARE; ii++)
		//	spare[ii] = 0;

		this.reissueRequests = false;
		AcnetPoolImpl.sharedTimer.schedule(this, 30000);
	}

	/**
	 * Search for a FTPPool that supports the characteristics of this request.
	 * If a matching FTPPool is not found then create and initialize a new one.
	 * 
	 * @param request
	 *            a FTP request.
	 * @return either an existing FTPPool object describing the request list
	 *         which matches the specified trunk, node and return event OR a
	 *         newly created FTPPool object
	 */
	//public synchronized static FTPPool getFTPPool(FTPRequest request)
	//{
	//	Iterator<FTPPool> pools = ftpPools.iterator();
	//	for (FTPPool pool = null; pools.hasNext();) {
	//		pool = pools.next();
	//		if (pool.services(request)) {
//				//++numFTPExists;
	//			return pool;
	//		}
	//	}
	//	FTPPool pool = new FTPPool(request);
	//	ftpPools.add(pool);
//		//++numFTPCreates;
	//	return pool;
	//}

	public synchronized static FTPPool get(FTPRequest req)
	{
		for (FTPPool pool : pools) {
			if (pool.services(req))
				return pool;
		}

		final FTPPool pool = new FTPPool(req);

		pools.add(pool);

		return pool;
	}

	/**
	 * Adds a FTP request to a consolidated object (SharedFTPRequest) in the
	 * user request list. Creates the consolidated object if needed.
	 * 
	 * @param ftpRequest
	 *            FTPRequest object defining request.
	 */
	@Override
	public void insert(FTPRequest ftpRequest)
	{
		//++numFTPInserts;
		// look for shared in userRequestList, add to shared if existing
		synchronized (userRequestList) {
			Iterator<SharedFTPRequest> requests = userRequestList.iterator();
			for (SharedFTPRequest shared = null; requests.hasNext();) {
				//shared = (SharedFTPRequest) requests.next();
				shared = requests.next();
				if (shared.getDevice().isEqual(ftpRequest.getDevice())) {
					synchronized (shared) {
						shared.addUser(ftpRequest);
					}
					//++totalDuplicateInserts;
					//++numFTPDuplicateInserts;
					return;
				}
			}
			// create a shared which will add this user
			//if (requestPriority > priority)
			//	priority = requestPriority;
			SharedFTPRequest shared = new SharedFTPRequest(ftpRequest.getDevice(), ftpRequest.getClassCode(), ftpRequest.getScope());
			shared.addUser(ftpRequest);
			userRequestList.add(shared);
			modified = true;
		}
	}

	/**
	 * Deletes entries from the pool (userRequestList) by the DataScheduler
	 * reference returned when data acquisition was started. Deletes on the
	 * FTPPool class causes delete to be set on the WhatDaq objects hanging from
	 * SharedFTPRequest objects in the userRequestList.
	 * 
	 * @param user
	 *            the owner.
	 * @param error
	 *            the error.
	 */
	@Override
	public void cancel(PoolUser user, int error)
	{
		//++numFTPCancel;
		boolean anyDeletes = false;
		// poolStatistics(true);

		// mark all user requests with this Scheduler for delete
		synchronized (userRequestList) {
			Iterator<SharedFTPRequest> requests = userRequestList.iterator();

			for (SharedFTPRequest shared = null; requests.hasNext();) {
				//shared = (SharedFTPRequest) requests.next();
				shared = requests.next();
				synchronized (shared) {
					//Iterator<FTPRequest> users = shared.getUsers().iterator();

					//for (FTPRequest user : users) {
					for (FTPRequest req : shared.getUsers()) {
						//userPerhaps = users.next();
						if (req.getDevice().getUser() == user) {
							//if (forceError && !userPerhaps.getDelete()) {
								//if (userPerhaps.getSuppressRMICallback()) {
									//userPerhaps.callback.plotData( userPerhaps, System.currentTimeMillis(), null,
									//								error, 0, null, null, null);
									req.callback.plotData(System.currentTimeMillis(), error, 0, null, null, null);
								//} else
								//	try {
								//		userPerhaps.getCallback().plotReceive(
								//				null, null, null);
								//	} catch (Exception e) {
								//	}
							//}
							req.setDelete();
							anyDeletes = true;
							//++totalCancels;
							//++numFTPCancelRequests;
						}
					}
				}
			}
		}

		//if (anyDeletes) {
			//++numFTPCancelProcs;
		//	processFalseEvent.addObserver(this);
		//}
		// poolStatistics(true);
	}

	/**
	 * Give collected fast time plots last chance to return data.
	 * 
	 * @param user
	 *            the owner.
	 * @param forceError 
     *            force an error when true
	 * @param error 
     *            the error code
	 */

/*
	public void lastCall(PoolUser user, boolean forceError, int error)
	{
		//if (EngineShutdown.shuttingDown())
		//	return;
		boolean anyCalls = false;
		synchronized (userRequestList) {
			Iterator<SharedFTPRequest> requests = userRequestList.iterator();

			for (SharedFTPRequest shared = null; requests.hasNext();) {
				shared = requests.next();
				synchronized (shared) {
					Iterator<FTPRequest> users = shared.getUsers().iterator();
					for (FTPRequest userPerhaps = null; users.hasNext();) {
						userPerhaps = users.next();
						if (userPerhaps.getDevice().getUser() == user) {
							userPerhaps.lastCall(forceError, error);
							anyCalls = true;
						}
					}
				}
			}
		}
		if (anyCalls)
			collectionCompleted();
	}
	*/

	/**
	 * Process changes to a FTP pool. Users FTPRequests objects are removed if
	 * marked for delete. SharedFTPRequests objects are removed if no users
	 * remain.
	 * 
	 * @param forceRetransmission
	 *            force new request frames to be sent when true.
	 * @return true if changes processed successfully
	 */
	@Override
	public synchronized boolean process(boolean forceRetransmission)
	{
		//++totalProcs;
		//++numFTPProcs;
		//int numDeletes = 0;
		//int numAdds = numInserts;
		//numInserts = 0;

		int sendCount = 0;
		int byteCount = 0;

		boolean anyActiveUsers = false; //, anySharedUsers;
		//if (false) {
		//	System.out.println("processUserRequests, force: " + forceRetransmission + ", " + this);
		//	Thread.dumpStack();
		//}

		// remove all user requests marked for delete
		// remove all shared requests with no users
		reissueRequests = false;
		//synchronized (userRequestList) {
			Iterator<SharedFTPRequest> requests = userRequestList.iterator();

			while (requests.hasNext()) {
				final SharedFTPRequest shared = requests.next();
				boolean anySharedUsers = false;

				synchronized (shared) {
					final Iterator<FTPRequest> users = shared.getUsers().iterator();

					while (users.hasNext()) {
						final FTPRequest user = users.next();
						if (user.getDelete()) {
							// found a user request to delete
							users.remove();
							//++totalProcDeletes;
							//++numFTPDeleteRequests;
						} else {
							// someone still using this
							anySharedUsers = true;
						}
					}
				}

				if (!anySharedUsers) {
					shared.stopCollection();
					requests.remove();
					//numDeletes++;
					modified = true;
				} else {
					// at least one shared request in use
					//if (shared.getDevice().getDefaultLength() == 4)
					//	numDataBytes += 4;
					//else
					//	numDataBytes += 2;

					byteCount += (shared.getDevice().getDefaultLength() == 4 ? 4 : 2);
					anyActiveUsers = true;
					sendCount++;
				}
			}

			if (!anyActiveUsers) {
				context.cancelNoEx();
				//if (acnetRequest != null) {
					//++numFTPAcnetCancel;
				//	acnetRequest.cancelAcnetRequest();
				//	acnetRequest = null;
				//}
				//++totalProcEnd;
				//++numFTPProcEnd;
				// start a timer to clean up this pool
				// System.out.println("FTPPool is empty " + this);
				return false;
			}
		//}

		//if (numAdds == 0 && numDeletes == 0 && !forceRetransmission) {
			//++totalProcQuiet;
			//++numFTPProcQuiet;
		//	return false;
		//}

		if (!modified && !forceRetransmission)
			return false;

		// eventually, decide if request needs to be reissued - if not return
		//if (ftpCode.ftpFromPool()) {
			//++numFTPFromPoolProc;
		//	return true; // collection job already running
		//}

		//final int messageSize = newProtocol ? 32 + (22 * numToSend) : 18 + (14 * numToSend);

		//if (newProtocol)
			//messageSize = 32 + (22 * numToSend);
		//else
		//	messageSize = 18 + (14 * numToSend);
		//messageSize = newProtocol ? 32 + (22 * numToSend) : 18 + (14 * numToSend);

		//AcnetMessage msg = new AcnetMessage(messageSize);

		//numCollectionCompleted = 0;
		//++totalProcSend;
		//++numFTPProcSend;
		//startFTPPool(msg, numToSend, numDataBytes);
		startFTPPool(sendCount, byteCount);

		return true;
	}

	private boolean isTimeout(AcnetReply r)
	{
		final int s = r.status();

		return s == ACNET_REQTMO || s == ACNET_REPLY_TIMEOUT;
	}

	private void startFTPPool(int numDevices, int numDataBytes)
	{
		final ByteBuffer buf = ByteBuffer.allocate(32 * 1024).order(ByteOrder.LITTLE_ENDIAN);

		if (!newProtocol)
			typecode |= 0;

		//msg.putNextShort(typecode);
		//msg.putNextInt(r50Name);
		buf.putShort(typecode);
		buf.putInt(0);

		if (newProtocol)
			//msg.putNextShort((short) numDevices);
			buf.putShort((short) numDevices);
		else
			//msg.putNextShort((short) (numDevices | (currentTime << 8)));
			buf.putShort((short) (numDevices | (currentTime << 8)));
		
		returnPeriod = FTP_RETURN_PERIOD; // number 15 hz ticks between
											// returns
		//msg.putNextShort(returnPeriod);
		buf.putShort(returnPeriod);

		int msgSize = (int) ((numDataBytes + (numDevices << 1)) * ftpScope.rate);
																					
		msgSize += (6 * numDevices); // status, index, numPoints
		msgSize += (msgSize >> 1); // 50% extra
		msgSize /= (returnPeriod << 1); // to words

		if (msgSize > (DaqDefinitions.MaxAcnetMessageSize >> 1))
			msgSize = (DaqDefinitions.MaxAcnetMessageSize >> 1);

		//msg.putNextShort((short) msgSize);
		buf.putShort((short) msgSize);
		int referenceWord;

		if (ftpScope.onGroupEventCode)
			referenceWord = ftpScope.groupEventCode | (0x8000);
		else
			referenceWord = (ftpScope.clockEventNumber) & 0xff;

		//msg.putNextShort((short) referenceWord);
		//msg.putNextShort(startReturnTime);
		//msg.putNextShort(stopReturnTime);
		buf.putShort((short) referenceWord);
		buf.putShort(startReturnTime);
		buf.putShort(stopReturnTime);

		if (newProtocol) {
			//msg.putNextShort((short) 0);
			//msg.putNextShort(currentTime);
			//msg.putShort((short) 0);
			//msg.putShort((short) 0);
			//msg.putShort((short) 0);
			//msg.putShort((short) 0);
			//msg.putShort((short) 0);
			buf.putShort((short) 0);
			buf.putShort(currentTime);
			buf.putShort((short) 0);
			buf.putShort((short) 0);
			buf.putShort((short) 0);
			buf.putShort((short) 0);
			buf.putShort((short) 0);
			//for (int ii = 0; ii < FTP_SPARE; ii++)
			//	msg.putNextShort(spare[ii]);
		}

		// pack the packets
		// look for shared in userRequestList, add to shared if existing
		SharedFTPRequest shared;

		//if (acnetRequest != null) {
			//++totalStartCancel;
			//++numFTPAcnetStartCancel;
			//acnetRequest.cancelAcnetRequest();
			//acnetRequest = null;
		//}
		context.cancelNoEx();

		//final List<FastScaling> activeRequestList = new LinkedList<>();
		//final List<FastScaling> activeRequestList = buildActiveList();

		//synchronized (activeRequestList) {
			//activeRequestList.clear();
			//activeRequestList.addAll(buildActiveList());

			//final int numChannels = activeRequestList.size();

			/*
			isTimeIncrementing = new long[numChannels];
			isTimeIncrementingPriorBase02 = new long[numChannels];
			isTimeIncrementingPriorBase02Root = new long[numChannels];
			isTimeIncrementingBase02Root = new long[numChannels];
			isTimeIncrementingFeMillis = new long[numChannels];
			isTimeIncrementingPriorFeMillis = new long[numChannels];
			isTimeIncrementingTimeStampMillis = new long[numChannels];
			isTimeIncrementingPriorTimeStampMillis = new long[numChannels];
			isTimeIncrementingError = new long[numChannels];
			isTimeIncrementingPriorError = new long[numChannels];
			for (int ii = 0; ii < numChannels; ii++) {
				isTimeIncrementing[ii] = 0;
				isTimeIncrementingPriorBase02[ii] = 0;
			}
			*/
			//Iterator<FastScaling> arl = activeRequestList.iterator();

			activeRequestList = buildActiveList();

			for (FastScaling scaling : activeRequestList) {
			//for (FastScaling scaling : activeRequestList) {
			//for (FastScaling fast = null; arl.hasNext();) {
				//fast = arl.next();
				shared = scaling.shared;
				//shared = fast.shared;
				//msg.putNextInt(shared.getDevice().dipi());
				buf.putInt(shared.getDevice().dipi());

				if (newProtocol)
					//msg.putNextInt(shared.getDevice().getOffset());
					buf.putInt(shared.getDevice().getOffset());

				//msg.putNextBytes(shared.getDevice().getSSDN()); // DaqDefinitions.SSDN_SIZE);
				buf.put(shared.getDevice().getSSDN());
				short samplePeriod;
				samplePeriod = (short) (100000 / shared.getScope().rate);
				//msg.putNextShort(samplePeriod);
				buf.putShort(samplePeriod);

				if (newProtocol)
					//msg.putNextInt(0);
					buf.putInt(0);
			}
		//}
		//int timeoutSecs = 10;
		//setupInProgress = true;
		//++totalStartSetup;
		//++numFTPStartSetup;
		try {
			context = acnetConnection.requestMultiple(node.value(), "FTPMAN", buf, 1000 * 10, this);
		} catch (AcnetStatusException e) {
			deliverError(e.status);
		}
				//msg.message(), // request buffer
				//msg.messageSize(), // length of the request buffer
				//0, // offset from the beginning of the request buffer to send
				//true, // true=multiple reply request
				//this, // reply object to receive replies
				//(timeoutSecs != 0), // enable timeout
				//(timeoutSecs != 0), // enable timeout for multiple replies
				//timeoutSecs * 1000); // millisecond timeout period

		//if (!acnetRequest.transmit()) {
			//++totalStartSetupErrors;
			//++numFTPStartSetupErrors;
		//	int badStatus = acnetRequest.getSendRequestStatus();
		//	deliverError(badStatus);
		//	acnetRequest = null;
		//}

	}

	/**
	 * Collection is completed.
	 */
	//public void collectionCompleted()
	//{
		//if (++numCollectionCompleted >= numInserts) {
		//	++totalCollectionCompleted;
		//	++numFTPCollectionCompleted;
		//}
	//}

	private boolean services(FTPRequest ftp)
	{
		return ftp.getDevice().node().equals(node) && ftp.getFTPClassCode() == ftpClassCode &&
				ftp.getScope().equals(ftpScope);

		//if (ftp.getDevice().node().equals(node) || ftp.getFTPClassCode() != ftpClassCode)
		//	return false;

		//if (ftp.getScope().equals(ftpScope))
		//	return true;

		//return false;
	}


	/**
	 * Notification of an ACNET reply timeout.
	 * 
	 * @param numberConsecutiveTimeouts
	 *            count of consecutive timeouts.
	 * @param timeSinceReply
	 *            the time in milliseconds since the last reply.
	 * @param request
	 *            the originating request.
	 * @return true if request should be canceled
	 */
	//public boolean replyTimeout(int numberConsecutiveTimeouts, long timeSinceReply, DaemonAcnetSendRequest request)
	//{
		//++totalReplyTimeouts;
	//	++numFTPReplyTimeouts;
		//if (request != acnetRequest) {
			//++totalReplyTimeoutMismatches;
	//		++numFTPReplyTimeoutMismatches;
		//	return true;
		//}
		//if (setupInProgress) {
			//++totalReplyTimeoutSetup;
	//		++numFTPReplyTimeoutSetup;
			//if (numberConsecutiveTimeouts == TIMEOUT_SETUP) {
				//++totalReplyTimeoutSetupDelivered;
	//			++numFTPReplyTimeoutSetupDelivered;
	//			deliverError(ACNET_UTIME);
				//acnetRequest = new AcnetRequestContext();
	//			context.cancel();
				//context = new AcnetRequestContext();
				//return true;
		//	}
		//	return false;
		//}
	//	reissueRequests = true;
		//return false;
	//}

	@Override
	//public boolean replyReceive(DaemonAcnetHeader acnetHeader, byte[] replyBuffer, int replyLength, long replyTime)
	public void handle(AcnetReply r)	
	{
		final ByteBuffer hdr = r.data().order(ByteOrder.LITTLE_ENDIAN);
		final int replyLength = hdr.remaining();

		if (isTimeout(r)) {
			deliverError(ACNET_UTIME);
			context.cancelNoEx();
			reissueRequests = true;
		}

	//	++totalReplies;
	//	++numFTPReplies;
		int minReplyLength = 4 + (numActive << 1);

		//if (acnetRequest == null || acnetRequest.getRequestId() != acnetHeader.getMessageId()) {
	//		++totalReplyMismatches;
	//		++numFTPReplyMismatches;
	//		System.out.println("FTPPool, unwanted acnet message, request "
	//				+ acnetRequest + ", reply " + acnetHeader + ", pool "
	//				+ this);
		//	reissueRequests = true;
		//	return false;
		//}
		// check to see that this message is the one we want
		//int error = acnetHeader.getStatus();
		int error = r.status();

		if (error < 0 || replyLength < minReplyLength) {
	//		++totalReplyErrors;
	//		++numFTPReplyErrors;
			if (error != 0)
				deliverError(error);

			reissueRequests = true;
			//return false;
			return;
		}

		//AcnetMessage msg = new AcnetMessage(replyBuffer, replyLength);

		//error = msg.getNextShort();
		error = hdr.getShort();

		if (error < 0) {
	//		++totalReplyErrors;
	//		++numFTPReplyErrors;
	//		lastSetupError = this + " setup error: "
	//				+ Integer.toHexString(error) + " " + acnetHeader;
			deliverError(error);
			return;
		}

		//final short replyType = msg.getNextShort();
		final short replyType = hdr.getShort();

		if (replyType == 2)
			minReplyLength += (4 + (numActive << 2));

		if (replyLength < minReplyLength) {
	//		System.out.println("FTPPool, reply too short, len = " + replyLength
	//				+ ", " + acnetHeader + " " + this);
	//		++totalReplyErrors;
	//		++numFTPReplyErrors;
			reissueRequests = true;
			// deliverError(error);
			return;
		}

		if (replyType == 2) {
			//msg.getNextInt();
			hdr.getInt(); // unused
		}
		//SharedFTPRequest shared;

		//AcnetMessage packet = new AcnetMessage(replyBuffer, replyLength);

		//int entry = 0;
		//int timeCheckIndex = -1;
		synchronized (activeRequestList) {
			int activeRequestListSize = activeRequestList.size();
			//if (activeRequestListSize != isTimeIncrementing.length) {
			/*
				if (System.currentTimeMillis() - lastTimeIncrementingComplaintTime.getTime() > 60000) {
					StringBuffer complaint = new StringBuffer();
					complaint.append("FTPPool arl vs time check mismatch");
					complaint.append("\r\narl size: " + activeRequestListSize
							+ ", check size: " + isTimeIncrementing.length);
					complaint.append("\r\n" + this);
					complaint.append("\r\n" + dumpUserRequests());
					complaint.append("\r\n" + poolStats());
				}
				*/
			//}
			//Iterator<FastScaling> arl = activeRequestList.iterator();

			//for (FastScaling fast = null; arl.hasNext();) {
			for (FastScaling scaling : activeRequestList) {
				//if (timeCheckIndex + 1 < activeRequestListSize)
				//	;
	//				++timeCheckIndex;
		//		else
		//			System.out.println("FTPPool, timeCheckIndex: "
		//					+ timeCheckIndex + ", arlSize: "
		//					+ activeRequestListSize);
				//fast = arl.next();

				final SharedFTPRequest shared = scaling.shared;

				//entry++;
				//error = msg.getNextShort();
				error = hdr.getShort();
				if (error != 0) {
					//++totalFailed;
		//			++numFTPFailed;
					reissueRequests = true;
					shared.plotData(System.currentTimeMillis(), error, 0, null, null, null);

					if (replyType == 1) {
						continue;
					} else {
						hdr.getShort(); // ignore index
						hdr.getShort(); // ignore number points
						//msg.getNextShort(); // ignore index
						//msg.getNextShort(); // ignore number points
						continue;
					}
				}

				if (replyType == 1)
					continue;

				final int index = hdr.getShort() & 0xffff; // byte offset to next packet
				final int numPoints = hdr.getShort() & 0xffff;

				final ByteBuffer data = hdr.duplicate().order(ByteOrder.LITTLE_ENDIAN);

				//data.rewind();

				//if (numPoints < 0 || (index + (numPoints * scaling.pointSize)) > data.remaining()) {
				//	++totalSanity;
				//	++numFTPSanity;
					//int calculatedPoints = (replyLength - index) / (scaling.pointSize);
					//lastFTPSanity = this.toString() + "\r\nentry  " + entry
					//		+ " of " + numActive + ", replyLen = "
					//		+ replyLength + ", points = " + numPoints
					//		+ ", index " + index + ", pointSize "
					//		+ scaling.pointSize + " " + acnetHeader
					//		+ "\r\n\tcalculatedNumberPoints: "
					//		+ calculatedPoints;
					//reissueRequests = true;
					//return true;
					//return;
				//}
				//packet.setGetIndex(index);

				try {
					data.position(index);
				} catch (Exception e) {
					reissueRequests = true;
					return;
				}

				final long[] microSecs = new long[numPoints];
				final int[] nanoSecs = new int[numPoints];
				final double[] values = new double[numPoints];

				int nextData = 0;
				int nextTS = 0;
				int lastTS = 0;
				int firstTS = 0;
				int previousTS = 0;
				long nanos, previous = 0, baseTime02 = 0;
				//totalPoints += numPoints;

				for (int ii = 0; ii < numPoints; ii++) {
					previousTS = nextTS;
					//nextTS = packet.getNextShort();
					nextTS = data.getShort() & 0xffff;
					// System.out.println("nextTS: " + nextTS + ", " +
					// toString() + ", " + Integer.toHexString(nextTS));
					//nextTS &= 0x0000ffff;
					while (nextTS > 50000) {
						// System.out.println("nextTS: " + nextTS + ", " +
						// toString() + ", " + Integer.toHexString(nextTS));
						//++totalPlus50KTimestamps;
						//if (nextTS > largestPlus50KTimestamp)
						//	largestPlus50KTimestamp = nextTS;
						nextTS -= 50000;
					}
					nanos = nextTS * 100000L; // get nanoseconds since 0x02

					if (ii == 0) {
						baseTime02 = TimeStamper.getBaseTime02(nextTS / 10);
																				
						//isTimeIncrementingPriorBase02Root[timeCheckIndex] = isTimeIncrementingBase02Root[timeCheckIndex];
						//isTimeIncrementingBase02Root[timeCheckIndex] = baseTime02;
						//isTimeIncrementingPriorFeMillis[timeCheckIndex] = isTimeIncrementingFeMillis[timeCheckIndex];
						//isTimeIncrementingFeMillis[timeCheckIndex] = nextTS / 10;
						//isTimeIncrementingPriorTimeStampMillis[timeCheckIndex] = isTimeIncrementingTimeStampMillis[timeCheckIndex];
						//isTimeIncrementingTimeStampMillis[timeCheckIndex] = System.currentTimeMillis() - (baseTime02 / 1000000);
						//isTimeIncrementingPriorError[timeCheckIndex] = isTimeIncrementingError[timeCheckIndex];
						//isTimeIncrementingError[timeCheckIndex] = System.currentTimeMillis() - ((nanos + baseTime02) / 1000000);
						//if (baseTime02 / 1000000 - System.currentTimeMillis() > 1000) {
						//	System.out.println("FTPPool, first base time not very good");
						//}
						previous = nanos; // base time handles first one
						firstTS = nextTS;
					}
					if ((previous - nanos) > 2500000000L) {// wrapped around zero within the packet
						baseTime02 += (5000000000L); // timestamps wrap every
														// 5 seconds
						if (baseTime02 / 1000000 - System.currentTimeMillis() > 1000) {
							baseTime02 -= (5000000000L);
						}
					}
					previous = nanos;
					lastTS = nextTS;
					nanos += baseTime02;
					microSecs[ii] = (nanos / 1000);
					nanoSecs[ii] = (short) (nanos % 1000);

					/*
					if ((microSecs[ii] - isTimeIncrementing[timeCheckIndex]) < -150000L) // 150
																							// milliseconds
					{
						++numTimeIncrementingComplaint;
						int delta = (int) Math
								.abs(((microSecs[ii] - isTimeIncrementing[timeCheckIndex]) / 1000));
								.toString();
								isTimeIncrementing[timeCheckIndex] / 1000)
								.toString();
								.toString();
						lastTimeIncrementingComplaint = toString()
								+ " < prior:\r\n" + lastPointDate + ", delta: " + delta
								+ "\r\nUCDError: " + TimeStamper.getUCDTimeError()
								+ "\r\nindex: " + ii + ", micros: " + microSecs[ii]
								+ ", prior: " + isTimeIncrementing[timeCheckIndex]
								+ ", base02: " + base02Date
								+ "\r\npriorTS: "
								+ (isTimeIncrementing[timeCheckIndex] - 
										isTimeIncrementingPriorBase02[timeCheckIndex] / 1000) / 100
										isTimeIncrementingPriorBase02[timeCheckIndex] / 1000000)
										isTimeIncrementingPriorBase02Root[timeCheckIndex] / 1000000)
								+ "\r\nnextTS: " + nextTS + ", ii: " + ii + ", numPoints: " + numPoints
								+ ", firstTS: " + firstTS + ", previousTS: " + previousTS
								+ "\r\nbase02Root: "
										isTimeIncrementingBase02Root[timeCheckIndex] / 1000000)
								+ "\r\nfeMillis: " + isTimeIncrementingFeMillis[timeCheckIndex]
								+ ", prior feMillis: " + isTimeIncrementingPriorFeMillis[timeCheckIndex]
								+ "\r\ntsMillis: " + isTimeIncrementingTimeStampMillis[timeCheckIndex]
								+ ", prior tsMillis: " + isTimeIncrementingPriorTimeStampMillis[timeCheckIndex]
								+ "\r\nerror: " + isTimeIncrementingError[timeCheckIndex]
								+ ", prior error: " + isTimeIncrementingPriorError[timeCheckIndex]
								+ "\r\n"; // suppress AcnetMessage dump
											// (rrorbl tests)
						// "\r\nMsg: " + new AcnetMessage(replyBuffer,
						// replyLength);

						if (false && delta > 200) {
							System.out.println(lastTimeIncrementingComplaint);
						}
						if (delta < 200)
							++numIncrementing200;
						else if (delta < 1000)
							++numIncrementing1000;
						else if (delta < 2000)
							++numIncrementing2000;
						else if (delta < 4000)
							++numIncrementing4000;
						else if (delta < 6000)
							++numIncrementing6000;
						else
							++numIncrementingLarge;
						if (delta > 2500) {
							if (false && System.currentTimeMillis()
									- lastTimeIncrementingComplaintTime
											.getTime() > 60000) {
							}
						}
					}
					*/

					//isTimeIncrementing[timeCheckIndex] = microSecs[ii];
					//isTimeIncrementingPriorBase02[timeCheckIndex] = baseTime02;
					// microSecs[ii] = 0xffff & packet.getNextShort();
					//if (scaling.bigRawData)
						//nextData = packet.getNextInt();
					//else
						//nextData = packet.getNextShort();

					nextData = scaling.bigRawData ? data.getInt() : data.getShort();

					try {
						//values[ii] = ((ReadSetScaling) fast.scaling).pdudcu(nextData);
						values[ii] = scaling.scaling.rawToCommon(nextData);
					} catch (Exception e) {
					//	++totalScaleFail;
					//	++numFTPScaleFail;
					//	System.out.println("FTPPoolScaling error " + e);
						values[ii] = 0.0;
					}
				}
				if (numPoints == 0) {
					//++totalEmptyPackets;
					//++numFTPEmptyPackets;
				} else {
					//++totalCallbacks;
					//++numFTPCallbacks;
					//shared.plotData(shared, System.currentTimeMillis(), null, error, numPoints,
					//		microSecs, nanoSecs, values);
					shared.plotData(System.currentTimeMillis(), error, numPoints, microSecs, nanoSecs, values);
				}
			}
		}

		//return true;
	}

	/**
	 * Returns true if this ACNET request is valid.
	 * 
	 * @param request
	 *            the ACNET request.
	 * @return true if request is valid
	 * 
	 */
//	public boolean isRequestValid(DaemonAcnetSendRequest request) {
//		if (acnetRequest == request)
//			return true;
//		return false;
//	}

	/**
	 * Returns a description of this ACNET request.
	 * 
	 * @return a description of this ACNET request
	 */
//	public String requestDescription() {
//		return ("FTPPool collection");
//	}

	/**
	 * Cannot collect this plot, deliver an error.
	 * 
	 * @param error
	 *            error to be delivered.
	 */
	private void deliverError(int error)
	{
		synchronized (userRequestList) {
			//Iterator<SharedFTPRequest> requests = userRequestList.iterator();
			//for (SharedFTPRequest shared = null; requests.hasNext();) {
			for (SharedFTPRequest shared : userRequestList) {
				//shared = requests.next();
				//shared = (SharedFTPRequest) requests.next();
				shared.deliverError(error);
			}
		}

		reissueRequests = true;
	}


	/**
	 * Return a string describing this FTPPool.
	 * 
	 * @return a string describing this FTPPool
	 */
	@Override
	public String toString()
	{
		return "FTPPool: node: " + node + " " + ftpScope + " class code = " + ftpClassCode;
	}

	private class FastScaling
	{
		private SharedFTPRequest shared;
		private DPMReadSetScaling scaling;
		private boolean bigRawData;
		private int pointSize = 4; // minimum size of one plot point

		/**
		 * Create a FastScaling object.
		 * 
		 * @param shared
		 *            the shared request.
		 * @param bigRawData
		 *            big raw data when true.
		 */
		private FastScaling(SharedFTPRequest shared, boolean bigRawData)
		{
			this.shared = shared;
			this.scaling = DPMReadSetScaling.get(shared.getDevice());
			this.bigRawData = bigRawData;
			if (bigRawData)
				pointSize += 2;
		}
	}

	/**
	 * Build the active request list.
	 * 
	 * @return the active request list
	 */
	private synchronized List<FastScaling> buildActiveList()
	{
		List<FastScaling> list = new LinkedList<>();
		//FastScaling fast;
		//boolean bigData;

		//numActive = 0;
		//synchronized (userRequestList) {
			//Iterator<SharedFTPRequest> requests = userRequestList.iterator();

			//for (SharedFTPRequest shared = null; requests.hasNext();) {
			for (SharedFTPRequest shared : userRequestList) {
				//shared =  requests.next();
				//if (shared.getDevice().getDefaultLength() == 4)
				//	bigData = true;
				//else
				//	bigData = false;

				final boolean bigData = shared.getDevice().getDefaultLength() == 4;
				//fast = new FastScaling(shared, bigData);
				list.add(new FastScaling(shared, bigData));
		////		++numActive;
			}
		//}

		return list;
	}

	/**
	 * The data event we have been observing has occurred. This is for
	 * processing a restart after a failure.
	 * 
	 * @param userEvent
	 *            my request.
	 * @param currentEvent
	 *            now.
	 */
	//@Override
	//public void update(DataEvent userEvent, DataEvent currentEvent) {
		//if (userEvent == reissueRequestsEvent) {
		//	if (reissueRequests) {
		//		++totalRestartRequests;
		//		++numFTPReissueRequests;
		//		process(true);
		//	} else
		//		process(false);
		//}
	//}

	@Override
	public void run()
	{
		//if (userEvent == reissueRequestsEvent) {
			//if (reissueRequests) {
		//		++totalRestartRequests;
		//		++numFTPReissueRequests;
			//	process(true);
			//} else
			//	process(false);
		//}

		logger.log(Level.FINER, "FTPPool reissue requests " + reissueRequests);
		process(reissueRequests);
		AcnetPoolImpl.sharedTimer.schedule(this, 30000);
	}

	/**
	 * Debug, print the contents of the userRequestList.
     * 
	 * @return contents of userRequestList
	 */
	//public String dumpUserRequests() {
	//	StringBuffer returnBuffer = new StringBuffer();
	//	returnBuffer.append("\r\n\tFTPPool userRequestList:");
	//	//returnBuffer.append(DaqPool.dumpList(userRequestList));
	//	returnBuffer.append("\r\n");
	//	return returnBuffer.toString();
	//}

	/**
	 * Return some FTP pool statistics.
	 * 
	 * @return a statistics report
	 */
	 /*
	public String poolStats() {
		StringBuffer returnBuffer = new StringBuffer();

		if (false) {
			returnBuffer.append("\r\n\tclass: creates: " + numFTPCreates
					+ ", reuse: " + numFTPExists + ", pool: services: "
					+ totalServicesMatches + ", asks: " + totalServicesAsks);
		}
		returnBuffer.append("\r\n\tinserts: " + totalSharedInserts
				+ ", duplicates: " + totalDuplicateInserts + ", cancels: "
				+ totalCancels + ", >50K ts: " + totalPlus50KTimestamps + ", "
				+ largestPlus50KTimestamp);
		returnBuffer.append("\r\n\tprocs: total: " + totalProcs + ", deletes: "
				+ totalProcDeletes + ", sharedDeletes: "
				+ totalProcSharedDeletes + ", cancels: " + totalProcEnd);
		returnBuffer.append("\r\n\tprocs: defer: " + totalProcDefer
				+ ", quiet: " + totalProcQuiet + ", send: " + totalProcSend);
		returnBuffer.append("\r\n\tstart: cancel: " + totalStartCancel
				+ ", setup: " + totalStartSetup + ", errs: "
				+ totalStartSetupErrors);
		returnBuffer.append("\r\n\tcollectionComplete: total: "
				+ totalCollectionCompleted + ", restarts: "
				+ totalCollectionCompletedRestarts + ", cancels: "
				+ totalCollectionCompletedCancels + ", failWaits: "
				+ totalCollectionCompletedFailWaits);
		if (totalCollectionCompletedFailWaits != 0) {
			returnBuffer.append("\r\n\tfailWait: wakes: " + totalFailWaitWakes
					+ ", skips: " + totalFailWaitSkips + ", procs: "
					+ totalFailWaitProcs);
		}
		if (totalRestartRequests != 0) {
			returnBuffer
					.append("\r\n\trestarts: total: " + totalRestartRequests
							+ ", qerrs: " + totalRestartRequestErrors
							+ ", tmo: " + totalRestartReplyTimeouts + ", msm: "
							+ totalRestartReplyTimeoutMismatches
							+ ", replies: " + totalRestartReplies + ", msm: "
							+ totalRestartReplyMismatches + ", perrs: "
							+ totalRestartReplyErrors + ", devs: "
							+ totalRestartShared);
		}
		if (totalReplyTimeouts != 0) {
			returnBuffer.append("\r\n\treplyTmo: total: " + totalReplyTimeouts
					+ ", msm: " + totalReplyTimeoutMismatches + ", setup: "
					+ totalReplyTimeoutSetup + ", setupDelivered: "
					+ totalReplyTimeoutSetupDelivered + ", awaiting: "
					+ totalReplyTimeoutAwaitingDelivered);
		}

		if (totalPending != 0) {
			returnBuffer.append("\r\n\twait: pend: " + totalPending
					+ ", event: " + totalWaitEvent + ", wait: "
					+ totalWaitDelay + ", collect: " + totalWaitCollecting
					+ ", ok: " + totalOk + ", otherOk: " + totalOtherOk
					+ ", fail: " + totalFailed);
		}
		returnBuffer.append("\r\n\treplies: total: " + totalReplies + ", msm: "
				+ totalReplyMismatches + ", errs: " + totalReplyErrors
				+ ", sanity: " + totalSanity);
		if (totalPoints != 0) {
			returnBuffer.append("\r\n\ttotal: scaleFail: " + totalScaleFail
					+ ", points: " + totalPoints + ", empties: "
					+ totalEmptyPackets + ", callbacks: " + totalCallbacks);
		}
		return returnBuffer.toString();
	}
	*/
}
