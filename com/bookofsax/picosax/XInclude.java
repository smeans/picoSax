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
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class XInclude
{
  public static void main(String [] args)
  {
    try {
      EchoDoc ed = new EchoDoc();
      PrintWriter pw = new PrintWriter(System.out);
      ed.setOutput(pw);

      picoSAX ps = new picoSAX();
      ps.setContentHandler(ed);

      if (args.length > 0) {
        for (int i = 0; i < args.length; i++) {
          InputSource is = new InputSource(args[i]);

          ps.parse(is);
        }
      } else {
        InputSource is = new InputSource(System.in);

        ps.parse(is);
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