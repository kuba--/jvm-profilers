package jvmprofilers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

class  CallInfo {
    String fromClass, fromMethod;
    String toClass, toMethod;
    Long count;
}

public class CallStack {

    static ThreadLocal<Stack<String>> stack = new ThreadLocal<Stack<String>>();
    static Map<String, CallInfo> graph = new ConcurrentHashMap<String, CallInfo>();

    static BufferedWriter dot;

    static {
        stack.set(new Stack<String>());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.err.println("[SHUTTING DOWN]\n");

                try {
                    for (Map.Entry<String, CallInfo> entry : graph.entrySet()) {
                        //String call = entry.getKey();
                        CallInfo info = entry.getValue();

                        //dot.write(info.fromClass + "->" + info.toClass + " [label=\"" + info.fromMethod + " -(" + info.count + ")-> " + info.toMethod + "\"];\n");
                        dot.write(info.fromClass + "->" + info.toClass + " [label=\"" + info.count + "\"];\n");
                    }

                    dot.write("}\n");
                    dot.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void push(String callname) throws IOException {
        if (stack.get() == null)
            stack.set(new Stack<String>());

        if (!stack.get().empty()) {
            String from = stack.get().peek(), to = callname;
            String call = from + "->" + to;

            CallInfo info = graph.get(call);
            if (info == null) {
                info = new CallInfo();

                int idx = from.lastIndexOf('.');
                info.fromClass = from.substring(0, idx);
                info.fromMethod = from.substring(idx + 1, from.length());

                idx = to.lastIndexOf('.');
                info.toClass = to.substring(0, idx);
                info.toMethod = to.substring(idx + 1, to.length());

                info.count = 1L;
            }
            else info.count += 1L;

            graph.put(call, info);
        }

        stack.get().push(callname);
    }

    public static void pop() throws IOException {
        if (null == stack || null == stack.get() || stack.get().empty()) return;

        stack.get().pop();
    }
}
