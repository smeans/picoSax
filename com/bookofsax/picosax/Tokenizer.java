/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 * 
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 * http://www.mozilla.org/MPL/ 
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License. 
 * 
 * The Original Code is picoSAX. 
 * 
 * The Initial Developer of the Original Code is W. Scott Means
 * <smeans@gmail.com>. 
 * 
 * Contributor(s): Michael A. Bodie.
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 * ***** END LICENSE BLOCK ***** */

package com.bookofsax.picosax;

import java.io.*;
import java.net.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class Tokenizer
{
  public final static int LINE_SEP = 0x0a;

  public static boolean isWS(int iChar)
  {
    switch (iChar) {
    case 0x20:
    case 0x09:
    case 0x0d:
    case 0x0a: {
      return true;
    }

    default: {
      return false;
    }
    }
  }

  public static boolean isXMLNameStart(int iChar)
  {
    if (Character.isLetter((char)iChar)) {
      return true;
    }

    switch (iChar) {
    case '_':
    case ':': {
      return true;
    }

    default: {
      return false;
    }
    }
  }

  public static boolean isXMLNamePart(int iChar)
  {
    if (Character.isLetterOrDigit((char)iChar)) {
      return true;
    }

    switch (iChar) {
    case '_':
    case ':':
    case '.':
    case '-': {
      return true;
    }

    default: {
      return false;
    }
    }
  }

  InputSource m_isIn;
  BufferedReader m_brIn;
  LocatorImpl m_liCur = new LocatorImpl();

  int m_chPushback = -1;
  int m_colPushback = 0;

  public Tokenizer(InputSource is) throws SAXException, IOException
  {
    m_isIn = is;

    if (m_isIn.getCharacterStream() != null) {
      m_brIn = new BufferedReader(m_isIn.getCharacterStream());
    } else if (m_isIn.getByteStream() != null) {
      m_brIn = new BufferedReader(new InputStreamReader(m_isIn.getByteStream()));
    } else {
      try {
        m_brIn = new BufferedReader(new java.io.FileReader(is.getSystemId()));
      } catch (FileNotFoundException fnfe) {
        try {
          URL url = new URL(is.getSystemId());

          m_brIn = new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (Exception e) {
          throw new SAXException("unable to open document '"
              + is.getSystemId() + "'", e);
        }
      }
    }
    m_liCur.setPublicId(is.getPublicId());
    m_liCur.setSystemId(is.getSystemId());
    m_liCur.setLineNumber(1);
    m_liCur.setColumnNumber(0);
  }

  public int readChar() throws IOException
  {
    int iChar = m_chPushback == -1 ? m_brIn.read() : m_chPushback;
    m_chPushback = -1;

    switch (iChar) {
    case -1: {
      // EOF
    } break;

    case LINE_SEP: {
      m_colPushback = m_liCur.getColumnNumber();
      m_liCur.setColumnNumber(0);
      m_liCur.setLineNumber(m_liCur.getLineNumber()+1);
    } break;

    default: {
      m_liCur.setColumnNumber(m_liCur.getColumnNumber()+1);
    } break;
    }

    return iChar;
  }

  public void pushback(int chPushback)
  {
    if (m_chPushback != -1) {
      System.err.println("Tokenizer.pushback(): internal error: pushback overflow");
    }

    m_chPushback = chPushback;

    if (m_chPushback == LINE_SEP) {
      m_liCur.setColumnNumber(m_colPushback);
      m_liCur.setLineNumber(m_liCur.getLineNumber()-1);
    } else {
      m_liCur.setColumnNumber(m_liCur.getColumnNumber()-1);
    }
  }

  public boolean eatWS() throws IOException
  {
    int iChar;
    boolean fFound = false;

    while (isWS((iChar = readChar()))) {
      fFound =  true;
    }

    pushback(iChar);

    return fFound;
  }

  public String getXMLName() throws IOException
  {
    int iChar = readChar();

    if (!isXMLNameStart(iChar)) {
      pushback(iChar);

      return null;
    }

    StringBuffer sb = new StringBuffer();
    sb.append((char)iChar);

    while (isXMLNamePart(iChar = readChar())) {
      sb.append((char)iChar);
    }

    pushback(iChar);

    return sb.toString();
  }

  public String readQuotedString() throws IOException
  {
    int iCharQuote = readChar();

    switch (iCharQuote) {
    case '"':
    case '\'': {
      break;
    }

    default: {
      pushback(iCharQuote);
      return null;
    }
    }

    StringBuffer sb = new StringBuffer();

    int iChar = readChar();
    while (iChar != iCharQuote && iChar > -1) {
      sb.append((char)iChar);
      iChar = readChar();
    }

    if (iChar == -1) {
      return null;
    } else {
      return sb.toString();
    }
  }

  public Locator getLocator()
  {
    return (Locator)m_liCur;
  }
}