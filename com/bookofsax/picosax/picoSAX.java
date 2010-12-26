package com.bookofsax.picosax;

import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * Title:        picoSAX
 * Description:  This is a minimal implementation of an XML and SAX parser.
 * Copyright:    Copyright (c) Bodie & Means
 * Company:      The Book of SAX -- No Starch Press
 * @author Bodie & Means
 * @version 1.0
 */

public class picoSAX extends DefaultHandler implements XMLReader
{
  //
  // global members
  //
  public final static String XMLNS_PREFIX = "xmlns";
  public final static String CDATA_NAME = "CDATA";
  public final static String NDATA_NAME = "NDATA";

  public final static int NS_INDEX_URI = 0;
  public final static int NS_INDEX_LOCAL = 1;
  public final static int NS_INDEX_RAW = 2;

  public final static int PUB_ID_INDEX = 0;
  public final static int SYS_ID_INDEX = 1;

  //
  // SAX 2.0 feature URLs
  public final static String NAMESPACE_FEATURE_URL =
      "http://xml.org/sax/features/namespaces";
  public final static String NAMESPACE_PREFIX_FEATURE_URL =
      "http://xml.org/sax/features/namespace-prefixes";
  public final static String NAMESPACE_XINCLUDE =
      "http://www.w3.org/2001/XInclude";

  public static void main(String [] args)
  {
    try {
      picoSAX ps = new picoSAX();

      if (args.length > 0) {
        for (int i = 0; i < args.length; i++) {
          ps.parse(args[i]);
        }
      } else {
        InputSource is = new InputSource(System.in);

        ps.parse(is);
      }
    } catch (SAXParseException spe) {
      System.err.println("line: " + spe.getLineNumber() + " col: "
          + spe.getColumnNumber() + ": " + spe.getMessage());
    } catch (Exception e) {
      System.err.println(e);
    }
  }

  //
  // instance members
  //


  ContentHandler m_chUser = this;
  DTDHandler m_dhUser = this;
  EntityResolver m_erUser = this;
  ErrorHandler m_ehUser = this;

  Tokenizer m_tCur = null;
  Stack m_stInclude = new Stack();
  NamespaceSupport m_nsCur = new NamespaceSupport();
  Stack m_sElem = new Stack();
  boolean m_fInDOCTYPE = false;
  boolean m_fSawDocElement = false;

  //
  // SAX 2.0 features
  //
  boolean m_fNamespaces = true;
  boolean m_fNamespacePrefixes = false;

  //
  // custom features
  //
  boolean m_fXInclude = true;

  public picoSAX()
  {
  }

  public ContentHandler getContentHandler()
  {
    return m_chUser == this ? null : m_chUser;
  }

  public DTDHandler getDTDHandler()
  {
    return m_dhUser == this ? null : m_dhUser;
  }

  public EntityResolver getEntityResolver()
  {
    return m_erUser == this ? null : m_erUser;
  }

  public ErrorHandler getErrorHandler()
  {
    return m_ehUser == this ? null : m_ehUser;
  }

  public boolean getFeature(String name)
    throws org.xml.sax.SAXNotRecognizedException,
    org.xml.sax.SAXNotSupportedException
  {
    if (name.equals(NAMESPACE_FEATURE_URL)) {
      return m_fNamespaces;
    } else if (name.equals(NAMESPACE_PREFIX_FEATURE_URL)) {
      return m_fNamespacePrefixes;
    } else {
      throw new SAXNotRecognizedException("feature '" + name
          + "' not recognized");
    }
  }

  public Object getProperty(String parm1) throws org.xml.sax.SAXNotRecognizedException, org.xml.sax.SAXNotSupportedException
  {
    throw new SAXNotSupportedException("no properties supported");
  }

  public void setContentHandler(ContentHandler handler)
  {
    m_chUser =  handler == null ? this : handler;
  }

  public void setDTDHandler(DTDHandler handler)
  {
    m_dhUser = handler == null ? this : handler;
  }

  public void setEntityResolver(EntityResolver resolver)
  {
    m_erUser = resolver == null ? this : resolver;
  }

  public void setErrorHandler(ErrorHandler handler)
  {
    m_ehUser = handler == null ? this : handler;
  }

  public void setFeature(String name, boolean value) throws org.xml.sax.SAXNotRecognizedException, org.xml.sax.SAXNotSupportedException
  {
    if (name.equals(NAMESPACE_FEATURE_URL)) {
      m_fNamespaces = value;
    } else if (name.equals(NAMESPACE_PREFIX_FEATURE_URL)) {
      m_fNamespacePrefixes = value;
    } else if (name.equals(NAMESPACE_XINCLUDE)) {
      m_fXInclude = value;
    } else {
      throw new SAXNotRecognizedException("feature '" + name
          + "' not recognized");
    }
  }

  public void setProperty(String parm1, Object parm2) throws org.xml.sax.SAXNotRecognizedException, org.xml.sax.SAXNotSupportedException
  {
    throw new SAXNotSupportedException("no properties are supported");
  }

  public void parse(String systemId) throws java.io.IOException,
      org.xml.sax.SAXException
  {
    parse(new InputSource(systemId));
  }

  public void parse(InputSource input) throws java.io.IOException,
      org.xml.sax.SAXException
  {
    try {
      m_tCur = (Tokenizer)new Tokenizer(input);

      m_chUser.setDocumentLocator(m_tCur.getLocator());

      m_chUser.startDocument();

      while (m_tCur != null) {
        // ok, this is a trifle redundant, but it is actually a pretty clean
        // way to deal with the fact that locators may vary due to XInclude
        // processing
        m_chUser.setDocumentLocator(m_tCur.getLocator());

        while (parseMarkup())
          ;

        int iChar = m_tCur.readChar();

        if (iChar != -1) {
            throw new SAXParseException("found '" + (char)iChar
                + "', expected valid markup", m_tCur.getLocator());
        }

        m_tCur = m_stInclude.empty() ? null : (Tokenizer)m_stInclude.pop();
      }

      if (!m_fSawDocElement) {
        throw new SAXParseException("no document element found",
            m_tCur.getLocator());
      }

      m_chUser.endDocument();

    } catch (SAXParseException se) {
      m_ehUser.fatalError(se);
    }
  }

  boolean parseMarkup() throws SAXException, IOException
  {
    int iChar;

    if (m_sElem.empty()) {
      if (!m_fInDOCTYPE) {
        m_tCur.eatWS();
      } else {
        boolean fContinue;

        do {
          fContinue = false;

          m_tCur.eatWS();

          iChar = m_tCur.readChar();
          m_tCur.pushback(iChar);
          if (iChar == '%') {
            parsePERef();
            fContinue = true;
          }
        } while (fContinue);
      }
    } else {
      StringBuffer sb = new StringBuffer();

      iChar = m_tCur.readChar();

      while (iChar != '<' && iChar > -1) {
        sb.append((char)iChar);
        iChar = m_tCur.readChar();
      }

      processChars(sb.toString());

      m_tCur.pushback(iChar);
    }


    iChar = m_tCur.readChar();

    if (iChar != '<') {
        return false;
    }

    iChar = m_tCur.readChar();

    switch (iChar) {
    case '?': {
      return parsePI();
    }

    case '/': {
      return parseElementEnd();
    }

    case '!': {
      return parseMetaMarkup();
    }

    default: {
      if (Tokenizer.isXMLNameStart(iChar)) {
        m_tCur.pushback(iChar);
        return parseElement();
      } else {
        m_tCur.pushback(iChar);

        return false;
      }
    }
    }
  }

  boolean parsePI() throws SAXException, IOException
  {
    String strTarget = m_tCur.getXMLName();

    if (strTarget == null) {
      throw new SAXParseException("expected identifier", m_tCur.getLocator());
    }

    int iChar = m_tCur.readChar();

    StringBuffer sbData = new StringBuffer();
    if (m_tCur.isWS(iChar)) {
      iChar = m_tCur.readChar();

      while (iChar != '?' && iChar > -1) {
        sbData.append((char)iChar);
        iChar = m_tCur.readChar();
      }
    }

    m_chUser.processingInstruction(strTarget, sbData.toString());

    if (!(iChar == '?' && m_tCur.readChar() == '>')) {
      throw new SAXParseException("expected '?>'", m_tCur.getLocator());
    }

    return true;
  }

  boolean parseElement() throws SAXException, IOException
  {
    if (m_fSawDocElement) {
      if (m_sElem.empty()) {
        throw new SAXParseException("only one top-level element allowed",
            m_tCur.getLocator());
      }
    } else {
      m_fSawDocElement = true;
    }

    if (m_fNamespaces) {
      m_nsCur.pushContext();
    }

    LocatorImpl liStart = new LocatorImpl(m_tCur.getLocator());

    String strTagName = m_tCur.getXMLName();

    AttributesImpl ai = new AttributesImpl();
    String [] astrName = new String[NS_INDEX_RAW+1];

    if (m_tCur.eatWS()) {
      String strQName;
      String strAttrVal;

      while ((strQName = m_tCur.getXMLName()) != null) {
        m_tCur.eatWS();
        if (m_tCur.readChar() != '=') {
          throw new SAXParseException("expected '='", m_tCur.getLocator());
        }
        m_tCur.eatWS();

        strAttrVal = m_tCur.readQuotedString();

        if (m_fNamespaces) {
          int iColon = strQName.indexOf(':');
          String strPrefix = "";

          if (iColon > -1) {
            strPrefix = strQName.substring(0, iColon);
          }

          if (strQName.equals(XMLNS_PREFIX)) {
            m_chUser.startPrefixMapping("", strAttrVal);
            m_nsCur.declarePrefix("", strAttrVal);

            if (m_fNamespacePrefixes) {
              ai.addAttribute("", "", strQName, CDATA_NAME, strAttrVal);
            }
          } else if (strPrefix.equals(XMLNS_PREFIX)) {
            m_chUser.startPrefixMapping(strQName.substring(iColon+1), strAttrVal);
            m_nsCur.declarePrefix(strQName.substring(iColon+1), strAttrVal);
            if (m_fNamespacePrefixes) {
              ai.addAttribute("", "", strQName, CDATA_NAME, strAttrVal);
            }
          } else {
            m_nsCur.processName(strQName, astrName, true);

            ai.addAttribute(astrName[NS_INDEX_URI], astrName[NS_INDEX_LOCAL],
                astrName[NS_INDEX_RAW], CDATA_NAME, strAttrVal);
          }
        } else {
          ai.addAttribute(null, strQName, strQName, CDATA_NAME, strAttrVal);
        }

        m_tCur.eatWS();
      }
    }

    m_tCur.eatWS();

    if (m_fNamespaces) {
      m_nsCur.processName(strTagName, astrName, false);
    } else {
      astrName[NS_INDEX_URI] = "";
      astrName[NS_INDEX_LOCAL] = strTagName;
    }

    int iChar = m_tCur.readChar();

    switch (iChar) {
    case '/': {
      if (m_tCur.readChar() != '>') {
        throw new SAXParseException("element start tag not well formed", liStart);
      }

      if (!isXIncludeElement(astrName)) {
        m_chUser.startElement(astrName[NS_INDEX_URI], astrName[NS_INDEX_LOCAL],
            strTagName, (Attributes)ai);
        m_chUser.endElement(astrName[NS_INDEX_URI], astrName[NS_INDEX_LOCAL],
            strTagName);
      } else {
        processInclude((Attributes)ai);
      }

      if (m_fNamespaces) {
        Enumeration e = m_nsCur.getDeclaredPrefixes();

        while (e.hasMoreElements()) {
          String strPrefix = (String)e.nextElement();
          m_chUser.endPrefixMapping(strPrefix);
        }

        m_nsCur.popContext();
      }
    } break;

    case '>': {
      if (m_fXInclude && !astrName[NS_INDEX_URI].equals(NAMESPACE_XINCLUDE)) {
        m_chUser.startElement(astrName[NS_INDEX_URI], astrName[NS_INDEX_LOCAL],
            strTagName, (Attributes)ai);
      }

      m_sElem.push(strTagName);
    } break;
    }

    return true;
  }

  boolean parseElementEnd() throws SAXException, IOException
  {
    String strTagName = m_tCur.getXMLName();

    if (strTagName == null) {
      throw new SAXParseException("malformed element close tag", m_tCur.getLocator());
    }

    String strLastStart = (String)m_sElem.pop();
    if (strLastStart == null || !strLastStart.equals(strTagName)) {
      throw new SAXParseException("mismatched element close tags <"
          + strLastStart + ">...</" + strTagName + ">", m_tCur.getLocator());
    }

    m_tCur.eatWS();

    if (m_tCur.readChar() != '>') {
      throw new SAXParseException("malformed element close tag",
          m_tCur.getLocator());
    }

    String [] astrName = new String[NS_INDEX_RAW+1];
    m_nsCur.processName(strTagName, astrName, false);
    if (!isXIncludeElement(astrName)) {
      m_chUser.endElement(astrName[NS_INDEX_URI], astrName[NS_INDEX_LOCAL],
          strTagName);
    }

    return true;
  }

  boolean isXIncludeElement(String [] astrName)
  {
    if (m_fXInclude) {
      return astrName[NS_INDEX_URI] != null &&
        astrName[NS_INDEX_URI].equals(NAMESPACE_XINCLUDE);
    } else {
      return false;
    }
  }

  void processInclude(Attributes atts) throws SAXException, IOException
  {
    String strHref = atts.getValue("href");

    if (m_tCur.getLocator().getSystemId() != null &&
        m_tCur.getLocator().getSystemId().equals(strHref)) {
      throw new SAXException("XInclude error: inclusion loop detected: href='"
          + strHref + "'");
    }

    Iterator i = m_stInclude.iterator();
    Tokenizer tk;

    while (i.hasNext()) {
      tk = (Tokenizer)i.next();

      if (tk.getLocator().getSystemId() != null &&
          tk.getLocator().getSystemId().equals(strHref)) {
        throw new SAXException("XInclude error: inclusion loop detected: href='"
            + strHref + "'");
      }
    }

    tk = new Tokenizer(new InputSource(strHref));

    m_stInclude.push(m_tCur);
    m_tCur = tk;
    m_chUser.setDocumentLocator(m_tCur.getLocator());
  }

  void processChars(String str) throws SAXException
  {
    StringTokenizer st = new StringTokenizer(str, "&", true);
    char [] ach;
    StringBuffer sb = new StringBuffer();

    while (st.hasMoreTokens()) {
      String strNext = st.nextToken("&");

      if (strNext.charAt(0) == '&') {
        if (!st.hasMoreTokens()) {
          throw new SAXParseException("expected ';'", m_tCur.getLocator());
        }

        String strEnt = st.nextToken(";");
        if (strEnt == null) {
          throw new SAXParseException("expected ';'", m_tCur.getLocator());
        }

        if (Tokenizer.isXMLNameStart(strEnt.charAt(0))) {
          if (sb.length() > 0) {
            ach = sb.toString().toCharArray();
            m_chUser.characters(ach, 0, ach.length);
            sb = new StringBuffer();
          }

          m_chUser.skippedEntity(strEnt);
        } else {
          if (strEnt.length() < 2 || strEnt.charAt(0) != '#') {
            throw new SAXParseException("malformed entity reference", m_tCur.getLocator());
          }

          int iCharVal;

          if (strEnt.charAt(1) == 'x') {
            iCharVal = Integer.parseInt(strEnt.substring(2), 16);
          } else {
            iCharVal = Integer.parseInt(strEnt.substring(1));
          }

          sb.append((char)iCharVal);
        }

        if (st.nextToken(";").charAt(0) != ';') {
          throw new SAXParseException("malformed entity reference", m_tCur.getLocator());
        }
      } else {
        sb.append(strNext);
      }
    }

    if (sb.length() > 0) {
      ach = sb.toString().toCharArray();
      m_chUser.characters(ach, 0, ach.length);
    }
  }

  boolean parseMetaMarkup() throws SAXException, IOException
  {
    int iChar = m_tCur.readChar();

    switch (iChar) {
    case '-': {
      if (m_tCur.readChar() != '-') {
        throw new SAXParseException("malformed comment", m_tCur.getLocator());
      }

      StringBuffer sb = new StringBuffer();

      iChar = m_tCur.readChar();
      while (iChar > -1) {
        sb.append((char)iChar);
        if (iChar == '>') {
          if (sb.substring(sb.length()-3).equals("-->")) {
            break;
          }
        }

        iChar = m_tCur.readChar();
      }

      if (sb.length() < 3) {
        throw new SAXParseException("malformed comment", m_tCur.getLocator());
      }

      String strComment = (sb.toString()).substring(0, sb.length()-3);

      // SAX doesn't provide for the reporting of comments

      return true;
    }

    case '[': {
      String strCDATA = m_tCur.getXMLName();

      if (strCDATA != null && strCDATA.equals("CDATA")) {
        if (m_tCur.readChar() != '[') {
          throw new SAXParseException("expected '['", m_tCur.getLocator());
        }

        StringBuffer sb = new StringBuffer();
        iChar = m_tCur.readChar();
        while (iChar > -1) {
          sb.append((char)iChar);
          if (iChar == '>') {
            if (sb.toString().endsWith("]]>")) {
              break;
            }
          }

          iChar = m_tCur.readChar();
        }

        if (iChar == -1) {
          throw new SAXParseException("CDATA section not terminated (expected ]]>)",
              m_tCur.getLocator());
        }

        String strChars = sb.substring(0, sb.length()-3);
        m_chUser.characters(strChars.toCharArray(), 0, strChars.length());
      }

      return true;
    }

    case 'D': {
      m_tCur.pushback(iChar);

      String strDecl = m_tCur.getXMLName();

      if (strDecl.equalsIgnoreCase("DOCTYPE")) {
        return parseDOCTYPE();
      }
    } break;

    case 'E': {
      m_tCur.pushback(iChar);

      String strDecl = m_tCur.getXMLName();

      if (strDecl.equalsIgnoreCase("ENTITY")) {
        return parseENTITY();
      } else if (strDecl.equalsIgnoreCase("ELEMENT")) {
        return parseELEMENT();
      }
    } break;

    case 'A': {
      m_tCur.pushback(iChar);

      String strDecl = m_tCur.getXMLName();

      if (strDecl.equalsIgnoreCase("ATTLIST")) {
        return parseATTLIST();
      }
    } break;

    case 'N': {
      m_tCur.pushback(iChar);

      String strDecl = m_tCur.getXMLName();

      if (strDecl.equalsIgnoreCase("NOTATION")) {
        return parseNOTATION();
      }
    } break;
    }

    throw new SAXParseException("unrecognized markup", m_tCur.getLocator());
  }

  boolean parseDOCTYPE() throws SAXException, IOException
  {
    m_fInDOCTYPE = true;

    m_tCur.eatWS();

    String strDTDName = m_tCur.getXMLName();

    if (strDTDName == null) {
      throw new SAXParseException("expected name", m_tCur.getLocator());
    }

    m_tCur.eatWS();
    int iChar = m_tCur.readChar();

    if (iChar == '>') {
      return true;
    } else if (m_tCur.isXMLNameStart(iChar)) {
      m_tCur.pushback(iChar);

      String [] astrIds = new String[SYS_ID_INDEX+1];

      parseExternalID(astrIds);

      m_tCur.eatWS();

      iChar = m_tCur.readChar();
    }

    if (iChar == '[') {
      while (parseMarkup())
        ;

      iChar = m_tCur.readChar();
    }

    m_fInDOCTYPE = false;

    return true;
  }

  boolean parseENTITY() throws SAXException, IOException
  {
    if (!m_fInDOCTYPE) {
      throw new SAXParseException("<!ENTITY> declaration illegal outside of <!DOCTYPE> declaration",
          m_tCur.getLocator());
    }

    m_tCur.eatWS();

    int iChar = m_tCur.readChar();

    boolean fParmEntity = false;

    if (iChar == '%') {
      fParmEntity = true;
      m_tCur.eatWS();
    } else {
      m_tCur.pushback(iChar);
    }

    String strEntityName = m_tCur.getXMLName();
    if (strEntityName == null) {
      throw new SAXParseException("expected entity name", m_tCur.getLocator());
    }

    m_tCur.eatWS();
    String strVal = m_tCur.readQuotedString();
    String [] astrIds = new String[SYS_ID_INDEX+1];
    if (strVal == null) {
      parseExternalID(astrIds);
    }

    m_tCur.eatWS();

    iChar = m_tCur.readChar();
    if (iChar != '>') {
      m_tCur.pushback(iChar);
      String strNDATA = m_tCur.getXMLName();
      String strNotation = null;

      if (strNDATA != null && strNDATA.equals(NDATA_NAME)) {
        m_tCur.eatWS();

        strNotation = m_tCur.getXMLName();
      }

      if (strNotation == null) {
        throw new SAXParseException("expected 'NDATA notation-name'",
            m_tCur.getLocator());
      } else {
        m_dhUser.unparsedEntityDecl(strEntityName, astrIds[PUB_ID_INDEX],
            astrIds[SYS_ID_INDEX], strNotation);
      }

      iChar = m_tCur.readChar();
    }

    if (iChar != '>') {
      throw new SAXParseException("expected '>' for entity declaration",
          m_tCur.getLocator());
    }

    return true;
  }

  boolean parseELEMENT() throws SAXException, IOException
  {
    if (!m_fInDOCTYPE) {
      throw new SAXParseException("<!ELEMENT> declaration illegal outside of <!DOCTYPE> declaration",
          m_tCur.getLocator());
    }

    m_tCur.eatWS();
    String strName = m_tCur.getXMLName();

    if (strName == null) {
      throw new SAXParseException("expected element name", m_tCur.getLocator());
    }

    m_tCur.eatWS();
    StringBuffer sb = new StringBuffer();
    int iChar = m_tCur.readChar();
    while (iChar > -1 && iChar != '>') {
      sb.append((char)iChar);
      iChar = m_tCur.readChar();
    }

    if (iChar == -1) {
      throw new SAXParseException("unexpected end of file",
          m_tCur.getLocator());
    }

    // !!!NOTE!!! string buffer contains unparsed element declaration information
    return true;
  }

  boolean parseATTLIST() throws SAXException, IOException
  {
    if (!m_fInDOCTYPE) {
      throw new SAXParseException("<!ATTLIST> declaration illegal outside of <!DOCTYPE> declaration",
          m_tCur.getLocator());
    }

    m_tCur.eatWS();
    String strElemName = m_tCur.getXMLName();
    if (strElemName == null) {
      throw new SAXParseException("expected parent element name",
          m_tCur.getLocator());
    }

    m_tCur.eatWS();
    int iChar;
    iChar = m_tCur.readChar();
    StringBuffer sb = new StringBuffer();
    while (iChar > -1 && iChar != '>') {
      sb.append((char)iChar);
      iChar = m_tCur.readChar();
    }

    if (iChar == -1) {
      throw new SAXParseException("unexpected end of file",
          m_tCur.getLocator());
    }

    return true;
  }

  boolean parseNOTATION() throws SAXException, IOException
  {
    if (!m_fInDOCTYPE) {
      throw new SAXParseException("<!NOTATION> declaration illegal outside of <!DOCTYPE> declaration",
          m_tCur.getLocator());
    }

    m_tCur.eatWS();

    String strName = m_tCur.getXMLName();
    if (strName == null) {
      throw new SAXParseException("expected notation name",
          m_tCur.getLocator());
    }

    m_tCur.eatWS();

    String [] astrIds = new String[SYS_ID_INDEX+1];

    parseExternalID(true, astrIds);

    m_tCur.eatWS();
    if (m_tCur.readChar() != '>') {
      throw new SAXParseException("expected '>' for notation declaration", m_tCur.getLocator());
    }

    m_dhUser.notationDecl(strName, astrIds[PUB_ID_INDEX],
        astrIds[SYS_ID_INDEX]);

    return true;
  }

  void parseExternalID(String [] astrIds)
      throws SAXException, IOException
  {
    parseExternalID(false, astrIds);
  }

  void parseExternalID(boolean fAllowPublicOnly, String [] astrIds)
      throws SAXException, IOException
  {
    String strKeyword = m_tCur.getXMLName();

    if (strKeyword == null) {
      throw new SAXParseException("expected 'PUBLIC' or 'SYSTEM'",
          m_tCur.getLocator());
    }

    String str;

    if (strKeyword.equals("SYSTEM")) {
      astrIds[PUB_ID_INDEX] = null;
      m_tCur.eatWS();
      astrIds[SYS_ID_INDEX] = m_tCur.readQuotedString();
      if (astrIds[SYS_ID_INDEX] == null) {
        throw new SAXParseException("expected system ID literal string",
            m_tCur.getLocator());
      }
    } else if (strKeyword.equals("PUBLIC")) {
      m_tCur.eatWS();
      astrIds[PUB_ID_INDEX] = m_tCur.readQuotedString();
      if (astrIds[PUB_ID_INDEX] == null) {
        throw new SAXParseException("expected public ID literal string",
            m_tCur.getLocator());
      }
      m_tCur.eatWS();
      astrIds[SYS_ID_INDEX] = m_tCur.readQuotedString();
      if (!fAllowPublicOnly) {
        if (astrIds[SYS_ID_INDEX] == null) {
          throw new SAXParseException("expected system ID literal string",
              m_tCur.getLocator());
        }
      }
    }
  }

  void parsePERef() throws SAXException, IOException
  {
    int iChar = m_tCur.readChar();

    if (iChar != '%') {
      throw new SAXParseException("expected %", m_tCur.getLocator());
    }

    String strPEName = m_tCur.getXMLName();
    if (strPEName == null) {
      throw new SAXParseException("expected parsed entity name", m_tCur.getLocator());
    }

    if (m_tCur.readChar() != ';') {
      throw new SAXParseException("expected ';'", m_tCur.getLocator());
    }
  }
}
