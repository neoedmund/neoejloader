package neoe.neoejloader;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;


import java.text.*;

public class Loader {

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        System.out.println("param: lib-dir main-class params...");
        String libDir = args[0];
        String mainClass = args[1];
        String[] args2 = new String[args.length - 2];
        if (args2.length > 0)
            System.arraycopy(args, 2, args2, 0, args2.length);
        load(libDir, mainClass, args2);
    }

    public static void load(String libDir, String mainClass, String[] args)
            throws Exception {
        Iterable<File> it = new FileIterator(libDir);
        List<URL> jars = new ArrayList<URL>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        for (File f : it) {
            if (f.isDirectory()) {
                continue;
            }
            if (f.getName().toLowerCase().endsWith(".jar")) {
                System.out.println("add " + f.getAbsolutePath()+" \t "+sdf.format(new Date(f.lastModified())));
                jars.add(f.toURI().toURL());
            }
        }
        System.out.println("jar cnt=" + jars.size() + " run " + mainClass + " "
                + Arrays.deepToString(args));
        new URLClassLoader(jars.toArray(new URL[jars.size()]), Loader.class
                .getClassLoader()).loadClass(mainClass).getMethod("main",
                new Class[] { String[].class }).invoke(null,
                new Object[] { args });
    }

}
