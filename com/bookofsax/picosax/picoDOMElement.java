package com.bookofsax.picosax;

import java.util.*;

/**
 * Title:        Industrial WebEngine
 * Description:  High-volume content server application.
 * Copyright:    Copyright (c) W. Scott Means
 * Company:      Industrial Web Machines
 * @author W. Scott Means
 * @version 1.0
 */

public class picoDOMElement
{
  picoDOM pdOwner = null;
  public String tagName;
  public HashMap attrs = new HashMap();
  public Vector content = new Vector();

  /**
   * Simple constructor that initializes fixed members.
   */
  picoDOMElement(picoDOM pdOwner, String tagName)
  {
    this.pdOwner = pdOwner;
    this.tagName = tagName;
  }

  /**
   * Returns the text portion of this element and its contents.
   */
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    Iterator i;

    if (content.size() > 0) {
      for (i = content.iterator(); i.hasNext(); sb.append(i.next().toString()))
        ;
    }

    return sb.toString();
  }

  /**
   * Returns the XML that corresponds to this element and its contents.
   */
  public String toXML()
  {
    StringBuffer sb = new StringBuffer();
    Iterator i;

    sb.append("<" + tagName);

    for (i = attrs.keySet().iterator(); i.hasNext();) {
      String strAttrName = (String)i.next();
      sb.append(" " + strAttrName + "=\"" + attrs.get(strAttrName) + "\"");
    }

    if (content.size() > 0) {
      sb.append(">");
      for (i = content.iterator(); i.hasNext(); ) {
        Object obj = i.next();

        if (obj instanceof picoDOMElement) {
          sb.append(((picoDOMElement)obj).toXML());
        } else {
          sb.append(obj.toString());
        }
      }
      sb.append("</" + tagName + ">");
    } else {
      sb.append("/>");
    }

    return sb.toString();
  }

  /**
   * A simple parser that compares a pseudo-XPath location step to a particular
   * tag name.
   */
  public static boolean compareLocStep(String strLocStep, String strTagName)
  {
    int iPos = strLocStep.indexOf('*');

    if (iPos >= 0) {
      String strFixed = strLocStep.substring(0, iPos);

      if (strTagName.length() >= iPos) {
        return strTagName.substring(0, iPos).equals(strFixed);
      } else {
        return false;
      }
    } else {
      return strLocStep.equals(strTagName);
    }
  }

  /**
   * Returns a vector of <code>picoDOMElement</code> objects that match the
   * pseudo-XPath given.
   */
  public Vector selectNodes(String strXPath)
  {
    StringTokenizer st = new StringTokenizer(strXPath, "/");
    Vector vecSearch;
    Vector vecResult = content;
    Vector vecRet = new Vector();

    while(st.hasMoreTokens()) {
      vecSearch = vecResult;
      vecResult = new Vector();

      String strLocStep = st.nextToken();
      Object obj;

      for (Iterator i = vecSearch.iterator(); i.hasNext();) {
        obj = i.next();

        if (obj instanceof picoDOMElement) {
          picoDOMElement pde = (picoDOMElement)obj;

          if (compareLocStep(strLocStep, pde.tagName)) {
            if (!st.hasMoreTokens()) {
              vecRet.add(pde);
            } else {
              vecResult.addAll(pde.content);
            }
          }
        }
      }
    }

    return vecRet;
  }

  /**
   * Returns the first <code>picoDOMElement</code> object that match the
   * pseudo-XPath given (in document order). Really a wrapper for the recursive
   * <code>StringTokenizer</code> based method.
   */
  public picoDOMElement selectSingleNode(String strXPath, boolean fCreate)
  {
    StringTokenizer st = new StringTokenizer(strXPath, "/");

    return selectSingleNode(st, fCreate);
  }

  public picoDOMElement selectSingleNode(String strXPath)
  {
    StringTokenizer st = new StringTokenizer(strXPath, "/");

    return selectSingleNode(st, false);
  }

  /**
   * Returns the first <code>picoDOMElement</code> object that match the
   * pre-parsed pseudo-XPath given (in document order). If <code>fCreate</code>
   * is true, the element path is created if it doesn't exist.
   */
  public picoDOMElement selectSingleNode(StringTokenizer st, boolean fCreate)
  {
    if (st.hasMoreTokens()) {
      String strLocStep = st.nextToken();

      for (Iterator i = content.iterator(); i.hasNext();) {
        Object obj = i.next();

        if (obj instanceof picoDOMElement) {
          picoDOMElement pde = (picoDOMElement)obj;

          if (compareLocStep(strLocStep, pde.tagName)) {
            if (st.hasMoreTokens()) {
              return pde.selectSingleNode(st, fCreate);
            } else {
              return pde;
            }
          }
        }
      }
      if (fCreate) {
        picoDOMElement pdeNew = pdOwner.createElement(strLocStep);

        content.add(pdeNew);
        return st.hasMoreTokens() ? pdeNew.selectSingleNode(st, fCreate)
            : pdeNew;
      }
    }

    return null;
  }
  
  /**
   * This method provides a shortcut to replace the contents of an element with a simple
   * text value.
   */
  public void setContent(String strText)
  {
    content.clear();
    content.add(strText);
  }
}