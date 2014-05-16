package net.atomcode.bearing;

/**
 * Basic listener for bearing tasks
 */
public interface BearingListener
{
	/**
	 * Failure due to timeout
	 */
	public void onTimeout();

	/**
	 * Generic failure
	 */
	public void onFailure();
}
