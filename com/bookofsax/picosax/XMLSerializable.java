package com.bookofsax.picosax;

public interface XMLSerializable {
  public String toXML();
  public boolean fromXML(picoDOMElement pde);
}