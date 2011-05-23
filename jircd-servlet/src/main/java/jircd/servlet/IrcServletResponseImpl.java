package jircd.servlet;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.io.IOException;
import javax.servlet.ServletOutputStream;

import jircd.irc.User;
import jircd.irc.Message;
import jircd.servlet.irc.IrcServletResponse;

/**
 * Implementation.
 */
public class IrcServletResponseImpl extends ServletResponseImpl implements IrcServletResponse {
	private final Servlet servlet;
	private final User user;
	private String command;
	private ByteArrayOutputStream out;
	private int bufferSize;

	public IrcServletResponseImpl(Servlet from, User to, String cmd) {
		servlet = from;
		user = to;
		command = cmd;
	}
	public void setCommand(String cmd) {
		command = cmd;
	}
	public String getCommand() {
		return command;
	}
	public void commit() throws IOException {
		if(out != null)
			send(new String(out.toByteArray(), getCharacterEncoding()));
	}
	public void send(String msg) {
		if(isCommitted)
			throw new IllegalStateException("Response already committed");
		boolean isMultiLine = (msg.indexOf('\n') != -1);
		if(isMultiLine) {
			StringBuffer line = new StringBuffer();
			int len = msg.length();
			for(int i=0; i<len; i++) {
				char ch = msg.charAt(i);
				if(ch == '\n') {
					Message message = new Message(servlet, command, user);
					message.appendLastParameter(line.toString());
					user.send(message);
					line.setLength(0);
				} else if(ch == '\r') {
					// skip
				} else {
					line.append(ch);
				}
			}
			msg = line.toString();
		}
		Message message = new Message(servlet, command, user);
		message.appendLastParameter(msg);
		user.send(message);
		isCommitted = true;
	}
	public void sendError(int code, String msg) {
		if(isCommitted)
			throw new IllegalStateException("Response already committed");
		String cmd = Integer.toString(code);
		int len = cmd.length();
		if(len == 1)
			cmd = "00"+cmd;
		else if(len == 2)
			cmd = "0"+cmd;
		else if(len > 3)
			throw new IllegalArgumentException("Invalid code: "+code);
		Message message = new Message(cmd, user);
		message.appendParameter(servlet.getNick());
		message.appendParameter(msg);
		user.send(message);
		isCommitted = true;
	}
	public ServletOutputStream getOutputStream() throws IOException {
		if(out != null)
			throw new IllegalStateException("Writer already opened");
		out = new ByteArrayOutputStream(bufferSize);
		return new CommitServletOutputStream(out);
	}
	public PrintWriter getWriter() throws IOException {
		if(out != null)
			throw new IllegalStateException("Stream already opened");
		out = new ByteArrayOutputStream(bufferSize);
		return new CommitPrintWriter(new OutputStreamWriter(out, getCharacterEncoding()));
	}
	public void setContentLength(int length) {
	}
	public void setBufferSize(int size) {
		if(out != null || isCommitted)
			throw new IllegalStateException("Content already written");
		bufferSize = size;
	}
	public int getBufferSize() {
		return out.size();
	}
	public void flushBuffer() throws IOException {
		out.flush();
		commit();
	}
	public void resetBuffer() {
		if(isCommitted)
			throw new IllegalStateException("Response already committed");
		out.reset();
	}
	public void reset() {
		if(isCommitted)
			throw new IllegalStateException("Response already committed");
		out.reset();
	}

	private class CommitServletOutputStream extends FilterServletOutputStream {
		public CommitServletOutputStream(OutputStream out) {
			super(out);
		}
		public void flush() throws IOException {
			out.flush();
			commit();
		}
	}
	private class CommitPrintWriter extends PrintWriter {
		public CommitPrintWriter(Writer out) {
			super(out, false);
		}
		public void flush() {
			try {
				out.flush();
				commit();
			} catch(IOException e) {
				setError();
			}
		}
	}
}
