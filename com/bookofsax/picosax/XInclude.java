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