package com.agriknowledge.mail;

/**
 * How an email actually leaves the building.
 *
 * <p>There are two of these because the platform forces it. Railway blocks
 * outbound SMTP — both 587 and 465 time out at the connection stage, while the
 * same container reaches Postgres and the wider internet without trouble — so an
 * SMTP transport that works perfectly in local development cannot send a single
 * message in production. The HTTP transport goes out over 443, which nothing
 * blocks.
 */
public interface EmailTransport {

	/** @return true if the message was accepted for delivery */
	boolean send(String to, String subject, String body);

	/** Shown in the startup log so the running configuration is never a mystery. */
	String describe();

}
