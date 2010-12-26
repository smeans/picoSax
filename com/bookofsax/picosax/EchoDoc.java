package com.bookofsax.picosax;

import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * Title:        Industrial WebEngine
 * Description:  High-volume content server application.
 * Copyright:    Copyright (c) W. Scott Means
 * Company:      Industrial Web Machines
 * @author W. Scott Means
 * @version 1.0
 */

public class EchoDoc extends DefaultHandler
{
  XMLReader m_xrParse = null;
  Locator m_locCur = null;
  PrintWriter m_pwOut = null;

  public EchoDoc()
  {
  }

  public void setOutput(PrintWriter pwOut)
  {
    m_pwOut = pwOut;
  }

  public PrintWriter getOutput()
  {
    return m_pwOut;
  }

  public XMLReader getXMLReader()
  {
    return m_xrParse;
  }

  public void doEcho(InputSource is) throws Exception
  {
    m_xrParse = (XMLReader)new picoSAX();

    m_xrParse.setFeature("http://xml.org/sax/features/namespaces", false);
    m_xrParse.setFeature("http://xml.org/sax/features/namespace-prefixes",
        false);

    m_xrParse.setContentHandler(this);

    m_xrParse.parse(is);
    m_xrParse = null;
  }

  public void setDocumentLocator (Locator locator)
  {
    m_locCur = locator;
  }

  public void startElement (String uri, String localName,
                            String qName, Attributes attributes)
      throws SAXException
  {
    m_pwOut.print("<" + qName);

    for (int i = 0; i < attributes.getLength(); i++) {
      m_pwOut.print(" " + attributes.getQName(i) + "=\"" + attributes.getValue(i) + "\"");
    }
    m_pwOut.print(">");
  }

  public void endElement (String uri, String localName, String qName)
      throws SAXException
  {
    m_pwOut.print("</" + qName + ">");
  }

  public void characters (char ch[], int start, int length)
      throws SAXException
  {
      m_pwOut.print(new String(ch, start, length));
  }

  public void ignorableWhitespace (char ch[], int start, int length)
      throws SAXException
  {
      m_pwOut.print(new String(ch, start, length));
  }

  public void processingInstruction (String target, String data)
      throws SAXException
  {
    m_pwOut.println("<?" + target + " " + data + "?>");
  }

  public void skippedEntity (String name)
      throws SAXException
  {
    m_pwOut.print("&" + name + ";");
  }

  public static void main(String [] args)
  {
    try {
      EchoDoc ed = new EchoDoc();
      PrintWriter pw = new PrintWriter(System.out);
      ed.setOutput(pw);

      if (args.length > 0) {
        for (int i = 0; i < args.length; i++) {
          InputSource is = new InputSource(args[i]);

          ed.doEcho(is);
        }
      } else {
        InputSource is = new InputSource(System.in);

        ed.doEcho(is);
      }

      pw.close();
    } catch (SAXParseException spe) {
      System.err.println("line: " + spe.getLineNumber() + " col: "
          + spe.getColumnNumber() + ": " + spe.getMessage());
    } catch (Exception e) {
      System.err.println(e);
    }
  }
}
