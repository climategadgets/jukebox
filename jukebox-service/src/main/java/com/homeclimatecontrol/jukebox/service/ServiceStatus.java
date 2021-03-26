package com.homeclimatecontrol.jukebox.service;

/**
 * @author <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a>
 */
public interface ServiceStatus {
  boolean isActive();
  boolean isEnabled();
  boolean isReady();
  long getUptimeMillis();
  String getUptime();
}