package org.cougaar.lib.aggagent.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.core.servlet.*;
import org.cougaar.lib.aggagent.session.SessionManager;

/**
 *  The AggregationComponent provides external access (via HTTP) to the
 *  Aggregation functionality.  Currently, it supports both a HTML user
 *  interface with frames and a XML interface for thick clients.
 */
public class AggregationComponent extends BlackboardServletComponent
{
  private Object lock = new Object();
  private AggregationServletInterface htmlInterface = null;
  private AggregationServletInterface xmlInterface = null;

  /**
   * Constructor.
   */
  public AggregationComponent()
  {
    super();
    myServlet = new AggregationServlet();
  }

  /**
   * When this class is created the "load()" method will
   * be called, at which time we'll register our Servlet.
   */
  public void load() {
    super.load();

    // create interface objects
    SessionManager man = new SessionManager(agentId.toString(), blackboard,
                                            createSubscriptionSupport());
    htmlInterface =
        new AggregationHTMLInterface(blackboard, createSubscriptionSupport(),
                                     myPath);
    xmlInterface =
      new AggregationXMLInterface(blackboard, createSubscriptionSupport(),
                                  agentId.toString(), naming, man);
  }

  /**
   * Here is our inner class that will handle all HTTP and
   * HTTPS service requests for our <tt>myPath</tt>.
   */
  private class AggregationServlet extends HttpServlet
  {
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        handleRequest(request, response);
    }

    public void doPut(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        handleRequest(request, response);
    }

    private void handleRequest(HttpServletRequest request,
                               HttpServletResponse response) throws IOException
    {
      PrintWriter out = response.getWriter();

      synchronized (lock)
      {
        if (request.getParameter("THICK_CLIENT") != null)
        {
          xmlInterface.handleRequest(out, request);
        }
        else
        {
          htmlInterface.handleRequest(out, request);
        }
        out.flush();
      }
    }
  }
}