package com.agriknowledge.material;

public enum MaterialStatus {
	/** Visible only to admins. Everything is created here. */
	DRAFT,
	PUBLISHED,
	/** Withdrawn from the feed, but not deleted: comments and progress still point at it. */
	ARCHIVED
}
