package net.atomcode.bearing;

/**
 * Task interface for all bearing tasks
 */
public interface BearingTask
{
	public static final int FALLBACK_NONE = 0x0;

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
	 * @param action The action to perform if the task times out, default FALLBACK_NONE
	 * @param timeout The timeout in milliseconds
	 */
	public BearingTask fallback(int action, long timeout);
}
