/**
 * Copyright (C) 2014  Eric Van Dewoestine
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.eclim.plugin.jdt.command.debug.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.core.DebugException;

import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

import org.eclipse.jdt.debug.core.IJavaStackFrame;

/**
 * UI model for displaying threads.
 */
public class ThreadView
{
  // Thread status
  private static final String RUNNING = "Running";
  private static final String SUSPENDED = "Suspended";

  private List<String> results = new ArrayList<String>();

  public synchronized List<String> get()
  {
    return results;
  }

  public synchronized void update(Map<Long, IThread> threadMap,
      Map<Long, IStackFrame[]> stackFrameMap)
    throws DebugException
  {
    results.clear();
    process(threadMap, stackFrameMap);
  }

  private void process(Map<Long, IThread> threadMap,
      Map<Long, IStackFrame[]> stackFrameMap)
    throws DebugException
  {
    for (Map.Entry<Long, IThread> entry : threadMap.entrySet()) {
      long threadId = entry.getKey();
      IThread thread = threadMap.get(threadId);
      String threadName = thread.getName();

      String status = thread.isSuspended() ? SUSPENDED : RUNNING;
      // Add 2 spaces for indentation
      results.add("  Thread-" + threadName +
          ":" + threadId  +
          " (" + status  + ")");

      IStackFrame[] stackFrames = stackFrameMap.get(threadId);
      if (stackFrames != null) {
        // Protect against invalid stack frame. When debug session is resumed,
        // all threads are resumed first. Then notification is sent for each
        // thread. While processing for one thread, we might end up using old
        // stack frames for some other thread, that are no longer valid.
        // This should not happen normally since we have a handler for debug
        // target itself, but this is being defensive.
        try {
          for (IStackFrame stackFrame : stackFrames) {
            // TODO Do formatting in VIM
            // Add 4 spaces for indentation under thread
            results.add("    " + getStackFrameText(stackFrame));
          }
        } catch (DebugException e) {}
      }
    }
  }

  private String getStackFrameText(IStackFrame stackFrame)
    throws DebugException
  {
    StringBuffer result = new StringBuffer();

    IJavaStackFrame frame = (IJavaStackFrame) stackFrame.getAdapter(
        IJavaStackFrame.class);
    if (frame != null) {
      String dec = frame.getDeclaringTypeName();

      if (frame.isObsolete()) {
        result.append(dec);
        result.append('>');
        return result.toString();
      }

      boolean javaStratum = true;
      javaStratum = frame.getReferenceType().getDefaultStratum().equals("Java");

      if (javaStratum) {
        // receiver name
        String rec = frame.getReceivingTypeName();
        result.append(getQualifiedName(rec));

        // append declaring type name if different
        if (!dec.equals(rec)) {
          result.append('(');
          result.append(getQualifiedName(dec));
          result.append(')');
        }
        // append a dot separator and method name
        result.append('.');
        result.append(frame.getMethodName());
        List<String> args = frame.getArgumentTypeNames();
        if (args.isEmpty()) {
          result.append("()"); //$NON-NLS-1$
        } else {
          result.append('(');
          Iterator<String> iter = args.iterator();
          while (iter.hasNext()) {
            result.append(getQualifiedName(iter.next()));
            if (iter.hasNext()) {
              result.append(", ");
            } else if (frame.isVarArgs()) {
              result.replace(result.length() - 2, result.length(), "...");
            }
          }
          result.append(')');
        }
      } else {
        result.append(frame.getSourcePath());
      }

      int lineNumber = frame.getLineNumber();
      result.append(' ');
      result.append(' ');
      if (lineNumber >= 0) {
        result.append(lineNumber);
      } else {
        if (frame.isNative()) {
          result.append(' ');
        }
      }

      if (!frame.wereLocalsAvailable()) {
        result.append(' ');
      }

      return result.toString();

    }
    return null;
  }

  private String getQualifiedName(String rec)
  {
    return rec;
  }
}