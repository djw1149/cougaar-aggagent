package org.cougaar.lib.aggagent.session;

import java.io.*;
import java.util.*;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.lib.planserver.*;
import org.cougaar.lib.aggagent.util.XmlUtils;
import org.cougaar.core.cluster.*;

public class Session implements UISubscriber {
  private Object lock = new Object();

  private String key = null;
  private String queryId = null;
  private String clusterId = null;
  private IncrementFormat sender = null;

  protected RemotePSPSubscription data = null;

  /**
   *  Create a new Session with the specified session ID.  This constructor
   *  should only be called by subclasses, where other required initializations
   *  will be implemented.
   */
  protected Session (String k, String queryId) {
    key = k;
    this.queryId = queryId;
  }

  /**
   *  Create a new Session with the specified session ID to search the
   *  blackboard for Objects matching the predicate given.  The
   *  ServerPlugInSupport argument is included so that the RemoteSubscription
   *  may be created.
   */
  public Session (String k, String queryId, IncrementFormat f) {
    this(k, queryId);
    sender = f;
  }

  /**
   *  Create the RemoteSubscription instance to be managed by this Session.
   *  Once this method is called, subscriptionChanged() events may start
   *  arriving.
   */
  public void start (ServerPlugInSupport spis, UnaryPredicate p) {
    synchronized (lock) {
      clusterId = spis.getClusterIDAsString();
      data = new RemotePSPSubscription(spis, p, this);
    }
  }

  /**
   *  End this session and let it halt all active and passive functions.
   */
  public void endSession () {
    data.shutDown();
  }

  /**
   *  For purposes of tabulation, this key identifies the session existing
   *  between this Object and the remote client.  Requests concerning the
   *  session (such as ending it, checking its status, etc.) should use this
   *  key.
   */
  public String getKey () {
    return key;
  }

  /**
   *  Check to see whether new information has been gathered since the last
   *  report.
   */
  public boolean hasChanged () {
    return data.hasChanged();
  }

  /**
   *  This method is called by the resident RemoteSubscription whenever new
   *  subscription information is available.  If immediate action is required,
   *  then it is implemented here.  By default, no action is taken.
   */
  public void subscriptionChanged (Subscription sub) {
  }

  /**
   *  Specify the IncrementFormat (q.v.) used to transmit data gathered by the
   *  RemoteSubscription for this Session.
   */
  public void setIncrementFormat (IncrementFormat f) {
    sender = f;
  }

  /**
   *  Send an update of recent changes to the resident RemoteSubscription
   *  through the provided OutputStream.  An IncrementFormat instance is used
   *  to encode the data being sent.
   */
  public void sendUpdate (OutputStream out) {
    synchronized (lock) {
      data.open();
      try {
        sender.encode(out, data, key, queryId, clusterId);
      } catch (Exception e) {
        XmlUtils.sendException(queryId, clusterId, e, new PrintStream(out));
      }
      data.close();
    }
  }
}