package com.muzima.listeners;


public interface DownloadListener<T> {
	void downloadTaskComplete(T result);
	void downloadTaskStart();
}
