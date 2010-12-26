package com.bookofsax.picosax;

import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * Title:        picoDOM Implementation
 * Description:  Ultra-lightweight document object model.
 * Copyright:    Copyright (c) W. Scott Means
 * Company:      Enterprise Web Machines
 * @author W. Scott Means
 * @version 1.0
 */

public class picoDOM extends DefaultHandler
{
  Stack elementStack = new Stack();
  public picoDOMElement rootElement = null;

  public void startElement (String uri, String localName,
                            String qName, Attributes attributes)
      throws SAXException
  {
    picoDOMElement pde = createElement(qName);

    for (int i = 0; i < attributes.getLength(); i++) {
      pde.attrs.put(attributes.getQName(i), attributes.getValue(i));
    }

    if (rootElement == null) {
      rootElement = pde;
    } else {
      ((picoDOMElement)elementStack.peek()).content.add(pde);
    }

    elementStack.push(pde);
  }

  public void endElement (String uri, String localName, String qName)
      throws SAXException
  {
    elementStack.pop();
  }

  public void characters (char ch[], int start, int length)
      throws SAXException
  {
    picoDOMElement pde = (picoDOMElement)elementStack.peek();

    pde.content.add(new String(ch, start, length));
  }

  /**
   * This factory method must be used to create elements that will be inserted
   * into a particular picoDOM document. Since parent pointers are not kept, each
   * element must retain a pointer to its parent document to make it possible to
   * locate its lineage.
   */
  public picoDOMElement createElement(String strTagName)
  {
    return new picoDOMElement(this, strTagName);
  }
  
  /**
   * This shortcut factory method is used to quickly create a text-only leaf element.
   */
  public picoDOMElement createElement(String strTagName, String strContent)
  {
    picoDOMElement pde = createElement(strTagName);
    pde.content.add(strContent);
    
    return pde;
  }

  /**
   * Static method for quickly parsing a document into a <code>picoDOM</code>
   * tree.
   */
  public static picoDOM parseDOC(InputSource is)
  {
    XMLReader xr = (XMLReader)new picoSAX();
    picoDOM pd = new picoDOM();

    try {
      xr.setFeature(picoSAX.NAMESPACE_FEATURE_URL, false);
      xr.setContentHandler(pd);

      xr.parse(is);
    } catch (Exception e) {
      return null;
    }

    return pd;
  }

  /**
   * Move an element from one document to another.
   */
  public picoDOMElement importElement(picoDOMElement pde)
  {
    pde.pdOwner = this;

    return pde;
  }

  /**
   * Pass-through to the root element, ITIS.
   */
  public String toString()
  {
    if (rootElement != null) {
      return rootElement.toString();
    } else {
      return "";
    }
  }

  /**
   * Pass-through to the root element, ITIS.
   */
  public String toXML()
  {
    if (rootElement != null) {
      return rootElement.toXML();
    } else {
      return "#EMPTY#";
    }
  }

  /**
   * Simple test method that parses each document at URL passed in on command
   * line, stores as picoDOM document, then regurgitates.
   */
  public static void main(String [] args)
  {
    try {
      XMLReader xr = (XMLReader)new picoSAX();

      for (int i = 0; i < args.length; i++) {
        picoDOM pd = new picoDOM();

        xr.setContentHandler(pd);
        xr.setFeature(picoSAX.NAMESPACE_FEATURE_URL, false);
        xr.parse(args[i]);

        System.out.println("document '" + args[i] + "':");
        System.out.println(pd);
      }
    } catch (Exception e) {
      System.err.println(e);
    }
  }
}