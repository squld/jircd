package jircd.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.management.*;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;

import jircd.irc.Listener;
import jircd.irc_p10.Server_P10;
import jircd.servlet.irc.IrcServlet;

public class jIRCd extends jircd.jIRCd implements MBeanRegistration {
    private static final Logger logger = Logger.getLogger(jIRCd.class);
    
    private static final String WEBAPP_PATH = "webapps";
    
    private final Set servlets = new HashSet();
    private MBeanServer mBeanServer;
    private ObjectName mBeanName;
    
    public static void main(String[] args) {
        // program must be executed using: jircd.servlet.jIRCd <configuration file>
        if ((args == null) || (args.length < 1)) {
            System.err.println("Usage: jircd.servlet.jIRCd <configuration file>");
            System.exit(1);
        }
        final String configURL = args[0];
        
        printBanner(System.out);
        
        jIRCd jircd = null;
        // attempt to read the specified configuration file
        try {
            jircd = new jIRCd(configURL);
        } catch (ParserConfigurationException pce) {
            System.err.println("No XML parser available: "+pce);
            System.exit(1);
        } catch (IOException ioe) {
            System.err.println(ioe + " occured while reading configuration file.");
            System.exit(1);
        }

        jircd.start();
        
        // now just hang out forever
        System.out.println("Press enter to terminate.");
        try {
            System.in.read();
        } catch (IOException e) {
            System.err.println(e + " occured while waiting for program termination.");
            System.exit(1);
        }
        
        System.out.println("Shutting down...");
        jircd.stop();
    }
    
    public jIRCd(String configURL) throws ParserConfigurationException, IOException {
        super(configURL);
        reloadWebApps();
    }
    
    public void reloadWebApps() throws ParserConfigurationException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        undeployServlets();
        File webappDir = new File(WEBAPP_PATH);
        File[] warFiles = webappDir.listFiles(new ExtensionFilenameFilter("war"));
        for(int i=0; i<warFiles.length; i++) {
            File warFile = warFiles[i];
            logger.info("Deploying webapp "+warFile+"...");
            String warName = warFile.getName();
            File warDir = new File(webappDir, warName.substring(0, warName.length()-4));
            try {
                unpackWAR(warFile, warDir);
                // create class loader for webapp
                File classesDir = new File(warDir, "WEB-INF/classes");
                File libDir = new File(warDir, "WEB-INF/lib");
                File[] jarFiles;
                if(libDir.exists())
                    jarFiles = libDir.listFiles(new ExtensionFilenameFilter("jar"));
                else
                    jarFiles = new File[0];
                URL[] classpath = new URL[jarFiles.length+1];
                classpath[0] = classesDir.toURL();
                for(int j=0; j<jarFiles.length; j++)
                    classpath[j+1] = jarFiles[j].toURL();
                final URLClassLoader loader = URLClassLoader.newInstance(classpath);
                // scan library jars for plugins
                for(int j=0; j<jarFiles.length; j++)
                    loadPlugin(new JarFile(jarFiles[j]), loader);
                
                WebAppHandler webAppDescriptor = new WebAppHandler();
                SAXParser parser = parserFactory.newSAXParser();
                parser.parse(new File(warDir, "WEB-INF/web.xml"), webAppDescriptor);
                
                final ServletContextImpl context = new ServletContextImpl(webAppDescriptor.contextName, webAppDescriptor.contextParameters);
                context.setAttribute("jircd", this);
                context.setAttribute("jircd.irc.network", network);
                for(Iterator iter = webAppDescriptor.mappings.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    String path = (String) entry.getKey();
                    String name = (String) entry.getValue();
                    if(path.indexOf(",isnick") != -1) {
                        deployServlet(path, name, webAppDescriptor, context, loader);
                    } else {
                        logger.warn("Unsupported mapping "+path+" for servlet "+name);
                    }
                }
            } catch(SAXException saxe) {
                logger.warn("Could not deploy webapp "+warFile, saxe);
            } catch(IOException ioe) {
                logger.warn("Could not deploy webapp "+warFile, ioe);
            }
        }
    }
    private void unpackWAR(File file, File dir) throws IOException {
        JarFile warFile = new JarFile(file);
        for(Enumeration iter = warFile.entries(); iter.hasMoreElements(); ) {
            JarEntry entry = (JarEntry) iter.nextElement();
            File outFile = new File(dir, entry.getName());
            if(entry.isDirectory()) {
                outFile.mkdirs();
            } else {
                outFile.getParentFile().mkdirs();
                BufferedInputStream in = new BufferedInputStream(warFile.getInputStream(entry));
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
                byte[] buffer = new byte[1024];
                int bytesRead;
                do {
                    bytesRead = in.read(buffer);
                    if(bytesRead > 0)
                        out.write(buffer, 0, bytesRead);
                } while(bytesRead != -1);
                out.close();
                in.close();
            }
        }
    }
    private void deployServlet(String path, String name, WebAppHandler descriptor, ServletContextImpl context, ClassLoader loader) {
        int startPos = (path.charAt(0) == '/') ? 1 : 0;
        int endPos = path.indexOf(',');
        String nick = path.substring(startPos, endPos);
        String className = (String) descriptor.servlets.get(name);
        Properties parameters = (Properties) descriptor.initParameters.get(name);
        ServletConfigImpl config = new ServletConfigImpl(name, parameters, context);
        try {
            Class cls = loader.loadClass(className);
            IrcServlet servlet = (IrcServlet) cls.newInstance();
            servlet.init(config);
            servlets.add(servlet);
            int pos = path.indexOf(',');
            Servlet user = new Servlet(nick, name, servlet, (Server_P10) thisServer);
            logger.info("...path "+path+" assigned to "+name+" ("+className+")");
        } catch(Exception ex) {
            logger.warn("Could not load class "+className, ex);
        }
    }
    private void undeployServlets() {
        for(Iterator iter = servlets.iterator(); iter.hasNext(); iter.remove()) {
            IrcServlet servlet = (IrcServlet) iter.next();
            servlet.destroy();
        }
    }
    
    public ObjectName preRegister(MBeanServer mBeanServer, ObjectName objectName) {
        this.mBeanServer = mBeanServer;
        mBeanName = objectName;
        return objectName;
    }
    public void postRegister(Boolean aBoolean) {
        if(aBoolean.booleanValue()) {
            registerMBeans();
        } else {
            postDeregister();
        }
    }
    public void preDeregister() {
        unregisterMBeans();
    }
    public void postDeregister() {
        this.mBeanServer = null;
        mBeanName = null;
    }
    
    private void registerMBeans() {
        for(Iterator iter = listeners.iterator(); iter.hasNext(); ) {
            Listener listener = (Listener) iter.next();
            Hashtable keyProps = new Hashtable(mBeanName.getKeyPropertyList());
            keyProps.put("listener", listener.getClass().getName());
            keyProps.put("address", listener.getAddress());
            keyProps.put("port", Integer.toString(listener.getPort()));
            try {
                ObjectName ln = ObjectName.getInstance(mBeanName.getDomain(), keyProps);
                logger.debug("Registering "+ln);
                this.mBeanServer.registerMBean(listener, ln);
            } catch(JMException e) {
                logger.warn("Could not register listener MBean", e);
            }
        }
        
        Hashtable keyProps = new Hashtable(mBeanName.getKeyPropertyList());
        keyProps.put("links", links.getClass().getName());
        try {
            ObjectName ln = ObjectName.getInstance(mBeanName.getDomain(), keyProps);
            logger.debug("Registering "+ln);
            this.mBeanServer.registerMBean(links, ln);
        } catch(JMException e) {
            logger.warn("Could not register links MBean", e);
        }
    }
    private void unregisterMBeans() {
        try {
            ObjectName namePattern = ObjectName.getInstance(mBeanName.getCanonicalName()+",*");
            Set mBeans = this.mBeanServer.queryNames(namePattern, null);
            for(Iterator iter = mBeans.iterator(); iter.hasNext(); ) {
                ObjectName name = (ObjectName) iter.next();
                try {
                    mBeanServer.unregisterMBean(name);
                } catch(JMException e) {
                    logger.warn("Could not unregister MBean", e);
                }
            }
        } catch(MalformedObjectNameException mone) {
            logger.warn("Cannot unregister MBeans", mone);
        }
    }
}
