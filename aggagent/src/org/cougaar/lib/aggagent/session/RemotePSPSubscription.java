package org.cougaar.lib.aggagent.session;

import org.cougaar.lib.planserver.*;
import org.cougaar.util.*;
import org.cougaar.core.cluster.*;

/**
 *  A RemotePSPSubscription is a type of RemoteBlackboardSubscription that is
 *  suitable for use within a PSP context (where Blackboard access is granted
 *  through a different API).
 *  <br><br>
 *  Eventually, the whole of the Planserver API will be going away.  When that
 *  time comes, this class will become obsolete.
 */
public class RemotePSPSubscription extends RemoteBlackboardSubscription {
  // A UISubscriber instance that doesn't do anything
  private static class NullUISubscriber implements UISubscriber {
    public void subscriptionChanged (Subscription s) { }
  }
  private static UISubscriber nullSubscriber = new NullUISubscriber();

  private ServerPlugInSupport spis = null;

  /**
   *  Create a RemotePSPSubscription by using the provided ServerPlugInSupport
   *  reference to subscribe to predicate <it>p</it>.  The instance constructed
   *  here does not have a UISubscriber to listen for subscription events.
   */
  public RemotePSPSubscription (ServerPlugInSupport s, UnaryPredicate p) {
    this(s, p, nullSubscriber);
  }

  /**
   *  Create a RemotePSPSubscription by using the provided ServerPlugInSupport
   *  reference to subscribe to predicate <it>p</it>.  A UISubscriber instance
   *  for responding to subscription events is also specified here.
   */
  public RemotePSPSubscription (
      ServerPlugInSupport s, UnaryPredicate p, UISubscriber subscriber)
  {
    spis = s;
    subs = (IncrementalSubscription) spis.subscribe(subscriber, p);
  }

  protected void unsubscribe () {
    spis.unsubscribeForSubscriber(subs);
  }
}