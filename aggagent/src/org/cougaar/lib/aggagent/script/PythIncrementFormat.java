/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.lib.aggagent.script;

import org.cougaar.lib.aggagent.session.IncrementFormat;
import org.cougaar.lib.aggagent.session.SubscriptionAccess;
import org.cougaar.lib.aggagent.session.UpdateDelta;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

/**
 *  An implementation of IncrementFormat that derives its functionality from
 *  a script written in the JPython language.  A PythIncrementFormat is
 *  configured in two stages:  one is to declare a function or class, and the
 *  other is to pass the function or an instance of the class to the
 *  controlling Java context.
 *  <br><br>
 *  The necessity of using a two-stage initialization procedure for both types
 *  of PythIncrementFormat implementations is due to JPython's resolute refusal
 *  to allow an expression to declare a new function or class (or, indeed, any
 *  multiline construct).  One possibility is to use a "magic" function through
 *  which the Java and JPython contexts can communicate.  For the sake of
 *  uniformity, this option is used here.  The magic function is "instantiate",
 *  which the script should define in the global context as a no-arg function
 *  that returns either an IncrementFormat instance or a function designed to
 *  act as the "encode" method of an IncrementFormat.
 *  <br><br>
 *  This class implements the IncrementFormat interface, and can be
 *  instantiated by calling the constructor or a static factory method,
 *  formatFromScript().
 */
public class PythIncrementFormat implements IncrementFormat {
  // This is the JPython instruction evaluated to retrieve the product of the
  // script.  The script is responsible for providing the correct behavior to
  // the named function.
  private static String MAGIC_FUNCTION = "instantiate()";

  // IncrementFormat implementation that uses a JPython script as its encode
  // method.
  private static class Func implements IncrementFormat {
    private PyFunction delegateFunction = null;

    public Func (PyFunction f) {
      delegateFunction = f;
    }

    public void encode (UpdateDelta out, SubscriptionAccess sacc) {
      delegateFunction._jcall(new Object[] {out, sacc});
    }
  }

  // Each instance carrys a delegate IncrementFormat derived from a script
  private IncrementFormat delegate = null;

  /**
   *  Create a PythIncrementFormat instance by using a script-generated
   *  IncrementFormat as a delegate.
   *  @param script the JPython script that defines the embodied functionality
   */
  public PythIncrementFormat (String script) {
    delegate = formatFromScript(script);
  }

  /**
   *  An implementation of the encode method of interface IncrementFormat.  The
   *  function is actually delegated to a script-generated implementation,
   *  which is fabricated in the constructor.
   *
   *  @param out an UpdateDelta to be returned to the client
   *  @param sacc the subscription data to be encoded
   */
  public void encode (UpdateDelta out, SubscriptionAccess sacc) {
    delegate.encode(out, sacc);
  }

  /**
   *  Create an IncrementFormat from a JPython script.  There are two
   *  acceptable modes for the script.  It must produce either a JPython
   *  subclass of Java interface IncrementFormat or a JPython function that
   *  behaves like the method IncrementFormat.encode (i.e., takes two
   *  arguments and treats them as an UpdateDelta and a SubscriptionAccess).
   *  Either way, the script is required to define the magic function
   *  "instantiate()" to provide the function or formatter instance to the Java
   *  context.
   *
   *  @param script the executable script that declares classes and variables
   *  @return an IncrementFormat instance derived from the JPython scripts
   */
  public static IncrementFormat formatFromScript (String script) {
    PythonInterpreter pi = new NoErrorPython();
    if (script != null)
      pi.exec(script);
    PyObject product = pi.eval(MAGIC_FUNCTION);
    if (product instanceof PyFunction) {
      return new Func((PyFunction) product);
    }
    else {
      Object obj = product.__tojava__(IncrementFormat.class);
      if (obj instanceof IncrementFormat)
        return (IncrementFormat) obj;
    }
    throw new IllegalArgumentException(
      "JPython script did not yield a function or an IncrementFormat");
  }
}
