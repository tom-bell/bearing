package net.atomcode.bearing;

/**
 * Task interface for all bearing tasks
 */
public interface BearingTask
{
	/**
	 * Begin the defined task
	 */
	public BearingTask start();

	/**
	 * Cancel the current task
	 */
	public void cancel();

	/**
	 * Check to see if the task is currently running
	 */
	public boolean isRunning();

	/*
	 * =================================
	 * BEARING TASK API
	 * =================================
	 */

	/**
	 * Timeout the task if it has not completed in the given time period
	 * @param timeout The timeout in milliseconds
	 */
	public BearingTask timeout(long timeout);
}
