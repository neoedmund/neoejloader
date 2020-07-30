package neoe.neoejloader;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Loader {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("neoejloader 7j30b\n  param: lib-dir main-class params...");
		String libDir = args[0];
		if (args.length == 1) {
			System.out.println("main-class is not paramed, so scan lib-dir");
			load(libDir, null, null);
			return;
		}
		String mainClass = args[1];
		String[] args2 = new String[args.length - 2];
		if (args2.length > 0)
			System.arraycopy(args, 2, args2, 0, args2.length);
		load(libDir, mainClass, args2);
	}

	public static void load(String libDir, String mainClass, String[] args) throws Exception {
		Iterable<File> it = new FileIterator(libDir);
		List<URL> jars = new ArrayList<URL>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		List<File> fs = new ArrayList<File>();
		for (File f : it) {
			if (f.isDirectory()) {
				continue;
			}
			if (f.getName().toLowerCase().endsWith(".jar")) {
				fs.add(f);
			}
		}
		Collections.sort(fs);
		for (File f : fs) {
			System.out.println("add " + f.getAbsolutePath() + " \t " + sdf.format(new Date(f.lastModified())));
			jars.add(f.toURI().toURL());
		}
		URLClassLoader cl = new URLClassLoader(jars.toArray(new URL[jars.size()]), Loader.class.getClassLoader());
		if (mainClass == null) {
			List mains = new ArrayList();
			scan(cl, jars, mains);
			System.out.println(String.format("Found %d main", mains.size()));
		} else {
			if (mainClass.startsWith("?")) {
				List mains = new ArrayList();
				scan(cl, jars, mains);
				String key = mainClass.substring(1);
				Set found = new HashSet();
				for (Object o : mains) {
					String s = (String) o;
					int p = s.lastIndexOf('.');
					if (p >= 0) {
						s = s.substring(p + 1);
					}
					if (s.contains(key)) {
						found.add(o);
					}
				}
				if (found.isEmpty()) {
					System.out.println("No match for " + key);
				} else if (found.size() > 1) {
					StringBuilder sb = new StringBuilder("Ambiguities:[\n");
					for (Object o : found) {
						sb.append("\t").append(o).append("\n");
					}
					sb.append("]\n");
					System.out.println(sb);
				} else {
					String target = (String) found.iterator().next();
					System.out.println("Matched: " + target);
//					Thread.currentThread().setContextClassLoader(cl);
					cl.loadClass(target).getMethod("main", new Class[] { String[].class }).invoke(null,
							new Object[] { args });
				}
			} else {
				System.out.println("jar cnt=" + jars.size() + " run " + mainClass + " " + Arrays.deepToString(args));
//				Thread.currentThread().setContextClassLoader(cl);
				cl.loadClass(mainClass).getMethod("main", new Class[] { String[].class }).invoke(null,
						new Object[] { args });
			}
		}
	}

	private static void scan(URLClassLoader cl, List<URL> jars, List mains) throws Exception {
		for (URL url : jars) {
			scan(cl, url, mains);
		}
	}

	private static void scan(URLClassLoader cl, URL url, List mains) throws Exception {
		File f = new File(url.toURI().getPath());
		ZipFile zip = new ZipFile(f);
		List res = new ArrayList();
		try {
			for (Enumeration list = zip.entries(); list.hasMoreElements();) {
				ZipEntry entry = (ZipEntry) list.nextElement();
				String fn = entry.getName();
				if (fn.endsWith(".class")) {
					String cn = fn.substring(0, fn.length() - 6).replace('/', '.');
					if (scanClass(cl, cn)) {
						res.add(cn);
					}
				}

			}
		} finally {
			zip.close();
		}
		if (!res.isEmpty()) {
			System.out.println("[in " + f.getName() + "]");
			for (Object o : res) {
				System.out.print("\t");
				System.out.println(o);
			}
			if (mains != null)
				mains.addAll(res);
		}
	}

	private static boolean scanClass(URLClassLoader cl, String cn) {
		try {
			Class clz = cl.loadClass(cn);
			Method method = clz.getMethod("main", new Class[] { String[].class });
			int mods = method.getModifiers();
			if (method.getReturnType().equals(Void.TYPE) && Modifier.isPublic(mods) && Modifier.isStatic(mods)) {
				return true;
			}
		} catch (Throwable e) {
		}
		return false;

	}

}
