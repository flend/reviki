package net.hillsdon.reviki.plugins;

import java.net.*;
import java.io.*;

import net.hillsdon.reviki.vc.PageInfo;
import net.hillsdon.reviki.wiki.renderer.macro.Macro;
import net.hillsdon.reviki.wiki.renderer.macro.ResultFormat;

public class IncludeHtmlMacro implements Macro {

  public String getName() {
    return "include-html";
  }

  public ResultFormat getResultFormat() {
    return ResultFormat.XHTML;
  }

  public String handle(final PageInfo page, final String remainder) throws Exception {
    URL oracle = new URL(remainder);
    BufferedReader in = new BufferedReader(
    new InputStreamReader(oracle.openStream()));

    StringBuilder output = new StringBuilder();
    String inputLine;
    while ((inputLine = in.readLine()) != null) {
      output.append(inputLine);
      output.append('\n');
    }
    in.close();

    return output.toString();
  }
}
