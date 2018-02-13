package com.jfrog.sample;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

/*
 * Java based CI/CD Demo
 * - Build me...........master.
 */
public class Hello extends AbstractHandler {

	public static void main(String[] args) throws Exception
	{
		Hello me = new Hello();
		
		Server server = new Server(6800);
		server.setHandler(me);
		
		server.start();
		server.join();
	}

	public void handle(String target,
            Request baseRequest,
            HttpServletRequest req,
            HttpServletResponse resp) throws IOException, ServletException
	{
    		if (target.equals("/shutdown")) {
    			doShutdown(baseRequest, resp);
    		} else {
	    		resp.setContentType("text/html;charset=utf-8");
	    		resp.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
			resp.getWriter().println("<h1>Hello World!!!</h1>");
			resp.getWriter().println("<br/>");
			
			resp.getWriter().println("<h2>Headers</h2>");
			
			Enumeration<String> hdrNames = req.getHeaderNames();
			while (hdrNames.hasMoreElements()) {
				String hdrName = hdrNames.nextElement();
				resp.getWriter().println(hdrName + ": " + req.getHeader(hdrName));
	
				resp.getWriter().println("<br/>");
			}
			resp.getWriter().println("<br/>");
		}
	}
    
    protected void doShutdown(Request baseRequest, HttpServletResponse response) throws IOException 
    {
        for (Connector connector : getServer().getConnectors()) 
        {
            connector.shutdown();
        }

        response.sendError(200, "Connectors closed, commencing full shutdown");
        baseRequest.setHandled(true);

        final Server server=getServer();
        new Thread()
        {
            @Override
            public void run ()
            {
                try
                {
                    shutdownServer(server);
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Shutting down server",e);
                }
            }
        }.start();
    }
    
    private void shutdownServer(Server server) throws Exception
    {
        server.stop();
        System.exit(0);
    }

}
