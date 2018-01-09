package com.jfrog.sample;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class Hello extends AbstractHandler {

    public void handle(String target,
            Request baseRequest,
            HttpServletRequest req,
            HttpServletResponse resp) throws IOException, ServletException
	{
    		resp.setContentType("text/html;charset=utf-8");
    		resp.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		resp.getWriter().println("<h1>Hello World</h1>");
		resp.getWriter().println("<br/>");
		
		
		resp.getWriter().println("<h2>Headers</h2>");
		
		@SuppressWarnings("unchecked")
		Enumeration<String> hdrNames = req.getHeaderNames();
		while (hdrNames.hasMoreElements()) {
			String hdrName = hdrNames.nextElement();
			resp.getWriter().println(hdrName + ": " + req.getHeader(hdrName));

			resp.getWriter().println("<br/>");
		}
	}

	public static void main(String[] args) throws Exception
	{
		Server server = new Server(6800);
		server.setHandler(new Hello());
		
		server.start();
		server.join();
	}
}
