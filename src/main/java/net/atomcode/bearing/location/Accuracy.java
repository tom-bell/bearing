package net.atomcode.bearing.location;

/**
 * Describes a location accuracy desired
 */
public enum Accuracy
{
	/**
	 * Within 100 metres
	 */
	LOW(100),
	/**
	 * Within 50 metres
	 */
	MEDIUM(50),
	/**
	 * Within 20 metres
	 */
	HIGH(20);

	public int value;

	Accuracy(int value)
	{
		this.value = value;
	}
}
