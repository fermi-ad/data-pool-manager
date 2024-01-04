// $Id: DaqPoolObserverInterface.java,v 1.1 2022/11/01 20:35:46 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;

import java.util.List;

/**
 * The interface an observer must implement to track pool transactions.
 * 
 * @author Kevin Cahill
 * @version 0.01<br>
 *          Class created: 25 Oct 2001
 * 
 */

public interface DaqPoolObserverInterface {
	/**
	 * Inform an observer of pool creation or existence. User may access pool's
	 * isSettingPool(), getEvent(), and getFrontEndStatus() to learn more about
	 * the pool.
	 * 
	 * @param pool
	 *            the datapool.
	 */
	public void daqPoolCreate(DaqPool pool);

	/**
	 * Inform an observer of pool data acquisition initiation or existence.
	 * 
	 * @param pool
	 *            the datapool.
	 * @param items
	 *            the request list object to process -- an iterator containing
	 *            items.
	 */
	public void daqPoolInitiate(DaqPool pool, List<WhatDaq> items);

	/**
	 * Inform an observer of a pool cancel. One shot pools will not deliver a
	 * cancel.
     * @param pool
	 */
	public void daqPoolCancel(DaqPool pool);

	/**
	 * Inform an observer of a local client plot initiation.
     * @param ftpCollector
     * @param data
     * @param offset
     * @param classCode
     * @param samplePeriod
	 */
	//public void ftpInitiate(
	//		FtpCollector.FtpPlotCollectionSpecification ftpCollector,
	//		OpenAccessClientData data, int offset, ClassCode classCode,
	//		int samplePeriod);

	/**
	 * Inform an observer of a local client plot cancel.
     * @param ftpCollector
	 */
	//public void ftpCancel(
	//		FtpCollector.FtpPlotCollectionSpecification ftpCollector);

	/**
	 * Inform an observer of a local client plot initiation.
     * @param snapCollector
     * @param data
     * @param offset
     * @param classCode
     * 
	 */
	//public void snapInitiate(
	//		SnapCollector.SnapPlotCollectionSpecification snapCollector,
	//		OpenAccessClientData data, int offset, ClassCode classCode);
//
	/**
	 * Inform an observer of a local client plot cancel.
     * @param snapCollector
	 */
//	public void snapCancel(
//			SnapCollector.SnapPlotCollectionSpecification snapCollector);

} // end DaqPoolObserverInterface
