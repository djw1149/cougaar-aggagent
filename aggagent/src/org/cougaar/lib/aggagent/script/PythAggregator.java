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

import java.util.Iterator;
import java.util.List;

import org.cougaar.lib.aggagent.query.Aggregator;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

/**
 *  An implementation of Aggregator that derives its functionality from a
 *  script written in the JPython language.  A PythAggregator is configured in
 *  two stages:  one is to declare a function or class, and the other is to
 *  pass the function or an instance of the class to the controlling Java
 *  context.
 *  <br><br>
 *  The necessity of using a two-stage initialization procedure for both types
 *  of PythAggregator implementations is due to JPython's resolute refusal to
 *  allow an expression to declare a new function or class (or, indeed, any
 *  multiline construct).  One possibility is to use a "magic" function through
 *  which the Java and JPython contexts can communicate.  For the sake of
 *  uniformity, this option is used here.  The magic function is "instantiate",
 *  which the script should define in the global context as a no-arg function
 *  that returns either an Aggregator instance or a function designed to act as
 *  the "aggregate" method of an Aggregator.
 *  <br><br>
 *  This class implements the Aggregator interface, and can be instantiated by
 *  calling the constructor or a static factory method, aggregatorFromScript().
 */
public class PythAggregator implements Aggregator {
  // This is the JPython instruction evaluated to retrieve the product of the
  // script.  The script is responsible for providing the correct behavior to
  // the named function.
  private static String MAGIC_FUNCTION = "instantiate()";

  // Aggregator implementation that uses a JPython script as its aggregate
  // method.
  private static class Func implements Aggregator {
    private PyFunction delegateFunction = null;

    public Func (PyFunction f) {
      delegateFunction = f;
    }

    public void aggregate (Iterator dataAtoms, List output) {
      delegateFunction._jcall(new Object[] {dataAtoms, output});
    }
  }

  // Each instance carrys a delegate Aggregator derived from a script
  private Aggregator delegate = null;

  /**
   *  Create a PythAggregator instance by using a script-generated Aggregator
   *  as a delegate.
   *  @param script the JPython script that defines the embodied functionality
   */
  public PythAggregator (String script) {
    delegate = aggregatorFromScript(script);
  }

  /**
   *  An implementation of the aggregate method of interface Aggregator. The
   *  function is actually delegated to a script-generated implementation,
   *  which is fabricated in the constructor.
   *
   *  @param dataAtoms an iterator that iterates through raw, unaggregated
   *    data atoms
   *  @param output a List into which the produced ResultSetDataAtoms should be
   *    placed
   */
  public void aggregate (Iterator dataAtoms, List output) {
    delegate.aggregate(dataAtoms, output);
  }

  /**
   *  Create an Aggregator from a JPython script.  There are two acceptable
   *  modes for the script.  It must produce either a JPython subclass of Java
   *  interface Aggregator or a JPython function that behaves like the method
   *  Aggregator.aggregate().  Either way, the script is required to define the
   *  magic function "instantiate()" to provide the function or Aggregator
   *  instance to the Java context.
   *
   *  @param script the executable script that declares classes and variables
   *  @return an Aggregator instance derived from the JPython scripts
   */
  public static Aggregator aggregatorFromScript (String script) {
    PythonInterpreter pi = new NoErrorPython();
    if (script != null)
      pi.exec(script);
    PyObject product = pi.eval(MAGIC_FUNCTION);
    if (product instanceof PyFunction) {
      return new Func((PyFunction) product);
    }
    else {
      Object obj = product.__tojava__(Aggregator.class);
      if (obj instanceof Aggregator)
        return (Aggregator) obj;
    }
    throw new IllegalArgumentException(
      "JPython script did not yield a function or an Aggregator");
  }
}
