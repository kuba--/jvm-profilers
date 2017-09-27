import java.io.File;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.AgentInitializationException;

public class Attach {

    static void loadAgent(String pid, String libpath, String options) throws Exception {
        VirtualMachine vm = VirtualMachine.attach(pid);

        try {
            File lib = new File(libpath);

            if (!lib.exists()) {
                System.err.printf("'%s' does not exist.\n", libpath);
                System.exit(1);
            }

            vm.loadAgentPath(lib.getAbsolutePath(), options);
        } catch(AgentInitializationException e) {
            e.printStackTrace(System.err);
        } finally {
            vm.detach();
        }
    }

    public static void main(String[] args) throws Exception {
        String pid = args[0], lib = args[1], options = "";
        if (args.length > 2) options = args[2];

        loadAgent(pid, lib, options);
    }
}
